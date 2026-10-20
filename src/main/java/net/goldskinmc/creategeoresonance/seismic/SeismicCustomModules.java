package net.goldskinmc.creategeoresonance.seismic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.Reader;

public final class SeismicCustomModules extends SimpleJsonResourceReloadListener {
    public static final int MAX_DEFINITIONS = 128;
    private static volatile Map<String, SeismicCustomModuleDefinition> definitions = Map.of();
    private static volatile int revision;

    private SeismicCustomModules() {
        super(new Gson(), "seismic_modules");
    }

    public static void register(IEventBus bus) {
        bus.addListener((AddReloadListenerEvent event) -> event.addListener(new SeismicCustomModules()));
        bus.addListener((OnDatapackSyncEvent event) -> {
            CompoundTag data = writeDefinitions();
            var players = event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers();
            players.forEach(player -> GeoResonancePackets.sendCustomModules(player, data));
        });
        bus.addListener((ServerStoppedEvent event) -> replaceDefinitions(Map.of()));
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> resources = new LinkedHashMap<>();
        FileToIdConverter converter = FileToIdConverter.json("seismic_modules");
        for (var entry : converter.listMatchingResources(manager).entrySet()) {
            ResourceLocation id = converter.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    throw new JsonParseException("Module definition must be an object");
                }
                resources.put(id, json);
            } catch (IOException | JsonParseException exception) {
                throw new JsonParseException("Failed to load custom seismic module " + id, exception);
            }
        }
        return resources;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        if (resources.size() > MAX_DEFINITIONS) {
            throw new JsonParseException("At most " + MAX_DEFINITIONS + " custom seismic modules are supported");
        }
        Map<String, SeismicCustomModuleDefinition> loaded = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            SeismicCustomModuleDefinition definition = SeismicCustomModuleDefinition.parse(entry.getKey(), entry.getValue().getAsJsonObject());
            loaded.put(definition.id().toString(), definition);
        });
        replaceDefinitions(loaded);
        LogUtils.getLogger().info("Loaded {} custom seismic module definitions", loaded.size());
    }

    private static synchronized void replaceDefinitions(Map<String, SeismicCustomModuleDefinition> loaded) {
        loaded.values().forEach(definition -> SeismicAnomalyType.custom(definition.id()));
        definitions = Map.copyOf(loaded);
        revision++;
    }

    public static int revision() {
        return revision;
    }

    public static Collection<SeismicCustomModuleDefinition> definitions() {
        return definitions.values().stream().sorted(Comparator.comparing(value -> value.id().toString())).toList();
    }

    @Nullable
    public static SeismicCustomModuleDefinition definition(SeismicAnomalyType type) {
        return definitions.get(type.name());
    }

    public static int color(SeismicAnomalyType type) {
        SeismicCustomModuleDefinition definition = definition(type);
        return definition != null ? definition.color() : 0xAAAAAA;
    }

    public static Component signalName(SeismicAnomalyType type) {
        SeismicCustomModuleDefinition definition = definition(type);
        return definition != null ? definition.signalDisplayName() : Component.literal(type.name());
    }

    public static boolean matches(net.minecraft.world.level.block.state.BlockState state, SeismicAnomalyType type) {
        SeismicCustomModuleDefinition definition = definition(type);
        return definition != null && !state.isAir() && state.is(definition.blockTag()) && state.getFluidState().isEmpty();
    }

    public static Map<Block, SeismicAnomalyType> detectableBlocks(Set<SeismicAnomalyType> types,
                                                                 Map<SeismicAnomalyType, Integer> priorities) {
        Map<Block, SeismicAnomalyType> targets = new HashMap<>();
        // Build once per scan. Overlapping custom tags use installed slot priority, then stable ID.
        List<SeismicAnomalyType> ordered = types.stream().filter(SeismicAnomalyType::isCustom)
            .sorted(Comparator.<SeismicAnomalyType>comparingInt(type -> priorities.getOrDefault(type, -1))
                .thenComparing(SeismicAnomalyType::name)).toList();
        for (SeismicAnomalyType type : ordered) {
            SeismicCustomModuleDefinition definition = definition(type);
            if (definition != null) {
                BuiltInRegistries.BLOCK.getTag(definition.blockTag())
                    .ifPresent(holders -> holders.forEach(holder -> targets.put(holder.value(), type)));
            }
        }
        return Map.copyOf(targets);
    }

    public static MapColor mapColor(SeismicAnomalyType type) {
        int rgb = color(type);
        MapColor closest = MapColor.COLOR_GRAY;
        long best = Long.MAX_VALUE;
        for (int index = 1; index < 64; index++) {
            MapColor candidate = MapColor.byId(index);
            if (candidate == MapColor.NONE) {
                continue;
            }
            long dr = (rgb >> 16 & 255) - (candidate.col >> 16 & 255);
            long dg = (rgb >> 8 & 255) - (candidate.col >> 8 & 255);
            long db = (rgb & 255) - (candidate.col & 255);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    public static CompoundTag writeDefinitions() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (SeismicCustomModuleDefinition definition : definitions()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", definition.id().toString());
            entry.putString("Json", definition.toJson().toString());
            entries.add(entry);
        }
        tag.put("Definitions", entries);
        return tag;
    }

    public static void readDefinitions(CompoundTag tag) {
        ListTag entries = tag.getList("Definitions", Tag.TAG_COMPOUND);
        if (entries.size() > MAX_DEFINITIONS) {
            throw new JsonParseException("Too many synced custom seismic modules");
        }
        Map<String, SeismicCustomModuleDefinition> loaded = new LinkedHashMap<>();
        for (Tag value : entries) {
            CompoundTag entry = (CompoundTag) value;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            String json = entry.getString("Json");
            if (id == null || json.length() > 2048) {
                throw new JsonParseException("Invalid synced custom seismic module");
            }
            SeismicCustomModuleDefinition definition = SeismicCustomModuleDefinition.parse(id, JsonParser.parseString(json).getAsJsonObject());
            if (loaded.put(id.toString(), definition) != null) {
                throw new JsonParseException("Duplicate synced module: " + id);
            }
        }
        replaceDefinitions(loaded);
    }
}
