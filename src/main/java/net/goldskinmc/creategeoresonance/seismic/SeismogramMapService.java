package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.event.TickEvent;
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

    private static final int WINDOW_HALF_BLOCKS = 24;
    private static final int WINDOW_DIAMETER_BLOCKS = WINDOW_HALF_BLOCKS * 2;
    private static final int MAP_SIZE = 128;
    private static final int SIGNAL_DOT_RADIUS = 2;
    private static final Map<MarkerKey, MarkerState> LAST_MARKERS = new HashMap<>();

    private SeismogramMapService() {
    }

    public static ItemStack createMap(ServerLevel level, BlockPos stationPos, List<SeismicStationBlockEntity.MapEntry> entries) {
        int centerX = centerOfChunk(stationPos.getX());
        int centerZ = centerOfChunk(stationPos.getZ());
        ItemStack mapStack = MapItem.create(level, centerX, centerZ, (byte) 0, false, false);
        MapItemSavedData mapData = MapItem.getSavedData(mapStack, level);
        if (mapData == null) {
            return mapStack;
        }

        CompoundTag geoTag = buildGeoTag(centerX, centerZ, stationPos, entries);
        mapStack.getOrCreateTag().put(TAG_ROOT, geoTag);

        renderStaticMap(mapData, geoTag);

        MapItem.lockMap(level, mapStack);
        mapStack.setHoverName(Component.translatable("item.creategeoresonance.seismogram"));
        return mapStack;
    }

    public static void clear() {
        LAST_MARKERS.clear();
    }

    @Nullable
    public static MapSnapshot readSnapshot(ItemStack stack) {
        CompoundTag geoTag = readGeoTag(stack);
        if (geoTag == null) {
            return null;
        }

        int centerX = geoTag.getInt(TAG_CENTER_X);
        int centerY = geoTag.getInt(TAG_CENTER_Y);
        int centerZ = geoTag.getInt(TAG_CENTER_Z);
        List<MapSignal> signals = new ArrayList<>();
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

        return new MapSnapshot(centerX, centerY, centerZ, List.copyOf(signals));
    }

    public static boolean isSeismogramStack(ItemStack stack) {
        return readGeoTag(stack) != null;
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        updateHeldMap(player, player.getMainHandItem());
        updateHeldMap(player, player.getOffhandItem());
    }

    private static void updateHeldMap(ServerPlayer player, ItemStack stack) {
        CompoundTag geoTag = readGeoTag(stack);
        if (geoTag == null) {
            return;
        }

        Integer mapId = MapItem.getMapId(stack);
        if (mapId == null) {
            return;
        }

        int centerX = geoTag.getInt(TAG_CENTER_X);
        int centerZ = geoTag.getInt(TAG_CENTER_Z);
        byte markerX = projectToMarker(player.getX(), centerX);
        byte markerZ = projectToMarker(player.getZ(), centerZ);
        byte markerRot = (byte) Math.floorMod(Mth.floor(player.getYRot() * 16.0F / 360.0F), 16);
        MarkerState markerState = new MarkerState(markerX, markerZ, markerRot);
        MarkerKey markerKey = new MarkerKey(player.getUUID(), mapId);

        if (markerState.equals(LAST_MARKERS.get(markerKey))) {
            return;
        }

        LAST_MARKERS.put(markerKey, markerState);
        GeoResonancePackets.sendSeismogramMarker(player, mapId, markerX, markerZ, markerRot);
    }

    private static CompoundTag buildGeoTag(int centerX, int centerZ, BlockPos stationPos,
                                           List<SeismicStationBlockEntity.MapEntry> entries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_CENTER_X, centerX);
        tag.putInt(TAG_CENTER_Y, stationPos.getY());
        tag.putInt(TAG_CENTER_Z, centerZ);

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
        return tag;
    }

    private static void renderStaticMap(MapItemSavedData mapData, CompoundTag geoTag) {
        byte background = MapColor.SAND.getPackedId(MapColor.Brightness.LOWEST);
        for (int i = 0; i < mapData.colors.length; i++) {
            mapData.colors[i] = background;
        }
        mapData.setDirty();

        int centerX = geoTag.getInt(TAG_CENTER_X);
        int centerZ = geoTag.getInt(TAG_CENTER_Z);
        paintDotWorld(mapData, centerX, centerZ, centerX, centerZ, MapColor.COLOR_BLACK, 1);

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
        return switch (type) {
            case CAVE -> MapColor.COLOR_LIGHT_GRAY;
            case WATER -> MapColor.WATER;
            case LAVA -> MapColor.COLOR_ORANGE;
            case COAL -> MapColor.COLOR_BLACK;
            case IRON -> MapColor.METAL;
            case COPPER -> MapColor.COLOR_BROWN;
            case GOLD -> MapColor.COLOR_YELLOW;
            case REDSTONE -> MapColor.COLOR_RED;
            case LAPIS -> MapColor.COLOR_BLUE;
            case EMERALD -> MapColor.COLOR_GREEN;
            case DIAMOND -> MapColor.COLOR_LIGHT_BLUE;
            case ZINC -> MapColor.COLOR_GRAY;
            case AMETHYST -> MapColor.COLOR_PURPLE;
            case CHEST -> MapColor.WOOD;
            case SPAWNER -> MapColor.COLOR_BLACK;
            case SOLID -> MapColor.STONE;
        };
    }

    private static CompoundTag readGeoTag(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP) || !stack.hasTag()) {
            return null;
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT, Tag.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompound(TAG_ROOT);
    }

    private static SeismicAnomalyType parseType(String raw) {
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
            case "AMETHYST" -> SeismicAnomalyType.AMETHYST;
            case "CHEST" -> SeismicAnomalyType.CHEST;
            case "SPAWNER" -> SeismicAnomalyType.SPAWNER;
            case "SOLID" -> SeismicAnomalyType.SOLID;
            default -> null;
        };
    }

    private record MarkerKey(UUID playerId, int mapId) {
    }

    private record MarkerState(byte markerX, byte markerZ, byte markerRot) {
    }

    public record MapSnapshot(int centerX, int centerY, int centerZ, List<MapSignal> signals) {
    }

    public record MapSignal(SeismicAnomalyType type, int worldX, int worldZ, int approxY) {
    }
}
