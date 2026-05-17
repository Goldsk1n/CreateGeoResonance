package net.goldskinmc.creategeoresonance.client;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class GeoResonancePonderScenes {
    private GeoResonancePonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<net.minecraft.resources.ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> itemHelper = helper.withKeyFunction(ItemProviderEntry::getId);

        itemHelper.forComponents(GeoResonanceItems.SEISMIC_HAMMER, GeoResonanceBlocks.PLACED_SEISMIC_HAMMER)
            .addStoryBoard("seismic_hammer/basics", GeoResonancePonderScenes::seismicHammerBasics, GeoResonancePonderTags.SEISMIC);

        itemHelper.forComponents(GeoResonanceBlocks.SEISMIC_STATION)
            .addStoryBoard("seismic_station/operation", GeoResonancePonderScenes::seismicStationOperation, GeoResonancePonderTags.SEISMIC);

        itemHelper.forComponents(GeoResonanceBlocks.SEISMIC_PROJECTOR)
            .addStoryBoard("seismic_projector/triangulation", GeoResonancePonderScenes::seismicProjectorTriangulation, GeoResonancePonderTags.SEISMIC);
    }

    private static void seismicHammerBasics(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_hammer/basics", "Survey terrain with the Seismic Hammer");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        scene.scaleSceneView(0.9F);

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 1, 6), Direction.UP);
        BlockPos impact = util.grid().at(3, 1, 3);
        BlockPos actorPos = util.grid().at(3, 1, 5);
        BlockPos caveEcho = util.grid().at(2, 1, 2);
        BlockPos waterEcho = util.grid().at(5, 1, 3);
        BlockPos lavaEcho = util.grid().at(1, 1, 4);

        ElementLink<EntityElement> actor = scene.world().createEntity(level -> {
            ArmorStand stand = new ArmorStand(level, actorPos.getX() + 0.5D, actorPos.getY(), actorPos.getZ() + 0.5D);
            stand.setNoBasePlate(true);
            stand.setShowArms(true);
            stand.setYRot(180.0F);
            stand.setRightArmPose(new Rotations(-8.0F, 0.0F, 7.0F));
            stand.setLeftArmPose(new Rotations(-14.0F, 0.0F, -5.0F));
            stand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
            return stand;
        });
        scene.idle(15);

        scene.overlay().showText(60)
            .text("Right-click to strike the ground.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().blockSurface(impact, Direction.UP), Pointing.DOWN, 35)
            .rightClick()
            .withItem(new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
        scene.idle(12);

        setActorArmPose(scene, actor, new Rotations(32.0F, 0.0F, 6.0F));
        scene.idle(3);
        setActorArmPose(scene, actor, new Rotations(-8.0F, 0.0F, 7.0F));

        emitEcho(scene, util, impact, new Vector3f(0.78F, 0.78F, 0.78F), 34, 0.18F);
        scene.idle(20);

        scene.overlay().showText(90)
            .colored(PonderPalette.BLUE)
            .text("Returning echoes reveal cave, water, and lava.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        emitEcho(scene, util, caveEcho, new Vector3f(0.70F, 0.70F, 0.70F), 26, 0.12F);
        scene.idle(14);

        emitEcho(scene, util, waterEcho, new Vector3f(0.24F, 0.64F, 0.96F), 30, 0.12F);
        scene.idle(16);

        emitEcho(scene, util, lavaEcho, new Vector3f(1.00F, 0.54F, 0.20F), 30, 0.12F);
        scene.idle(32);
    }

    private static void seismicStationOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_station/operation", "Power and run a Seismic Station");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();

        BlockPos motor = util.grid().at(1, 2, 2);
        BlockPos input = util.grid().at(3, 2, 2);
        BlockPos station = util.grid().at(3, 1, 2);

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 2, 5), Direction.DOWN);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().position(motor), 64.0F);
        scene.effects().rotationSpeedIndicator(input);
        scene.overlay().showText(80)
            .text("Feed rotation into the station's top input shaft.")
            .pointAt(util.vector().centerOf(input))
            .placeNearTarget();
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(station, Direction.NORTH), Pointing.RIGHT, 35)
            .rightClick()
            .withItem(new ItemStack(Items.PAPER));
        scene.overlay().showText(70)
            .text("Insert paper and ink sac on the table side.")
            .pointAt(util.vector().centerOf(station))
            .placeNearTarget();
        scene.idle(40);

        scene.overlay().showText(80)
            .text("Right-click the station to start scanning and generate a seismogram.")
            .pointAt(util.vector().topOf(station))
            .placeNearTarget();
        scene.idle(50);
    }

    private static void seismicProjectorTriangulation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_projector/triangulation", "Project anomalies from two seismograms");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);

        BlockPos motor = util.grid().at(1, 3, 2);
        BlockPos shaft = util.grid().at(2, 3, 2);
        BlockPos projector = util.grid().at(3, 3, 2);
        BlockPos diamondProjection = util.grid().at(2, 0, 0);
        BlockPos redstoneA = util.grid().at(0, 0, 2);
        BlockPos redstoneB = util.grid().at(1, 0, 2);
        BlockPos redstoneC = util.grid().at(0, 0, 3);
        BlockPos redstoneD = util.grid().at(1, 0, 3);
        List<BlockPos> redstoneCluster = List.of(redstoneA, redstoneB, redstoneC, redstoneD);
        Vec3 seismogramPoint = projectorRearUpperRightPoint(util, projector, Direction.EAST);
        Vec3 redstoneCenter = clusterCenter(util, redstoneCluster);
        applyProjectorPonderTerrain(scene, util, diamondProjection, redstoneCluster);

        // Re-orient the setup so motor -> shaft -> projector input line is valid on X axis.
        scene.world().modifyBlock(projector, state -> state
            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
            .setValue(SeismicProjectorBlock.ACTIVE, false), false);
        scene.world().modifyBlock(motor, state -> state.hasProperty(BlockStateProperties.FACING)
            ? state.setValue(BlockStateProperties.FACING, Direction.EAST)
            : state, false);

        scene.world().showSection(util.select().layer(2), Direction.UP);
        scene.world().showSection(util.select().layer(1), Direction.UP);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().showSection(util.select().position(diamondProjection), Direction.UP);
        for (BlockPos redstonePos : redstoneCluster) {
            scene.world().showSection(util.select().position(redstonePos), Direction.UP);
        }
        scene.world().showSection(util.select().position(projector), Direction.UP);
        scene.idle(20);

        scene.overlay().showText(70)
            .text("Place the Seismic Projector first.")
            .pointAt(util.vector().topOf(projector))
            .placeNearTarget();
        scene.idle(75);

        scene.overlay().showControls(util.vector().blockSurface(projector, Direction.UP), Pointing.DOWN, 40)
            .rightClick()
            .withItem(createPonderSeismogramStack());
        scene.overlay().showText(70)
            .text("Load the first seismogram.")
            .pointAt(seismogramPoint)
            .placeNearTarget();
        scene.idle(10);
        setProjectorNodeData(scene, util, projector,
            diamondProjection, redstoneCluster,
            false, true, false);
        scene.effects().indicateSuccess(projector);
        scene.idle(65);

        scene.overlay().showControls(util.vector().blockSurface(projector, Direction.UP), Pointing.DOWN, 45)
            .rightClick()
            .withItem(createPonderSeismogramStack());
        scene.overlay().showText(100)
            .text("Load the second seismogram. Stations must be in the same dimension and at least 8 blocks apart.")
            .pointAt(seismogramPoint)
            .placeNearTarget();
        scene.idle(12);
        setProjectorNodeData(scene, util, projector,
            diamondProjection, redstoneCluster,
            true, true, false);
        scene.effects().indicateSuccess(projector);
        scene.idle(95);

        scene.world().showSection(util.select().position(shaft), Direction.UP);
        scene.idle(14);
        scene.world().showSection(util.select().position(motor), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(80)
            .text("Power the rear shaft input to start projection.")
            .pointAt(util.vector().centerOf(shaft))
            .placeNearTarget();
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().position(motor), 96.0F);
        scene.world().setKineticSpeed(util.select().position(shaft), 96.0F);
        scene.world().setKineticSpeed(util.select().position(projector), 96.0F);
        scene.effects().rotationSpeedIndicator(shaft);
        setProjectorNodeData(scene, util, projector,
            diamondProjection, redstoneCluster,
            true, true, true);
        scene.world().modifyBlock(projector, state -> state.setValue(SeismicProjectorBlock.ACTIVE, true), false);
        scene.idle(20);
        scene.idle(45);
        scene.overlay().showText(90)
            .colored(PonderPalette.GREEN)
            .text("With two valid records, holograms appear at estimated depth.")
            .pointAt(redstoneCenter)
            .placeNearTarget();
        scene.idle(100);
    }

    private static void setActorArmPose(CreateSceneBuilder scene, ElementLink<EntityElement> actor, Rotations pose) {
        scene.world().modifyEntity(actor, entity -> {
            if (entity instanceof ArmorStand stand) {
                stand.setRightArmPose(pose);
            }
        });
    }

    private static void emitEcho(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Vector3f color, int count, float speed) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        scene.effects().emitParticles(util.vector().centerOf(pos),
            scene.effects().particleEmitterWithinBlockSpace(dust, util.vector().of(0.45D, 0.06D, 0.45D)), speed, count);
        scene.effects().indicateSuccess(pos);
    }

    private static void applyProjectorPonderTerrain(CreateSceneBuilder scene, SceneBuildingUtil util,
                                                    BlockPos diamondProjection, List<BlockPos> redstoneCluster) {
        boolean removeStoneFill = Config.PROJECTOR_PONDER_DEBUG_REMOVE_STONE_FILL.get();
        BlockState topState = Blocks.ANDESITE.defaultBlockState();
        BlockState midState = removeStoneFill ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
        BlockState lowState = removeStoneFill ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();

        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                BlockPos top = util.grid().at(x, 2, z);
                BlockPos middle = util.grid().at(x, 1, z);
                BlockPos stone = util.grid().at(x, 0, z);

                if (!redstoneCluster.contains(top)) {
                    scene.world().setBlocks(util.select().position(top), topState, false);
                }
                if (!redstoneCluster.contains(middle)) {
                    scene.world().setBlocks(util.select().position(middle), midState, false);
                }
                if (!stone.equals(diamondProjection) && !redstoneCluster.contains(stone)) {
                    scene.world().setBlocks(util.select().position(stone), lowState, false);
                }
            }
        }
        for (BlockPos redstonePos : redstoneCluster) {
            scene.world().setBlocks(util.select().position(redstonePos), Blocks.REDSTONE_ORE.defaultBlockState(), false);
        }
    }

    private static ItemStack createPonderSeismogramStack() {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("GeoSeismogram", new CompoundTag());
        stack.setTag(tag);
        return stack;
    }

    private static Vec3 projectorRearUpperRightPoint(SceneBuildingUtil util, BlockPos projectorPos, Direction facing) {
        Direction rear = facing.getOpposite();
        Direction right = facing.getClockWise();
        Vec3 center = util.vector().centerOf(projectorPos);
        return center.add(
            rear.getStepX() * 0.42D + right.getStepX() * 0.42D,
            0.42D,
            rear.getStepZ() * 0.42D + right.getStepZ() * 0.42D
        );
    }

    private static Vec3 clusterCenter(SceneBuildingUtil util, List<BlockPos> cluster) {
        if (cluster.isEmpty()) {
            return util.vector().of(0.5D, 0.5D, 0.5D);
        }
        double sx = 0.0D;
        double sy = 0.0D;
        double sz = 0.0D;
        for (BlockPos pos : cluster) {
            Vec3 center = util.vector().centerOf(pos);
            sx += center.x;
            sy += center.y;
            sz += center.z;
        }
        double inv = 1.0D / cluster.size();
        return new Vec3(sx * inv, sy * inv, sz * inv);
    }

    private static void setProjectorNodeData(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos projectorPos,
                                             BlockPos diamondOre, List<BlockPos> redstoneCluster,
                                             boolean twoNodes, boolean includeSignals, boolean includeDepthTargets) {
        scene.world().modifyBlockEntityNBT(util.select().position(projectorPos), SeismicProjectorBlockEntity.class, tag -> {
            ListTag nodes = new ListTag();
            nodes.add(createProjectorNodeTag(
                0, 64, 0,
                -12, 70, -8,
                "minecraft:overworld",
                includeSignals ? List.of(
                    createSignalTag("DIAMOND", 24, 12, includeDepthTargets ? 18 : 62),
                    createSignalTag("REDSTONE", 18, 14, includeDepthTargets ? 20 : 63)
                ) : List.of(),
                includeDepthTargets ? List.of(
                    createExactClusterTag("DIAMOND", List.of(diamondOre)),
                    createExactClusterTag("REDSTONE", redstoneCluster)
                ) : List.of()
            ));
            if (twoNodes) {
                nodes.add(createProjectorNodeTag(
                    16, 64, 16,
                    18, 72, 14,
                    "minecraft:overworld",
                    includeSignals ? List.of(
                        createSignalTag("DIAMOND", 23, 11, includeDepthTargets ? 17 : 61),
                        createSignalTag("REDSTONE", 17, 13, includeDepthTargets ? 20 : 63)
                    ) : List.of(),
                    includeDepthTargets ? List.of(
                        createExactClusterTag("DIAMOND", List.of(diamondOre)),
                        createExactClusterTag("REDSTONE", redstoneCluster)
                    ) : List.of()
                ));
            }
            tag.put("Nodes", nodes);
        }, false);
    }

    private static CompoundTag createProjectorNodeTag(int centerX, int centerY, int centerZ,
                                                      int stationX, int stationY, int stationZ,
                                                      String stationDimension,
                                                      List<CompoundTag> signals,
                                                      List<CompoundTag> exactClusters) {
        CompoundTag node = new CompoundTag();
        node.putInt("CenterX", centerX);
        node.putInt("CenterY", centerY);
        node.putInt("CenterZ", centerZ);
        node.putInt("StationX", stationX);
        node.putInt("StationY", stationY);
        node.putInt("StationZ", stationZ);
        node.putString("StationDimension", stationDimension);

        ListTag signalsTag = new ListTag();
        for (CompoundTag signal : signals) {
            signalsTag.add(signal);
        }
        node.put("Signals", signalsTag);
        ListTag exactClustersTag = new ListTag();
        for (CompoundTag cluster : exactClusters) {
            exactClustersTag.add(cluster);
        }
        node.put("ExactClusters", exactClustersTag);
        return node;
    }

    private static CompoundTag createSignalTag(String type, int worldX, int worldZ, int approxY) {
        CompoundTag signal = new CompoundTag();
        signal.putString("Type", type);
        signal.putInt("X", worldX);
        signal.putInt("Z", worldZ);
        signal.putInt("Y", approxY);
        return signal;
    }

    private static CompoundTag createExactClusterTag(String type, List<BlockPos> blocks) {
        CompoundTag cluster = new CompoundTag();
        cluster.putString("ClusterType", type);
        ListTag blockList = new ListTag();
        for (BlockPos blockPos : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("BlockX", blockPos.getX());
            blockTag.putInt("BlockY", blockPos.getY());
            blockTag.putInt("BlockZ", blockPos.getZ());
            blockList.add(blockTag);
        }
        cluster.put("Blocks", blockList);
        return cluster;
    }
}
