package net.goldskinmc.creategeoresonance.seismic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.goldskinmc.creategeoresonance.ItemStackComponentData;
import net.minecraft.core.component.DataComponents;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SeismogramMapService {
    private static final String TAG_ROOT = "GeoSeismogram";
    private static final String TAG_CENTER_X = "CenterX";
    private static final String TAG_CENTER_Y = "CenterY";
    private static final String TAG_CENTER_Z = "CenterZ";
    private static final String TAG_ENTRIES = "Entries";
    private static final String TAG_ENTRY_TYPE = "Type";
    private static final String TAG_ENTRY_WORLD_X = "WorldX";
    private static final String TAG_ENTRY_WORLD_Z = "WorldZ";
    private static final String TAG_ENTRY_WORLD_Y = "WorldY";
    private static final String TAG_STATION_X = "StationX";
    private static final String TAG_STATION_Y = "StationY";
    private static final String TAG_STATION_Z = "StationZ";
    private static final String TAG_STATION_DIMENSION = "StationDimension";
    private static final String TAG_EXACT_CLUSTERS = "ExactClusters";
    private static final String TAG_CLUSTER_TYPE = "ClusterType";

    private static final int WINDOW_HALF_BLOCKS = 24;
    private static final int WINDOW_DIAMETER_BLOCKS = WINDOW_HALF_BLOCKS * 2;
    private static final int MAP_SIZE = 128;
    private static final int SIGNAL_DOT_RADIUS = 2;
    private static final Map<MarkerKey, MarkerState> LAST_MARKERS = new HashMap<>();
    // Weak keys use identity, avoiding deep NBT hashing and retaining no discarded item data.
    private static final Cache<CustomData, CachedSeismogram> SNAPSHOTS = CacheBuilder.newBuilder()
        .weakKeys()
        .maximumSize(128)
        .build();

    private SeismogramMapService() {
    }

    public static ItemStack createMap(ServerLevel level, BlockPos stationPos, List<SeismicStationBlockEntity.MapEntry> entries,
                                      List<ExactCluster> exactClusters) {
        int centerX = centerOfChunk(stationPos.getX());
        int centerZ = centerOfChunk(stationPos.getZ());
        ItemStack mapStack = MapItem.create(level, centerX, centerZ, (byte) 0, false, false);
        MapItemSavedData mapData = MapItem.getSavedData(mapStack, level);
        if (mapData == null) {
            return mapStack;
        }

        CompoundTag geoTag = buildGeoTag(centerX, centerZ, stationPos, level.dimension().location().toString(), entries, exactClusters);
        ItemStackComponentData.putCompound(mapStack, TAG_ROOT, geoTag);

        renderStaticMap(mapData, geoTag);

        MapItem.lockMap(level, mapStack);
        mapStack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.creategeoresonance.seismogram")
            .withStyle(style -> style.withItalic(false)));
        return mapStack;
    }

    public static ItemStack restoreMap(ServerLevel level, MapSnapshot snapshot) {
        List<SeismicStationBlockEntity.MapEntry> entries = snapshot.signals().stream()
            .map(signal -> new SeismicStationBlockEntity.MapEntry(signal.type(),
                signal.worldX() - snapshot.stationPos().getX(),
                signal.worldZ() - snapshot.stationPos().getZ(), signal.approxY()))
            .toList();
        CompoundTag geoTag = buildGeoTag(snapshot.centerX(), snapshot.centerZ(), snapshot.stationPos(),
            snapshot.stationDimension(), entries, snapshot.exactClusters());
        geoTag.putInt(TAG_CENTER_Y, snapshot.centerY());
        var dimension = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse(snapshot.stationDimension()));
        MapItemSavedData mapData = MapItemSavedData.createFresh(snapshot.centerX(), snapshot.centerZ(),
            (byte) 0, false, false, dimension);
        var mapId = level.getFreeMapId();
        level.setMapData(mapId, mapData);
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, mapId);
        ItemStackComponentData.putCompound(stack, TAG_ROOT, geoTag);
        renderStaticMap(mapData, geoTag);
        MapItem.lockMap(level, stack);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.creategeoresonance.seismogram")
            .withStyle(style -> style.withItalic(false)));
        return stack;
    }

    public static void clear() {
        LAST_MARKERS.clear();
        SNAPSHOTS.invalidateAll();
    }

    @Nullable
    public static MapSnapshot readSnapshot(ItemStack stack) {
        CachedSeismogram cached = getCachedSeismogram(stack);
        return cached != null ? cached.snapshot() : null;
    }

    @Nullable
    public static MapSnapshot readSnapshotWithExactData(ItemStack stack) {
        CompoundTag geoTag = readGeoTag(stack);
        return geoTag != null ? readSnapshotInternal(geoTag, true) : null;
    }

    @Nullable
    private static CachedSeismogram getCachedSeismogram(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CachedSeismogram cached = SNAPSHOTS.getIfPresent(customData);
        if (cached != null) {
            return cached;
        }
        CompoundTag geoTag = readGeoTag(stack);
        if (geoTag == null) {
            return null;
        }
        cached = new CachedSeismogram(geoTag.getInt(TAG_CENTER_X), geoTag.getInt(TAG_CENTER_Z),
            readSnapshotInternal(geoTag, false));
        SNAPSHOTS.put(customData, cached);
        return cached;
    }

    @Nullable
    private static MapSnapshot readSnapshotInternal(CompoundTag geoTag, boolean includeExactClusters) {
        if (!geoTag.contains(TAG_STATION_X, Tag.TAG_INT)
            || !geoTag.contains(TAG_STATION_Y, Tag.TAG_INT)
            || !geoTag.contains(TAG_STATION_Z, Tag.TAG_INT)
            || !geoTag.contains(TAG_STATION_DIMENSION, Tag.TAG_STRING)) {
            return null;
        }

        int centerX = geoTag.getInt(TAG_CENTER_X);
        int centerY = geoTag.getInt(TAG_CENTER_Y);
        int centerZ = geoTag.getInt(TAG_CENTER_Z);
        BlockPos stationPos = new BlockPos(geoTag.getInt(TAG_STATION_X), geoTag.getInt(TAG_STATION_Y), geoTag.getInt(TAG_STATION_Z));
        String stationDimension = geoTag.getString(TAG_STATION_DIMENSION);
        List<MapSignal> signals = new ArrayList<>();
        List<ExactCluster> exactClusters = new ArrayList<>();
        ListTag entries = geoTag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (Tag raw : entries) {
            CompoundTag entryTag = (CompoundTag) raw;
            SeismicAnomalyType type = parseType(entryTag.getString(TAG_ENTRY_TYPE));
            if (type == null) {
                continue;
            }
            signals.add(new MapSignal(
                type,
                entryTag.getInt(TAG_ENTRY_WORLD_X),
                entryTag.getInt(TAG_ENTRY_WORLD_Z),
                entryTag.contains(TAG_ENTRY_WORLD_Y, Tag.TAG_INT) ? entryTag.getInt(TAG_ENTRY_WORLD_Y) : centerY
            ));
        }

        if (includeExactClusters) {
            ListTag clusterList = geoTag.getList(TAG_EXACT_CLUSTERS, Tag.TAG_COMPOUND);
            for (Tag rawCluster : clusterList) {
                CompoundTag clusterTag = (CompoundTag) rawCluster;
                SeismicAnomalyType type = parseType(clusterTag.getString(TAG_CLUSTER_TYPE));
                if (type == null) {
                    continue;
                }
                List<BlockPos> blocks = SeismicSurveyNbt.readBlocks(clusterTag, "X", "Y", "Z");
                if (!blocks.isEmpty()) {
                    exactClusters.add(new ExactCluster(type, List.copyOf(blocks)));
                }
            }
        }

        return new MapSnapshot(centerX, centerY, centerZ, stationPos, stationDimension, List.copyOf(signals), List.copyOf(exactClusters));
    }

    public static boolean isSeismogramStack(ItemStack stack) {
        return readGeoTag(stack) != null;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updateHeldMap(player, player.getMainHandItem());
        updateHeldMap(player, player.getOffhandItem());
    }

    private static void updateHeldMap(ServerPlayer player, ItemStack stack) {
        CachedSeismogram cached = getCachedSeismogram(stack);
        if (cached == null) {
            return;
        }

        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            return;
        }

        byte markerX = projectToMarker(player.getX(), cached.centerX());
        byte markerZ = projectToMarker(player.getZ(), cached.centerZ());
        byte markerRot = (byte) Math.floorMod(Mth.floor(player.getYRot() * 16.0F / 360.0F), 16);
        MarkerState markerState = new MarkerState(markerX, markerZ, markerRot);
        MarkerKey markerKey = new MarkerKey(player.getUUID(), mapId.id());

        if (markerState.equals(LAST_MARKERS.get(markerKey))) {
            return;
        }

        LAST_MARKERS.put(markerKey, markerState);
        GeoResonancePackets.sendSeismogramMarker(player, mapId.id(), markerX, markerZ, markerRot);
    }

    private static CompoundTag buildGeoTag(int centerX, int centerZ, BlockPos stationPos,
                                           String stationDimension,
                                           List<SeismicStationBlockEntity.MapEntry> entries,
                                           List<ExactCluster> exactClusters) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_CENTER_X, centerX);
        tag.putInt(TAG_CENTER_Y, stationPos.getY());
        tag.putInt(TAG_CENTER_Z, centerZ);
        tag.putInt(TAG_STATION_X, stationPos.getX());
        tag.putInt(TAG_STATION_Y, stationPos.getY());
        tag.putInt(TAG_STATION_Z, stationPos.getZ());
        tag.putString(TAG_STATION_DIMENSION, stationDimension);

        ListTag entryList = new ListTag();
        for (SeismicStationBlockEntity.MapEntry entry : entries) {
            int worldX = stationPos.getX() + entry.offsetX();
            int worldZ = stationPos.getZ() + entry.offsetZ();
            if (Math.abs(worldX - centerX) > WINDOW_HALF_BLOCKS || Math.abs(worldZ - centerZ) > WINDOW_HALF_BLOCKS) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_ENTRY_TYPE, entry.type().name());
            entryTag.putInt(TAG_ENTRY_WORLD_X, worldX);
            entryTag.putInt(TAG_ENTRY_WORLD_Z, worldZ);
            entryTag.putInt(TAG_ENTRY_WORLD_Y, entry.approxY());
            entryList.add(entryTag);
        }
        tag.put(TAG_ENTRIES, entryList);

        ListTag clusterList = new ListTag();
        for (ExactCluster cluster : exactClusters) {
            if (cluster.blocks().isEmpty()) {
                continue;
            }
            CompoundTag clusterTag = new CompoundTag();
            clusterTag.putString(TAG_CLUSTER_TYPE, cluster.type().name());
            SeismicSurveyNbt.writeBlocks(clusterTag, cluster.blocks());
            clusterList.add(clusterTag);
        }
        tag.put(TAG_EXACT_CLUSTERS, clusterList);
        return tag;
    }

    public static Tag compactSerializedMap(Tag serializedItem) {
        Tag copy = serializedItem.copy();
        if (copy instanceof CompoundTag item) {
            CompoundTag survey = item.getCompound("components").getCompound("minecraft:custom_data").getCompound(TAG_ROOT);
            SeismicSurveyNbt.compactLegacyClusters(survey.getList(TAG_EXACT_CLUSTERS, Tag.TAG_COMPOUND));
        }
        return copy;
    }

    public static void compactLegacyMap(ItemStack stack) {
        CompoundTag survey = readGeoTag(stack);
        if (survey == null) {
            return;
        }
        ListTag clusters = survey.getList(TAG_EXACT_CLUSTERS, Tag.TAG_COMPOUND);
        for (Tag raw : clusters) {
            if (((CompoundTag) raw).contains("Blocks", Tag.TAG_LIST)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                    SeismicSurveyNbt.compactLegacyClusters(tag.getCompound(TAG_ROOT).getList(TAG_EXACT_CLUSTERS, Tag.TAG_COMPOUND)));
                return;
            }
        }
    }

    private static void renderStaticMap(MapItemSavedData mapData, CompoundTag geoTag) {
        byte background = MapColor.SAND.getPackedId(MapColor.Brightness.LOWEST);
        for (int i = 0; i < mapData.colors.length; i++) {
            mapData.colors[i] = background;
        }
        mapData.setDirty();

        int centerX = geoTag.getInt(TAG_CENTER_X);
        int centerZ = geoTag.getInt(TAG_CENTER_Z);

        ListTag entries = geoTag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (Tag raw : entries) {
            CompoundTag entryTag = (CompoundTag) raw;
            SeismicAnomalyType type = parseType(entryTag.getString(TAG_ENTRY_TYPE));
            if (type == null) {
                continue;
            }

            int worldX = entryTag.getInt(TAG_ENTRY_WORLD_X);
            int worldZ = entryTag.getInt(TAG_ENTRY_WORLD_Z);
            paintDotWorld(mapData, centerX, centerZ, worldX, worldZ, colorFor(type), SIGNAL_DOT_RADIUS);
        }
    }

    private static void paintDotWorld(MapItemSavedData mapData, int centerX, int centerZ, int worldX, int worldZ, MapColor color, int radius) {
        int px = Mth.clamp(projectToPixel(worldX, centerX), 0, MAP_SIZE - 1);
        int pz = Mth.clamp(projectToPixel(worldZ, centerZ), 0, MAP_SIZE - 1);
        paintDotPixel(mapData, px, pz, color, radius);
    }

    private static void paintDotPixel(MapItemSavedData mapData, int px, int pz, MapColor color, int radius) {
        byte core = color.getPackedId(MapColor.Brightness.HIGH);
        byte edge = color.getPackedId(MapColor.Brightness.NORMAL);
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = px + dx;
                int z = pz + dz;
                if (x < 0 || x >= MAP_SIZE || z < 0 || z >= MAP_SIZE) {
                    continue;
                }
                mapData.setColor(x, z, (dx == 0 && dz == 0) ? core : edge);
            }
        }
    }

    private static int projectToPixel(double worldCoord, int centerCoord) {
        double min = centerCoord - WINDOW_HALF_BLOCKS;
        double normalized = (worldCoord - min) / WINDOW_DIAMETER_BLOCKS;
        return Mth.floor(normalized * MAP_SIZE);
    }

    private static byte projectToMarker(double worldCoord, int centerCoord) {
        double min = centerCoord - WINDOW_HALF_BLOCKS;
        double normalized = (worldCoord - min) / WINDOW_DIAMETER_BLOCKS;
        double mapPixels = normalized * MAP_SIZE;
        int marker = Mth.floor(mapPixels * 2.0D - 128.0D);
        return (byte) Mth.clamp(marker, -128, 127);
    }

    private static int centerOfChunk(int blockCoord) {
        return (Mth.floorDiv(blockCoord, 16) * 16) + 8;
    }

    private static MapColor colorFor(SeismicAnomalyType type) {
        return switch (type.kind()) {
            case CAVE -> MapColor.COLOR_LIGHT_GRAY;
            case WATER -> MapColor.WATER;
            case LAVA -> MapColor.COLOR_ORANGE;
            case COAL, COAL_DEPOSIT -> MapColor.COLOR_BLACK;
            case IRON, IRON_DEPOSIT -> MapColor.METAL;
            case COPPER, COPPER_DEPOSIT -> MapColor.COLOR_BROWN;
            case GOLD, GOLD_DEPOSIT -> MapColor.COLOR_YELLOW;
            case REDSTONE, REDSTONE_DEPOSIT -> MapColor.COLOR_RED;
            case LAPIS, LAPIS_DEPOSIT -> MapColor.COLOR_BLUE;
            case EMERALD -> MapColor.COLOR_GREEN;
            case DIAMOND -> MapColor.COLOR_LIGHT_BLUE;
            case ZINC, ZINC_DEPOSIT -> MapColor.COLOR_GRAY;
            case CRIMSITE -> MapColor.TERRACOTTA_RED;
            case ASURINE -> MapColor.COLOR_LIGHT_BLUE;
            case OCHRUM -> MapColor.TERRACOTTA_YELLOW;
            case VERIDIUM -> MapColor.TERRACOTTA_GREEN;
            case THORIUM, THORIUM_DEPOSIT -> MapColor.COLOR_LIGHT_GREEN;
            case MAGNETITE -> MapColor.COLOR_GRAY;
            case LEAD, LEAD_DEPOSIT -> MapColor.TERRACOTTA_BLUE;
            case NICKEL, NICKEL_DEPOSIT -> MapColor.COLOR_LIGHT_GREEN;
            case LITHIUM -> MapColor.COLOR_PINK;
            case BAUXITE -> MapColor.TERRACOTTA_ORANGE;
            case GALENA -> MapColor.TERRACOTTA_PURPLE;
            case LIGNITE -> MapColor.COLOR_BROWN;
            case SULFUR -> MapColor.COLOR_YELLOW;
            case CRUDE_OIL -> MapColor.COLOR_BLACK;
            case WOLFRAMITE, WOLFRAMITE_DEPOSIT -> MapColor.COLOR_LIGHT_GRAY;
            case URANIUM, URANIUM_DEPOSIT -> MapColor.COLOR_GREEN;
            case AUTUNITE -> MapColor.COLOR_LIGHT_GREEN;
            case NITRATE -> MapColor.SNOW;
            case TIN, TIN_DEPOSIT -> MapColor.METAL;
            case SILVER, SILVER_DEPOSIT -> MapColor.COLOR_LIGHT_GRAY;
            case PLATINUM, PLATINUM_DEPOSIT -> MapColor.COLOR_PURPLE;
            case OSMIUM, OSMIUM_DEPOSIT -> MapColor.COLOR_BLUE;
            case QUARTZ, QUARTZ_DEPOSIT -> MapColor.QUARTZ;
            case ANCIENT_DEBRIS -> MapColor.TERRACOTTA_BROWN;
            case FLUORITE -> MapColor.COLOR_LIGHT_BLUE;
            case CERTUS_QUARTZ -> MapColor.COLOR_LIGHT_BLUE;
            case INFERIUM -> MapColor.COLOR_LIGHT_GREEN;
            case PROSPERITY -> MapColor.SNOW;
            case SOULIUM -> MapColor.TERRACOTTA_BROWN;
            case ARCANE_CRYSTAL -> MapColor.COLOR_BLUE;
            case RUNE -> MapColor.COLOR_PURPLE;
            case STELLARITE -> MapColor.COLOR_YELLOW;
            case URANINITE -> MapColor.COLOR_LIGHT_GREEN;
            case DRACONIUM -> MapColor.COLOR_PURPLE;
            case BLACK_QUARTZ -> MapColor.COLOR_GRAY;
            case COBALT, COBALT_DEPOSIT -> MapColor.COLOR_BLUE;
            case AMETHYST -> MapColor.COLOR_PURPLE;
            case CHEST -> MapColor.WOOD;
            case SPAWNER -> MapColor.COLOR_BLACK;
            case SOLID -> MapColor.STONE;
            case CUSTOM -> SeismicCustomModules.mapColor(type);
        };
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private static CompoundTag readGeoTag(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        // Read only: never mutate or expose this tag; returned snapshots contain immutable values.
        CompoundTag root = customData.getUnsafe();
        return root.contains(TAG_ROOT, Tag.TAG_COMPOUND) ? root.getCompound(TAG_ROOT) : null;
    }

    private static SeismicAnomalyType parseType(String raw) {
        if (raw.indexOf(':') >= 0) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(raw);
            return id != null ? SeismicAnomalyType.custom(id) : null;
        }
        return switch (raw) {
            case "CAVE" -> SeismicAnomalyType.CAVE;
            case "WATER" -> SeismicAnomalyType.WATER;
            case "LAVA" -> SeismicAnomalyType.LAVA;
            case "COAL" -> SeismicAnomalyType.COAL;
            case "IRON" -> SeismicAnomalyType.IRON;
            case "COPPER" -> SeismicAnomalyType.COPPER;
            case "GOLD" -> SeismicAnomalyType.GOLD;
            case "REDSTONE" -> SeismicAnomalyType.REDSTONE;
            case "LAPIS" -> SeismicAnomalyType.LAPIS;
            case "EMERALD" -> SeismicAnomalyType.EMERALD;
            case "DIAMOND" -> SeismicAnomalyType.DIAMOND;
            case "ZINC" -> SeismicAnomalyType.ZINC;
            case "CRIMSITE" -> SeismicAnomalyType.CRIMSITE;
            case "ASURINE" -> SeismicAnomalyType.ASURINE;
            case "OCHRUM" -> SeismicAnomalyType.OCHRUM;
            case "VERIDIUM" -> SeismicAnomalyType.VERIDIUM;
            case "THORIUM" -> SeismicAnomalyType.THORIUM;
            case "MAGNETITE" -> SeismicAnomalyType.MAGNETITE;
            case "LEAD" -> SeismicAnomalyType.LEAD;
            case "NICKEL" -> SeismicAnomalyType.NICKEL;
            case "LITHIUM" -> SeismicAnomalyType.LITHIUM;
            case "BAUXITE" -> SeismicAnomalyType.BAUXITE;
            case "GALENA" -> SeismicAnomalyType.GALENA;
            case "LIGNITE" -> SeismicAnomalyType.LIGNITE;
            case "SULFUR" -> SeismicAnomalyType.SULFUR;
            case "CRUDE_OIL" -> SeismicAnomalyType.CRUDE_OIL;
            case "WOLFRAMITE" -> SeismicAnomalyType.WOLFRAMITE;
            case "URANIUM" -> SeismicAnomalyType.URANIUM;
            case "AUTUNITE" -> SeismicAnomalyType.AUTUNITE;
            case "NITRATE" -> SeismicAnomalyType.NITRATE;
            case "TIN" -> SeismicAnomalyType.TIN;
            case "SILVER" -> SeismicAnomalyType.SILVER;
            case "PLATINUM" -> SeismicAnomalyType.PLATINUM;
            case "OSMIUM" -> SeismicAnomalyType.OSMIUM;
            case "QUARTZ" -> SeismicAnomalyType.QUARTZ;
            case "ANCIENT_DEBRIS" -> SeismicAnomalyType.ANCIENT_DEBRIS;
            case "FLUORITE" -> SeismicAnomalyType.FLUORITE;
            case "CERTUS_QUARTZ" -> SeismicAnomalyType.CERTUS_QUARTZ;
            case "INFERIUM" -> SeismicAnomalyType.INFERIUM;
            case "PROSPERITY" -> SeismicAnomalyType.PROSPERITY;
            case "SOULIUM" -> SeismicAnomalyType.SOULIUM;
            case "ARCANE_CRYSTAL" -> SeismicAnomalyType.ARCANE_CRYSTAL;
            case "RUNE" -> SeismicAnomalyType.RUNE;
            case "STELLARITE" -> SeismicAnomalyType.STELLARITE;
            case "URANINITE" -> SeismicAnomalyType.URANINITE;
            case "DRACONIUM" -> SeismicAnomalyType.DRACONIUM;
            case "BLACK_QUARTZ" -> SeismicAnomalyType.BLACK_QUARTZ;
            case "COBALT" -> SeismicAnomalyType.COBALT;
            case "COAL_DEPOSIT" -> SeismicAnomalyType.COAL_DEPOSIT;
            case "IRON_DEPOSIT" -> SeismicAnomalyType.IRON_DEPOSIT;
            case "COPPER_DEPOSIT" -> SeismicAnomalyType.COPPER_DEPOSIT;
            case "GOLD_DEPOSIT" -> SeismicAnomalyType.GOLD_DEPOSIT;
            case "REDSTONE_DEPOSIT" -> SeismicAnomalyType.REDSTONE_DEPOSIT;
            case "LAPIS_DEPOSIT" -> SeismicAnomalyType.LAPIS_DEPOSIT;
            case "ZINC_DEPOSIT" -> SeismicAnomalyType.ZINC_DEPOSIT;
            case "LEAD_DEPOSIT" -> SeismicAnomalyType.LEAD_DEPOSIT;
            case "NICKEL_DEPOSIT" -> SeismicAnomalyType.NICKEL_DEPOSIT;
            case "URANIUM_DEPOSIT" -> SeismicAnomalyType.URANIUM_DEPOSIT;
            case "THORIUM_DEPOSIT" -> SeismicAnomalyType.THORIUM_DEPOSIT;
            case "WOLFRAMITE_DEPOSIT" -> SeismicAnomalyType.WOLFRAMITE_DEPOSIT;
            case "TIN_DEPOSIT" -> SeismicAnomalyType.TIN_DEPOSIT;
            case "SILVER_DEPOSIT" -> SeismicAnomalyType.SILVER_DEPOSIT;
            case "PLATINUM_DEPOSIT" -> SeismicAnomalyType.PLATINUM_DEPOSIT;
            case "OSMIUM_DEPOSIT" -> SeismicAnomalyType.OSMIUM_DEPOSIT;
            case "QUARTZ_DEPOSIT" -> SeismicAnomalyType.QUARTZ_DEPOSIT;
            case "COBALT_DEPOSIT" -> SeismicAnomalyType.COBALT_DEPOSIT;
            case "AMETHYST" -> SeismicAnomalyType.AMETHYST;
            case "CHEST" -> SeismicAnomalyType.CHEST;
            case "SPAWNER" -> SeismicAnomalyType.SPAWNER;
            case "SOLID" -> SeismicAnomalyType.SOLID;
            default -> null;
        };
    }

    private record CachedSeismogram(int centerX, int centerZ, @Nullable MapSnapshot snapshot) {
    }

    private record MarkerKey(UUID playerId, int mapId) {
    }

    private record MarkerState(byte markerX, byte markerZ, byte markerRot) {
    }

    public record MapSnapshot(int centerX, int centerY, int centerZ, BlockPos stationPos, String stationDimension,
                              List<MapSignal> signals, List<ExactCluster> exactClusters) {
    }

    public record MapSignal(SeismicAnomalyType type, int worldX, int worldZ, int approxY) {
    }

    public record ExactCluster(SeismicAnomalyType type, List<BlockPos> blocks) {
    }
}
