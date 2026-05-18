package net.goldskinmc.creategeoresonance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.theme.Color;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.UUID;

public final class GeoResonancePonderScenes {
    private static final BindableTexture PONDER_SEISMIC_WAVE_TEXTURE =
        () -> ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "textures/block/seismic_wave_clear.png");

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
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85F);

        BlockPos stoneMarkerA = util.grid().at(2, 0, 0);
        BlockPos stoneMarkerB1 = util.grid().at(0, 0, 2);
        BlockPos stoneMarkerB2 = util.grid().at(1, 0, 2);
        BlockPos stoneMarkerB3 = util.grid().at(0, 0, 3);
        BlockPos stoneMarkerB4 = util.grid().at(1, 0, 3);
        List<BlockPos> stoneMarkerCluster = List.of(stoneMarkerB1, stoneMarkerB2, stoneMarkerB3, stoneMarkerB4);
        BlockPos caveEcho = util.grid().at(1, 2, 1);
        BlockPos waterEcho = util.grid().at(1, 2, 4);
        BlockPos lavaEcho = util.grid().at(4, 2, 1);

        applyHammerPonderTerrain(scene, util, stoneMarkerA, stoneMarkerCluster);
        scene.world().showSection(util.select().layer(2), Direction.UP);
        scene.world().showSection(util.select().layer(1), Direction.UP);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().showSection(util.select().position(stoneMarkerA), Direction.UP);
        for (BlockPos marker : stoneMarkerCluster) {
            scene.world().showSection(util.select().position(marker), Direction.UP);
        }
        BlockPos impact = util.grid().at(2, 2, 2);
        BlockPos actorPos = util.grid().at(3, 3, 4);
        ElementLink<EntityElement> actor = scene.world().createEntity(level -> {
            ArmorStand stand = new ArmorStand(level, actorPos.getX() + 0.5D, actorPos.getY(), actorPos.getZ() + 0.5D);
            stand.setNoBasePlate(true);
            stand.setShowArms(true);
            stand.setYRot(180.0F);
            stand.setRightArmPose(new Rotations(-18.0F, 0.0F, 10.0F));
            stand.setLeftArmPose(new Rotations(-8.0F, 0.0F, -4.0F));
            stand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
            stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            return stand;
        });
        scene.idle(32);
        scene.world().modifyEntity(actor, entity -> {
            if (entity instanceof ArmorStand stand) {
                stand.setLeftArmPose(new Rotations(-10.0F, 0.0F, -5.0F));
            }
        });
        scene.overlay().showText(75)
            .text("Seismic Hammer reveals underground anomalies using surface impacts.")
            .pointAt(util.vector().centerOf(impact))
            .placeNearTarget();
        scene.idle(90);

        scene.addKeyframe();
        scene.overlay().showText(78)
            .text("Strike the surface to send a seismic pulse.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().blockSurface(impact, Direction.UP), Pointing.DOWN, 70)
            .rightClick()
            .withItem(new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
        scene.idle(32);
        scene.world().modifyEntity(actor, entity -> {
            if (entity instanceof ArmorStand stand) {
                stand.setRightArmPose(new Rotations(30.0F, 0.0F, 6.0F));
            }
        });
        scene.idle(6);
        scene.world().modifyEntity(actor, entity -> {
            if (entity instanceof ArmorStand stand) {
                stand.setRightArmPose(new Rotations(-18.0F, 0.0F, 10.0F));
            }
        });
        emitWaveEcho(scene, util, impact, 0xC2C2C2, 1.0F, 0.2F, 16, 3.25F,
            0.52F, 0.0F, 1.0F);
        scene.idle(48);

        scene.addKeyframe();
        scene.overlay().showText(40)
            .text("Blue return marks water.")
            .pointAt(util.vector().topOf(waterEcho))
            .placeNearTarget();
        emitWaveEcho(scene, util, waterEcho, 0x4AA8FF, 1.0F, 0.15F, 16, 2.2F,
            0.52F, 0.0F, 1.0F);
        scene.idle(48);
        scene.overlay().showText(40)
            .text("Orange return marks lava.")
            .pointAt(util.vector().topOf(lavaEcho))
            .placeNearTarget();
        emitWaveEcho(scene, util, lavaEcho, 0xFF9A3D, 1.0F, 0.15F, 16, 2.2F,
            0.52F, 0.0F, 1.0F);
        scene.idle(48);

        scene.addKeyframe();
        scene.overlay().showText(80)
            .text("Gray return suggests a cavity. Deeper objects echoes take longer to return.")
            .pointAt(util.vector().topOf(caveEcho))
            .placeNearTarget();
        emitWaveEcho(scene, util, caveEcho, 0xB7B7B7, 1.0F, 0.15F, 16, 2.2F,
            0.52F, 0.0F, 1.0F);
        scene.idle(90);
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
        Vec3 seismogramHintPoint = projectorFrontUpperRightPoint(util, projector, Direction.EAST);
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
        scene.idle(16);
        scene.world().showSection(util.select().position(projector), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(55)
            .text("Place the Seismic Projector first.")
            .pointAt(util.vector().topOf(projector))
            .placeNearTarget();
        scene.idle(75);

        scene.addKeyframe();
        scene.overlay().showControls(seismogramHintPoint, Pointing.DOWN, 40)
            .rightClick()
            .withItem(createPonderSeismogramStack());
        scene.overlay().showText(55)
            .text("Load the first seismogram.")
            .pointAt(seismogramPoint)
            .placeNearTarget();
        scene.idle(10);
        setProjectorNodeData(scene, util, projector,
            diamondProjection, redstoneCluster,
            false, true, false);
        scene.effects().indicateSuccess(projector);
        scene.idle(65);
        scene.addKeyframe();

        scene.overlay().showControls(seismogramHintPoint, Pointing.DOWN, 45)
            .rightClick()
            .withItem(createPonderSeismogramStack());
        scene.overlay().showText(85)
            .text("Load the second seismogram. Stations must be in the same dimension and at least 8 blocks apart.")
            .pointAt(seismogramPoint)
            .placeNearTarget();
        scene.idle(12);
        setProjectorNodeData(scene, util, projector,
            diamondProjection, redstoneCluster,
            true, true, false);
        scene.effects().indicateSuccess(projector);
        scene.idle(95);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(shaft), Direction.DOWN);
        scene.idle(14);
        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(65)
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
        scene.overlay().showText(75)
            .colored(PonderPalette.GREEN)
            .text("With two valid records, holograms appear at estimated depth.")
            .pointAt(redstoneCenter)
            .placeNearTarget();
        scene.idle(100);
    }

    private static void applyHammerPonderTerrain(CreateSceneBuilder scene, SceneBuildingUtil util,
                                                 BlockPos stoneMarkerA, List<BlockPos> stoneMarkerCluster) {
        BlockState topState = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState middleState = Blocks.STONE.defaultBlockState();
        BlockState lowerState = Blocks.STONE.defaultBlockState();

        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                BlockPos top = util.grid().at(x, 2, z);
                BlockPos middle = util.grid().at(x, 1, z);
                BlockPos lower = util.grid().at(x, 0, z);
                if (!stoneMarkerCluster.contains(top)) {
                    scene.world().setBlocks(util.select().position(top), topState, false);
                }
                if (!stoneMarkerCluster.contains(middle)) {
                    scene.world().setBlocks(util.select().position(middle), middleState, false);
                }
                if (!lower.equals(stoneMarkerA) && !stoneMarkerCluster.contains(lower)) {
                    scene.world().setBlocks(util.select().position(lower), lowerState, false);
                }
            }
        }

        // Mirror projector cutaway structure, but use stone instead of ore markers.
        scene.world().setBlocks(util.select().position(stoneMarkerA), Blocks.STONE.defaultBlockState(), false);
        for (BlockPos marker : stoneMarkerCluster) {
            scene.world().setBlocks(util.select().position(marker), Blocks.STONE.defaultBlockState(), false);
        }

        // Keep the full lowest layer solid for clearer depth contrast in this scene.
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                scene.world().setBlocks(util.select().position(util.grid().at(x, 0, z)), Blocks.STONE.defaultBlockState(), false);
            }
        }

        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    BlockState state = y == 0 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    scene.world().setBlocks(util.select().position(util.grid().at(x, y, z)), state, false);
                }
            }
        }
        for (int x = 4; x <= 5; x++) {
            for (int z = 0; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    BlockState state = y == 1 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    scene.world().setBlocks(util.select().position(util.grid().at(x, y, z)), state, false);
                }
            }
        }
        for (int x = 0; x <= 1; x++) {
            for (int z = 4; z <= 5; z++) {
                for (int y = 0; y <= 1; y++) {
                    BlockState state = y == 1 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    scene.world().setBlocks(util.select().position(util.grid().at(x, y, z)), state, false);
                }
            }
        }
        for (int x = 4; x <= 5; x++) {
            for (int z = 4; z <= 5; z++) {
                for (int y = 0; y <= 1; y++) {
                    scene.world().setBlocks(util.select().position(util.grid().at(x, y, z)), Blocks.STONE.defaultBlockState(), false);
                }
            }
        }
    }

    private static void emitWaveEcho(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, int rgb, float opacity,
                                     float blur, int lifetimeTicks, float maxRadius, float yOffset, float yDeviation,
                                     float thicknessScale) {
        Vec3 center = util.vector().centerOf(pos);
        scene.addInstruction(new PonderSeismicWaveInstruction(UUID.randomUUID(), center, rgb, opacity, blur, lifetimeTicks, maxRadius,
            yOffset, yDeviation, thicknessScale));
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

    private static Vec3 projectorFrontUpperLeftPoint(SceneBuildingUtil util, BlockPos projectorPos, Direction facing) {
        Direction front = facing;
        Direction left = facing.getCounterClockWise();
        Vec3 center = util.vector().centerOf(projectorPos);
        return center.add(
            front.getStepX() * 0.42D + left.getStepX() * 0.42D,
            0.42D,
            front.getStepZ() * 0.42D + left.getStepZ() * 0.42D
        );
    }

    private static Vec3 projectorFrontUpperRightPoint(SceneBuildingUtil util, BlockPos projectorPos, Direction facing) {
        Direction front = facing;
        Direction right = facing.getClockWise();
        Vec3 center = util.vector().centerOf(projectorPos);
        return center.add(
            front.getStepX() * 0.42D + right.getStepX() * 0.42D,
            0.42D,
            front.getStepZ() * 0.42D + right.getStepZ() * 0.42D
        );
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

    private static final class PonderSeismicWaveInstruction extends PonderInstruction {
        private final Object slot;
        private final Vec3 center;
        private final int rgb;
        private final float opacity;
        private final float blur;
        private final int lifetimeTicks;
        private final float maxRadius;
        private final float yOffset;
        private final float yDeviation;
        private final float thicknessScale;
        private int age;

        private PonderSeismicWaveInstruction(Object slot, Vec3 center, int rgb, float opacity, float blur, int lifetimeTicks,
                                             float maxRadius, float yOffset, float yDeviation, float thicknessScale) {
            this.slot = slot;
            this.center = center;
            this.rgb = rgb & 0x00FFFFFF;
            this.opacity = Mth.clamp(opacity, 0.0F, 1.0F);
            this.blur = Math.max(0.0F, blur);
            this.lifetimeTicks = Math.max(1, lifetimeTicks);
            this.maxRadius = Math.max(0.1F, maxRadius);
            this.yOffset = yOffset;
            this.yDeviation = Math.max(0.0F, yDeviation);
            this.thicknessScale = Math.max(0.1F, thicknessScale);
        }

        @Override
        public boolean isComplete() {
            return age > lifetimeTicks;
        }

        @Override
        public void reset(PonderScene scene) {
            age = 0;
            scene.getOutliner().remove(slot);
        }

        @Override
        public void onScheduled(PonderScene scene) {
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            if (isComplete()) {
                scene.getOutliner().remove(slot);
                return;
            }

            float progress = Mth.clamp(age / (float) lifetimeTicks, 0.0F, 1.0F);
            float radius = 0.35F + progress * maxRadius;
            float thickness = (0.04F + blur * 0.08F) * thicknessScale;
            float alpha = Mth.clamp(opacity * (1.0F - progress), 0.0F, 1.0F);
            int argb = ((int) (alpha * 255.0F) << 24) | rgb;
            double y = center.y + yOffset + blur * 0.03F + (Mth.sin(progress * Mth.PI) * yDeviation);

            scene.getOutliner().showOutline(slot, new PonderSeismicWaveOutline(center, y, radius, thickness))
                .lineWidth(0.0F)
                .disableCull()
                .disableLineNormals()
                .lightmap(LightTexture.FULL_BRIGHT)
                .colored(new Color(argb, true));

            age++;
        }
    }

    private static final class PonderSeismicWaveOutline extends Outline {
        private final Vec3 center;
        private final double y;
        private final float radius;
        private final float thickness;

        private PonderSeismicWaveOutline(Vec3 center, double y, float radius, float thickness) {
            this.center = center;
            this.y = y;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
            params.loadColor(colorTemp);
            Vector4f color = colorTemp;
            if (color.w() <= 0.0F) {
                return;
            }

            VertexConsumer consumer = buffer.getLateBuffer(PonderRenderTypes.outlineTranslucent(PONDER_SEISMIC_WAVE_TEXTURE.getLocation(), false));

            ms.pushPose();
            ms.translate(center.x - camera.x, y - camera.y, center.z - camera.z);

            PoseStack.Pose pose = ms.last();
            Vector3f p0 = new Vector3f(-radius, 0.0F, -radius);
            Vector3f p1 = new Vector3f(-radius, 0.0F, radius);
            Vector3f p2 = new Vector3f(radius, 0.0F, radius);
            Vector3f p3 = new Vector3f(radius, 0.0F, -radius);

            Vector3f p4 = new Vector3f(-radius, thickness, -radius);
            Vector3f p5 = new Vector3f(-radius, thickness, radius);
            Vector3f p6 = new Vector3f(radius, thickness, radius);
            Vector3f p7 = new Vector3f(radius, thickness, -radius);

            // Top and bottom faces keep UV 0..1 regardless of scale, matching the single-ring in-game look.
            int light = LightTexture.FULL_BRIGHT;
            bufferQuad(pose, consumer, p4, p5, p6, p7, color, 0.0F, 0.0F, 1.0F, 1.0F, light, new Vector3f(0.0F, 1.0F, 0.0F));
            bufferQuad(pose, consumer, p0, p1, p2, p3, color, 0.0F, 0.0F, 1.0F, 1.0F, light, new Vector3f(0.0F, -1.0F, 0.0F));

            ms.popPose();
        }
    }

}
