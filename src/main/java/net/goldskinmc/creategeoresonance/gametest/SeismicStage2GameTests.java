package net.goldskinmc.creategeoresonance.gametest;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationControllerLogic;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.List;
import java.util.Set;

@GameTestHolder(CreateGeoResonanceMod.MODID)
@Mod.EventBusSubscriber(modid = CreateGeoResonanceMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshotWithExactData(map);
        helper.assertTrue(snapshot != null, "Seismogram snapshot should be readable");
        helper.assertTrue(snapshot.signals().size() == 1, "Expected one signal in snapshot");
        helper.assertTrue(snapshot.signals().get(0).approxY() == stationWorldPos.getY() - 18, "Approximate Y should roundtrip");
        helper.assertTrue(snapshot.exactClusters().size() == 1, "Expected one exact cluster");
        helper.assertTrue(snapshot.exactClusters().get(0).blocks().size() == 2, "Exact cluster should preserve block set");
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
        inventory.deserializeNBT(legacy.serializeNBT());
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
        inventory.deserializeNBT(inventory.serializeNBT());
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
