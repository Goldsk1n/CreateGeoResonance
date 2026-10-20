package net.goldskinmc.creategeoresonance.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleDefinition;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationData;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationControllerLogic;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorData;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(CreateGeoResonanceMod.MODID)
@EventBusSubscriber(modid = CreateGeoResonanceMod.MODID)
public final class SeismicCustomModuleGameTests {
    private SeismicCustomModuleGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(SeismicCustomModuleGameTests.class);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void customSurveyAndProjectorSaveRoundTripsKeepDistinctIdentities(GameTestHelper helper) {
        SeismicAnomalyType first = SeismicAnomalyType.valueOf("geo_test:first");
        SeismicAnomalyType second = SeismicAnomalyType.valueOf("geo_test:second");
        BlockPos station = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos oreA = station.below(2);
        BlockPos oreB = oreA.east(3);
        List<SeismogramMapService.ExactCluster> clusters = List.of(
            new SeismogramMapService.ExactCluster(first, List.of(oreA)),
            new SeismogramMapService.ExactCluster(second, List.of(oreB)));
        ItemStack map = SeismogramMapService.createMap(helper.getLevel(), station,
            List.of(new SeismicStationBlockEntity.MapEntry(first, 0, 0, oreA.getY()),
                new SeismicStationBlockEntity.MapEntry(second, 3, 0, oreB.getY())), clusters);
        var snapshot = SeismogramMapService.readSnapshotWithExactData(map);
        helper.assertTrue(snapshot != null && snapshot.signals().get(0).type() == first
            && snapshot.signals().get(1).type() == second, "Custom map signal IDs must round-trip");
        helper.assertTrue(snapshot.exactClusters().get(0).type() == first
            && snapshot.exactClusters().get(1).type() == second, "Custom exact IDs must round-trip");

        SeismicProjectorData projector = new SeismicProjectorData();
        String dimension = helper.getLevel().dimension().location().toString();
        projector.tryLoadSnapshot(dimension, station, snapshot);
        int spacing = Math.max(8, net.goldskinmc.creategeoresonance.Config.PROJECTOR_MIN_NODE_DISTANCE_BLOCKS.get());
        BlockPos otherStation = station.east(spacing);
        var other = new SeismogramMapService.MapSnapshot(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ(),
            otherStation, dimension, snapshot.signals(), snapshot.exactClusters());
        projector.tryLoadSnapshot(dimension, station, other);
        CompoundTag saved = new CompoundTag();
        projector.writeToTag(saved);
        SeismicProjectorData restored = new SeismicProjectorData();
        restored.readFromTag(saved);
        helper.assertTrue(restored.getConfirmedVeins().stream().map(SeismicProjectorData.ExactVein::type)
            .collect(java.util.stream.Collectors.toSet()).equals(Set.of(first, second)),
            "Projector save must retain both distinct custom veins");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void customDefinitionsSyncAndMissingDefinitionsPreserveInstalledItems(GameTestHelper helper) {
        CompoundTag previous = SeismicCustomModules.writeDefinitions();
        try {
            ResourceLocation idA = ResourceLocation.fromNamespaceAndPath("geo_test", "module_a");
            ResourceLocation idB = ResourceLocation.fromNamespaceAndPath("geo_test", "module_b");
            CompoundTag definitions = new CompoundTag();
            ListTag list = new ListTag();
            list.add(definitionTag(idA, "First", "#CC4455"));
            list.add(definitionTag(idB, "Second", "#44CCDD"));
            definitions.put("Definitions", list);
            SeismicCustomModules.readDefinitions(definitions);
            ItemStack first = SeismicCustomModuleItem.create(idA);
            ItemStack second = SeismicCustomModuleItem.create(idB);
            helper.assertTrue(SeismicCustomModuleItem.isConfigured(first) && SeismicCustomModuleItem.isConfigured(second),
                "Both variants must resolve after definition sync");
            SeismicAnomalyType typeA = SeismicModuleItem.getDetectsType(first);
            SeismicAnomalyType typeB = SeismicModuleItem.getDetectsType(second);
            var inventory = new SeismicStationData().inventory();
            inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START, first);
            inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 1, second);
            helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(Set.of(typeA, typeB)),
                "Different custom modules must contribute separate targets");
            var priority = SeismicStationControllerLogic.buildModulePriorityMap(inventory);
            helper.assertTrue(SeismicCustomModules.detectableBlocks(Set.of(typeA, typeB), priority).get(Blocks.STONE) == typeB,
                "Overlapping custom tags must prefer the last installed module");
            helper.assertTrue(SeismicCustomModules.detectableBlocks(Set.of(typeA, typeB), Map.of(typeA, 9, typeB, 1)).get(Blocks.STONE) == typeA,
                "Reversed priorities must reverse overlap ownership");
            ItemStack savedCopy = ItemStack.parse(helper.getLevel().registryAccess(), first.save(helper.getLevel().registryAccess())).orElseThrow();
            helper.assertTrue(SeismicModuleItem.getDetectsType(savedCopy) == typeA, "Module ID must survive item serialization");
            SeismicCustomModules.readDefinitions(new CompoundTag());
            helper.assertFalse(SeismicCustomModuleItem.isConfigured(first), "Removed definitions must not remain active");
            helper.assertTrue(SeismicModuleItem.getDetectsType(first) == typeA
                && SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(Set.of(typeA, typeB)),
                "Missing definitions must retain identities without enabling default ambient detections");
            helper.assertTrue(SeismicStationControllerLogic.getInstalledModuleStacks(inventory).size() == 2,
                "Missing installed modules must remain extractable");
            SeismicCustomModules.readDefinitions(definitions);
            helper.assertTrue(SeismicCustomModuleItem.isConfigured(first), "Restoring a definition ID must restore recognition");
        } finally {
            SeismicCustomModules.readDefinitions(previous);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void customIconSizesDefaultAndSynchronize(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("geo_test", "icon_size");
        JsonObject json = definitionJson("Sized", "#123456");
        var standard = SeismicCustomModuleDefinition.parse(id, json);
        helper.assertTrue(standard.iconSize() == SeismicCustomModuleDefinition.IconSize.STANDARD,
            "Existing definitions without icon_size must retain the standard model");
        helper.assertTrue(SeismicCustomModuleDefinition.parse(id, standard.toJson()).equals(standard),
            "Standard size must survive definition serialization");
        json.addProperty("icon_size", "standard");
        helper.assertTrue(SeismicCustomModuleDefinition.parse(id, json).equals(standard),
            "Explicit standard size must match the omitted default");

        json.addProperty("icon_size", "small");
        var small = SeismicCustomModuleDefinition.parse(id, json);
        helper.assertTrue(small.iconSize() == SeismicCustomModuleDefinition.IconSize.SMALL
            && small.toJson().get("icon_size").getAsString().equals("small"),
            "Small size must be accepted and serialized");
        CompoundTag previous = SeismicCustomModules.writeDefinitions();
        try {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", id.toString());
            entry.putString("Json", small.toJson().toString());
            ListTag entries = new ListTag();
            entries.add(entry);
            CompoundTag definitions = new CompoundTag();
            definitions.put("Definitions", entries);
            SeismicCustomModules.readDefinitions(definitions);
            SeismicCustomModules.readDefinitions(SeismicCustomModules.writeDefinitions());
            helper.assertTrue(SeismicCustomModules.definition(SeismicAnomalyType.custom(id)).iconSize()
                == SeismicCustomModuleDefinition.IconSize.SMALL, "Definition sync must preserve the small model choice");
        } finally {
            SeismicCustomModules.readDefinitions(previous);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void invalidCustomDefinitionIsRejected(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("geo_test", "validation");
        JsonObject json = definitionJson("Validation", "#123456");
        json.addProperty("color", "purple");
        boolean rejected = false;
        try {
            SeismicCustomModuleDefinition.parse(id, json);
        } catch (JsonParseException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Invalid color must produce a definition error");
        json = definitionJson("Validation", "#123456");
        json.addProperty("typo_field", "ignored?");
        rejected = false;
        try {
            SeismicCustomModuleDefinition.parse(id, json);
        } catch (JsonParseException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Unknown fields must not be silently ignored");
        json = definitionJson("Validation", "#123456");
        json.addProperty("icon_size", "tiny");
        rejected = false;
        try {
            SeismicCustomModuleDefinition.parse(id, json);
        } catch (JsonParseException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Unsupported icon sizes must not silently fall back to standard");
        helper.succeed();
    }

    private static CompoundTag definitionTag(ResourceLocation id, String name, String color) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        tag.putString("Json", definitionJson(name, color).toString());
        return tag;
    }

    private static JsonObject definitionJson(String name, String color) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name + " Module");
        json.addProperty("signal_name", name);
        json.addProperty("icon", "minecraft:diamond");
        json.addProperty("block_tag", "minecraft:base_stone_overworld");
        json.addProperty("color", color);
        return json;
    }
}
