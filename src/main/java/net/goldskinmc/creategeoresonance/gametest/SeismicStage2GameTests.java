package net.goldskinmc.creategeoresonance.gametest;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationControllerLogic;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorData;
import net.goldskinmc.creategeoresonance.seismic.SeismicSurveyNbt;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@GameTestHolder(CreateGeoResonanceMod.MODID)
@EventBusSubscriber(modid = CreateGeoResonanceMod.MODID)
public final class SeismicStage2GameTests {
    private SeismicStage2GameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(SeismicStage2GameTests.class);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void stationCreatesAllBoundingParts(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION.get(), fixture.controller());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.lowerLeft());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.upperRight());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.upperLeft());
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void stationStaysAfterBreakingSupportBlocks(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));

        helper.setBlock(fixture.controller().below(), net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(fixture.lowerLeft().below(), net.minecraft.world.level.block.Blocks.AIR);

        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION.get(), fixture.controller());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.lowerLeft());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.upperRight());
        helper.assertBlockPresent(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), fixture.upperLeft());
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void survivalBreakFromBoundingDropsOneController(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        BlockPos targetPos = helper.absolutePos(fixture.upperLeft());

        boolean broken = player.gameMode.destroyBlock(targetPos);
        helper.assertTrue(broken, "Survival player should be able to break station bounding part");
        helper.assertBlockNotPresent(GeoResonanceBlocks.SEISMIC_STATION.get(), fixture.controller());
        helper.assertTrue(countStationDrops(helper, fixture.controller()) == 1, "Expected exactly one station drop in survival");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void creativeBreakFromBoundingDropsNothing(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);

        int dropsBefore = countStationDrops(helper, fixture.controller());
        BlockPos targetPos = helper.absolutePos(fixture.upperLeft());
        boolean broken = player.gameMode.destroyBlock(targetPos);

        helper.assertTrue(broken, "Creative player should be able to break station bounding part");
        helper.assertBlockNotPresent(GeoResonanceBlocks.SEISMIC_STATION.get(), fixture.controller());
        int dropsAfter = countStationDrops(helper, fixture.controller());
        helper.assertTrue(dropsAfter == dropsBefore, "Creative destroy should not drop station");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void seismogramStaticMapHasNoCenterDot(GameTestHelper helper) {
        BlockPos stationWorldPos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack map = SeismogramMapService.createMap(helper.getLevel(), stationWorldPos, List.of(), List.of());
        MapItemSavedData data = MapItem.getSavedData(map, helper.getLevel());
        helper.assertTrue(data != null, "Seismogram map data should exist");

        int centerPixelIndex = (64 * 128) + 64;
        byte expectedBackground = MapColor.SAND.getPackedId(MapColor.Brightness.LOWEST);
        helper.assertTrue(data.colors[centerPixelIndex] == expectedBackground, "Center pixel should remain background color");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void seismogramMarkersUseBrightThreeByThreeCores(GameTestHelper helper) {
        BlockPos station = helper.absolutePos(new BlockPos(1, 2, 1));
        int centerX = Math.floorDiv(station.getX(), 16) * 16 + 8;
        int centerZ = Math.floorDiv(station.getZ(), 16) * 16 + 8;
        for (SeismicAnomalyType type : List.of(SeismicAnomalyType.DIAMOND, SeismicAnomalyType.EMERALD,
            SeismicAnomalyType.ZINC, SeismicAnomalyType.COAL, SeismicAnomalyType.SPAWNER)) {
            ItemStack map = SeismogramMapService.createMap(helper.getLevel(), station,
                List.of(new SeismicStationBlockEntity.MapEntry(type,
                    centerX - station.getX(), centerZ - station.getZ(), station.getY() - 8)), List.of());
            MapItemSavedData data = MapItem.getSavedData(map, helper.getLevel());
            helper.assertTrue(data != null, "New survey needs map pixels");
            MapColor color = net.goldskinmc.creategeoresonance.seismic.SeismicSignalProfiles.mapColor(type);
            for (int dz = -3; dz <= 3; dz++) {
                for (int dx = -3; dx <= 3; dx++) {
                    boolean inMarker = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                    boolean inCore = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                    byte expected = inMarker
                        ? color.getPackedId(inCore ? MapColor.Brightness.HIGH : MapColor.Brightness.NORMAL)
                        : MapColor.SAND.getPackedId(MapColor.Brightness.LOWEST);
                    helper.assertTrue(data.colors[(64 + dz) * 128 + 64 + dx] == expected,
                        "Marker brightness/footprint mismatch for " + type + " at " + dx + "," + dz);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void seismogramSnapshotKeepsApproxYAndExactClusters(GameTestHelper helper) {
        BlockPos stationWorldPos = helper.absolutePos(new BlockPos(1, 2, 1));
        List<SeismicStationBlockEntity.MapEntry> entries = List.of(
            new SeismicStationBlockEntity.MapEntry(SeismicAnomalyType.DIAMOND, 3, -2, stationWorldPos.getY() - 18)
        );
        List<SeismogramMapService.ExactCluster> exactClusters = List.of(
            new SeismogramMapService.ExactCluster(
                SeismicAnomalyType.REDSTONE,
                List.of(stationWorldPos.offset(5, -12, 4), stationWorldPos.offset(5, -12, 5))
            )
        );

        ItemStack map = SeismogramMapService.createMap(helper.getLevel(), stationWorldPos, entries, exactClusters);
        CustomData originalData = map.get(DataComponents.CUSTOM_DATA);
        CompoundTag originalTag = originalData.copyTag();
        SeismogramMapService.MapSnapshot summary = SeismogramMapService.readSnapshot(map);
        helper.assertTrue(summary != null && summary.exactClusters().isEmpty(),
            "Sidebar summaries must not decode exact vein blocks");
        helper.assertTrue(SeismogramMapService.readSnapshot(map) == summary,
            "Repeated reads must reuse the immutable summary");
        helper.assertTrue(SeismogramMapService.readSnapshot(map.copy()) == summary,
            "Stack copies sharing the same component must reuse its summary");
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshotWithExactData(map);
        helper.assertTrue(snapshot != null, "Seismogram snapshot should be readable");
        helper.assertTrue(snapshot.signals().size() == 1, "Expected one signal in snapshot");
        helper.assertTrue(snapshot.signals().get(0).approxY() == stationWorldPos.getY() - 18, "Approximate Y should roundtrip");
        helper.assertTrue(snapshot.exactClusters().size() == 1, "Expected one exact cluster");
        helper.assertTrue(snapshot.exactClusters().get(0).blocks().size() == 2, "Exact cluster should preserve block set");
        helper.assertTrue(originalTag.equals(originalData.copyTag()), "Survey reads must not mutate item data");
        helper.assertTrue(SeismogramMapService.readSnapshot(map) == summary,
            "Reading exact data must not replace the lightweight cached summary");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void seismogramSummaryCacheTracksComponentReplacement(GameTestHelper helper) {
        BlockPos stationWorldPos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack map = SeismogramMapService.createMap(helper.getLevel(), stationWorldPos,
            List.of(new SeismicStationBlockEntity.MapEntry(SeismicAnomalyType.COPPER, 0, 0, stationWorldPos.getY() - 8)),
            List.of());
        SeismogramMapService.MapSnapshot original = SeismogramMapService.readSnapshot(map);
        helper.assertTrue(original != null, "Initial summary must be readable");
        ItemStack copy = map.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, map, tag -> {
            CompoundTag survey = tag.getCompound("GeoSeismogram");
            survey.putInt("CenterX", original.centerX() + 16);
            survey.getList("Entries", 10).getCompound(0).putInt("WorldY", stationWorldPos.getY() - 20);
        });

        SeismogramMapService.MapSnapshot updated = SeismogramMapService.readSnapshot(map);
        helper.assertTrue(updated != null && updated != original, "Replacing data must invalidate its summary");
        helper.assertTrue(updated.centerX() == original.centerX() + 16, "Marker center must reflect new data");
        helper.assertTrue(updated.signals().get(0).approxY() == stationWorldPos.getY() - 20,
            "Sidebar height must reflect new data even when the map ID is unchanged");
        helper.assertTrue(SeismogramMapService.readSnapshot(copy) == original,
            "An unchanged stack copy must retain its own survey");

        map.set(DataComponents.CUSTOM_DATA, CustomData.of(map.get(DataComponents.CUSTOM_DATA).copyTag()));
        SeismogramMapService.MapSnapshot equivalent = SeismogramMapService.readSnapshot(map);
        helper.assertTrue(equivalent != updated && updated.equals(equivalent),
            "Cache keys must use component identity rather than deep NBT equality");
        map.remove(DataComponents.CUSTOM_DATA);
        helper.assertTrue(!SeismogramMapService.isSeismogramStack(map) && SeismogramMapService.readSnapshot(map) == null,
            "Removing survey data must not leave a cached summary behind");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void seismogramIdentificationRejectsOtherItemsAndMalformedData(GameTestHelper helper) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        helper.assertTrue(!SeismogramMapService.isSeismogramStack(map), "Ordinary maps are not seismograms");
        CompoundTag tag = new CompoundTag();
        tag.putString("GeoSeismogram", "invalid");
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        helper.assertTrue(!SeismogramMapService.isSeismogramStack(map), "A non-compound survey must be rejected");
        tag.put("GeoSeismogram", new CompoundTag());
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        helper.assertTrue(SeismogramMapService.isSeismogramStack(map), "Compound survey identification must remain compatible");
        helper.assertTrue(SeismogramMapService.readSnapshot(map) == null, "Incomplete survey metadata must not create a snapshot");
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        helper.assertTrue(!SeismogramMapService.isSeismogramStack(paper) && SeismogramMapService.readSnapshot(paper) == null,
            "Survey tags on other items must not be treated as seismograms");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void stationAutomaticallySelectsAmbientOrTargetOnlySurvey(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        SeismicStationBlockEntity station = (SeismicStationBlockEntity) helper.getBlockEntity(fixture.controller());
        ItemStackHandler inventory = station.getInventory();
        Set<SeismicAnomalyType> ambient = Set.of(SeismicAnomalyType.CAVE, SeismicAnomalyType.WATER, SeismicAnomalyType.LAVA);
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(ambient),
            "A module-free station must detect only ambient targets");

        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START,
            new ItemStack(GeoResonanceItems.COPPER_RESONANCE_MODULE.get()));
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(Set.of(SeismicAnomalyType.COPPER)),
            "A Copper module must exclude ambient signals, even when no Copper is found");
        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 1,
            new ItemStack(GeoResonanceItems.DIAMOND_RESONANCE_MODULE.get()));
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory)
            .equals(Set.of(SeismicAnomalyType.COPPER, SeismicAnomalyType.DIAMOND)),
            "Multiple modules must include only their selected target types");

        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START, ItemStack.EMPTY);
        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 1, ItemStack.EMPTY);
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(ambient),
            "Removing all target modules must restore ambient scanning");

        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START,
            new ItemStack(GeoResonanceItems.LAVA_MODULE.get()));
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(Set.of(SeismicAnomalyType.LAVA)),
            "A Lava module must select lava without caves or water");
        helper.assertTrue(SeismicStationControllerLogic.getInstalledModuleCount(inventory) == 1,
            "A Lava module must occupy one active slot and contribute one module's stress");
        inventory.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 1,
            new ItemStack(GeoResonanceItems.DIAMOND_RESONANCE_MODULE.get()));
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory)
            .equals(Set.of(SeismicAnomalyType.LAVA, SeismicAnomalyType.DIAMOND)),
            "Lava must remain selectable alongside ore targets without other ambient types");
        helper.assertTrue(SeismicStationControllerLogic.buildModulePriorityMap(inventory).containsKey(SeismicAnomalyType.LAVA),
            "Lava must participate in balanced target selection");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void retiredModulesSurviveMigrationWithoutSlotsOrStress(GameTestHelper helper) {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        SeismicStationBlockEntity station = (SeismicStationBlockEntity) helper.getBlockEntity(fixture.controller());
        ItemStackHandler inventory = station.getInventory();
        ItemStackHandler legacy = new ItemStackHandler(SeismicStationBlockEntity.SLOT_MODULE_START + 16);
        legacy.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START,
            new ItemStack(GeoResonanceItems.NOISE_CANCELLATION_MODULE.get()));
        legacy.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 1,
            new ItemStack(GeoResonanceItems.COPPER_RESONANCE_MODULE.get()));
        legacy.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 14,
            new ItemStack(GeoResonanceItems.BELOW_ZERO_MODULE.get()));
        legacy.setStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START + 15,
            new ItemStack(GeoResonanceItems.NOISE_CANCELLATION_MODULE.get()));
        inventory.deserializeNBT(helper.getLevel().registryAccess(), legacy.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(inventory.getStackInSlot(SeismicStationBlockEntity.SLOT_MODULE_START).isEmpty(),
            "Retired modules must free their active slots");
        helper.assertTrue(SeismicStationControllerLogic.getInstalledModuleCount(inventory) == 1,
            "Only the active Copper module may contribute to module stress");
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory).equals(Set.of(SeismicAnomalyType.COPPER)),
            "Recovered modules must not change active survey targets");
        helper.assertTrue(!inventory.isItemValid(SeismicStationBlockEntity.SLOT_MODULE_START,
            new ItemStack(GeoResonanceItems.NOISE_CANCELLATION_MODULE.get())), "Retired modules cannot be reinstalled");
        helper.assertTrue(SeismicModuleType.NOISE_CANCELLATION.isRetired()
            && !SeismicModuleType.NOISE_CANCELLATION.isAvailable(), "Noise Cancellation must be unavailable");

        // Roundtrip the recovery bank, as world reloads and train assembly do.
        inventory.deserializeNBT(helper.getLevel().registryAccess(), inventory.serializeNBT(helper.getLevel().registryAccess()));
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setShiftKeyDown(true);
        for (var expected : List.of(GeoResonanceItems.COPPER_RESONANCE_MODULE.get(),
            GeoResonanceItems.NOISE_CANCELLATION_MODULE.get(), GeoResonanceItems.BELOW_ZERO_MODULE.get(),
            GeoResonanceItems.NOISE_CANCELLATION_MODULE.get())) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(station.tryExtractModule(player, InteractionHand.MAIN_HAND), "Expected a recoverable module");
            helper.assertTrue(player.getMainHandItem().is(expected), "Active modules must be extracted before recovered modules");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(station.tryExtractModule(player, InteractionHand.MAIN_HAND), "Recovery must not duplicate retired items");
        player.setShiftKeyDown(false);
        helper.assertTrue(SeismicStationControllerLogic.buildDetectableTypes(inventory)
            .equals(Set.of(SeismicAnomalyType.CAVE, SeismicAnomalyType.WATER, SeismicAnomalyType.LAVA)),
            "An empty recovery bank and no active modules must restore ambient scanning");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void compactSurveyBlocksSupportLegacyCoordinates(GameTestHelper helper) {
        List<BlockPos> positions = List.of(new BlockPos(-30000000, -64, 30000000),
            new BlockPos(0, 319, -1), new BlockPos(-1, -50, -2));
        CompoundTag packed = new CompoundTag();
        SeismicSurveyNbt.writeBlocks(packed, positions);
        helper.assertTrue(packed.contains("Blocks", Tag.TAG_LONG_ARRAY), "New block sets must use packed coordinates");
        helper.assertTrue(SeismicSurveyNbt.readBlocks(packed, "X", "Y", "Z").equals(positions),
            "Packed positions must preserve negative and world-edge coordinates");
        for (String prefix : List.of("", "Block")) {
            ListTag blocks = new ListTag();
            for (BlockPos position : positions) {
                CompoundTag block = new CompoundTag();
                block.putInt(prefix + "X", position.getX());
                block.putInt(prefix + "Y", position.getY());
                block.putInt(prefix + "Z", position.getZ());
                blocks.add(block);
            }
            CompoundTag legacy = new CompoundTag();
            legacy.put("Blocks", blocks);
            helper.assertTrue(SeismicSurveyNbt.readBlocks(legacy, prefix + "X", prefix + "Y", prefix + "Z").equals(positions),
                "Both legacy coordinate schemas must remain readable");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void oversizedLegacySurveysProduceSmallClientUpdates(GameTestHelper helper) throws IOException {
        StationFixture fixture = placeStation(helper, new BlockPos(2, 1, 2));
        SeismicStationBlockEntity station = (SeismicStationBlockEntity) helper.getBlockEntity(fixture.controller());
        CompoundTag stationTag = station.saveWithoutMetadata(helper.getLevel().registryAccess());
        CompoundTag legacyCluster = new CompoundTag();
        legacyCluster.putString("Type", "DIAMOND");
        ListTag blocks = new ListTag();
        for (int i = 0; i < 16384; i++) {
            CompoundTag block = new CompoundTag();
            block.putInt("X", i % 32);
            block.putInt("Y", -50 + i / 1024);
            block.putInt("Z", (i / 32) % 32);
            blocks.add(block);
        }
        legacyCluster.put("Blocks", blocks);
        ListTag clusters = new ListTag();
        clusters.add(legacyCluster);
        stationTag.put("ExactClusters", clusters);
        station.loadWithComponents(stationTag, helper.getLevel().registryAccess());
        CompoundTag stationClient = station.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(!stationClient.contains("ExactClusters"), "Station client updates must omit exact blocks");
        assertFitsClientNbtBudget(stationClient);
        CompoundTag stationSave = station.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(stationSave.getList("ExactClusters", Tag.TAG_COMPOUND).getCompound(0)
            .getLongArray("Blocks").length == blocks.size(), "Station saves must retain every legacy block compactly");

        CompoundTag projectorCluster = new CompoundTag();
        projectorCluster.putString("ClusterType", "DIAMOND");
        ListTag projectorBlocks = new ListTag();
        for (Tag raw : blocks) {
            CompoundTag block = (CompoundTag) raw;
            CompoundTag legacyBlock = new CompoundTag();
            legacyBlock.putInt("BlockX", block.getInt("X"));
            legacyBlock.putInt("BlockY", block.getInt("Y"));
            legacyBlock.putInt("BlockZ", block.getInt("Z"));
            projectorBlocks.add(legacyBlock);
        }
        projectorCluster.put("Blocks", projectorBlocks);
        ListTag projectorClusters = new ListTag();
        projectorClusters.add(projectorCluster);
        ListTag nodes = new ListTag();
        for (int i = 0; i < 2; i++) {
            CompoundTag node = new CompoundTag();
            node.putInt("StationX", i * 16);
            node.putString("StationDimension", "minecraft:overworld");
            node.put("ExactClusters", projectorClusters.copy());
            nodes.add(node);
        }
        CompoundTag projectorTag = new CompoundTag();
        projectorTag.put("Nodes", nodes);
        SeismicProjectorData projector = new SeismicProjectorData();
        projector.readFromTag(projectorTag);
        CompoundTag projectorClient = new CompoundTag();
        projector.writeToTag(projectorClient, true);
        assertFitsClientNbtBudget(projectorClient);
        SeismicProjectorData clientCopy = new SeismicProjectorData();
        clientCopy.readFromTag(projectorClient);
        helper.assertTrue(clientCopy.getConfirmedVeins().get(0).blocks().size() == blocks.size(),
            "Compact projector updates must preserve the complete vein");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void legacySeismogramItemsRemainExtractableButAreNotSynced(GameTestHelper helper) {
        ItemStack map = SeismogramMapService.createMap(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
            List.of(), List.of(new SeismogramMapService.ExactCluster(SeismicAnomalyType.DIAMOND,
                List.of(new BlockPos(7, -50, 9)))));
        CustomData.update(DataComponents.CUSTOM_DATA, map, tag -> {
            CompoundTag cluster = tag.getCompound("GeoSeismogram").getList("ExactClusters", Tag.TAG_COMPOUND).getCompound(0);
            CompoundTag block = new CompoundTag();
            block.putInt("X", 7);
            block.putInt("Y", -50);
            block.putInt("Z", 9);
            ListTag legacy = new ListTag();
            legacy.add(block);
            cluster.put("Blocks", legacy);
        });
        CompoundTag node = new CompoundTag();
        node.put("SeismogramItem", map.save(helper.getLevel().registryAccess()));
        ListTag nodes = new ListTag();
        nodes.add(node);
        CompoundTag legacyProjector = new CompoundTag();
        legacyProjector.put("Nodes", nodes);
        SeismicProjectorData projector = new SeismicProjectorData();
        projector.readFromTag(legacyProjector);
        CompoundTag clientTag = new CompoundTag();
        projector.writeToTag(clientTag, true);
        helper.assertTrue(!clientTag.getList("Nodes", Tag.TAG_COMPOUND).getCompound(0).contains("SeismogramItem"),
            "Clients must not receive the original item copy");
        ItemStack returned = projector.takeLastSeismogram(helper.getLevel());
        helper.assertTrue(returned.get(DataComponents.MAP_ID).equals(map.get(DataComponents.MAP_ID)),
            "Extraction must retain the original map ID");
        helper.assertTrue(SeismogramMapService.readSnapshotWithExactData(returned)
            .equals(SeismogramMapService.readSnapshotWithExactData(map)), "Legacy item migration must preserve its survey");
        helper.assertTrue(returned.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("GeoSeismogram")
            .getList("ExactClusters", Tag.TAG_COMPOUND).getCompound(0).contains("Blocks", Tag.TAG_LONG_ARRAY),
            "Extracted legacy items must use compact block storage");
        helper.succeed();
    }

    private static void assertFitsClientNbtBudget(CompoundTag tag) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeAnyTag(tag, new DataOutputStream(bytes));
        NbtIo.readAnyTag(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), NbtAccounter.create(2097152));
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void projectorShiftPlacementReversesShaft(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        float oldYaw = player.getYRot();
        boolean oldShift = player.isShiftKeyDown();
        ItemStack oldStack = player.getMainHandItem();
        var projector = GeoResonanceBlocks.SEISMIC_PROJECTOR.get();
        BlockPos basePos = helper.absolutePos(new BlockPos(2, 0, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(basePos), Direction.UP, basePos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(projector));
        try {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                player.setYRot(direction.toYRot());
                for (boolean shifted : new boolean[] { false, true }) {
                    player.setShiftKeyDown(shifted);
                    BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                    BlockState state = projector.getStateForPlacement(context);
                    Direction expectedShaft = shifted ? direction : direction.getOpposite();
                    helper.assertTrue(state.getValue(SeismicProjectorBlock.HORIZONTAL_FACING) == expectedShaft.getOpposite(),
                        "Shift placement must reverse projector facing for " + direction);
                    for (Direction face : Direction.values()) {
                        helper.assertTrue(projector.hasShaftTowards(helper.getLevel(), context.getClickedPos(), state, face)
                            == (face == expectedShaft), "Projector shaft must use the expected side");
                    }
                }
            }
        } finally {
            player.setYRot(oldYaw);
            player.setShiftKeyDown(oldShift);
            player.setItemInHand(InteractionHand.MAIN_HAND, oldStack);
        }
        helper.succeed();
    }

    private static StationFixture placeStation(GameTestHelper helper, BlockPos basePos) {
        helper.setBlock(basePos, Blocks.STONE);
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.moveTo(helper.absoluteVec(new Vec3(basePos.getX() + 0.5D, basePos.getY() + 1.2D, basePos.getZ() + 0.5D)));
        ItemStack stationStack = new ItemStack(GeoResonanceBlocks.SEISMIC_STATION.asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stationStack);

        BlockPos baseAbsolute = helper.absolutePos(basePos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(baseAbsolute), Direction.UP, baseAbsolute, false);
        InteractionResult result = stationStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(result.consumesAction(), "Station placement should consume interaction");

        BlockPos controller = null;
        BlockPos lowerLeft = null;
        BlockPos upperRight = null;
        BlockPos upperLeft = null;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos relativePos = basePos.offset(dx, dy, dz);
                    BlockState state = helper.getLevel().getBlockState(helper.absolutePos(relativePos));
                    if (state.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()) {
                        controller = relativePos;
                        continue;
                    }
                    if (state.getBlock() != GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get()) {
                        continue;
                    }
                    SeismicStationBoundingBlock.BoundingPart part = state.getValue(SeismicStationBoundingBlock.PART);
                    if (part == SeismicStationBoundingBlock.BoundingPart.LOWER_LEFT) {
                        lowerLeft = relativePos;
                    } else if (part == SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT) {
                        upperRight = relativePos;
                    } else if (part == SeismicStationBoundingBlock.BoundingPart.UPPER_LEFT) {
                        upperLeft = relativePos;
                    }
                }
            }
        }

        helper.assertTrue(controller != null, "Station controller block was not placed");
        helper.assertTrue(lowerLeft != null, "Lower-left bounding part is missing");
        helper.assertTrue(upperRight != null, "Upper-right bounding part is missing");
        helper.assertTrue(upperLeft != null, "Upper-left bounding part is missing");
        return new StationFixture(controller, lowerLeft, upperRight, upperLeft);
    }

    private static int countStationDrops(GameTestHelper helper, BlockPos aroundRelativePos) {
        BlockPos center = helper.absolutePos(aroundRelativePos);
        AABB bounds = new AABB(center).inflate(2.0D);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (entity.getItem().is(GeoResonanceBlocks.SEISMIC_STATION.asItem())) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    private record StationFixture(BlockPos controller, BlockPos lowerLeft, BlockPos upperRight, BlockPos upperLeft) {
    }
}
