package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationControllerLogic;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationData;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMountedRuntime;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMountedVisualState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class SeismicStationRenderer extends KineticBlockEntityRenderer<SeismicStationBlockEntity> {
    private static final Direction LOCAL_SHAFT_AXIS = Direction.SOUTH;
    private static final float SHAFT_PIVOT_X = 8.0F;
    private static final float SHAFT_PIVOT_Y = 24.0F;
    private static final float SHAFT_PIVOT_Z = 8.0F;
    private static final float DRUM_PIVOT_X = -8.0F;
    private static final float DRUM_PIVOT_Y = 11.0F;
    private static final float DRUM_PIVOT_Z = 7.915F;
    private static final Direction LOCAL_SWING_AXIS = Direction.SOUTH;
    private static final float SWING_PIVOT_X = -8.0F;
    private static final float SWING_PIVOT_Y = 29.75F;
    private static final float SWING_PIVOT_Z = 8.0F;
    private static final float SWING_MAX_ANGLE_DEGREES = 5.0F;
    private static final float PISTON_TRAVEL_BLOCKS = 1.0F;
    private static final float MAX_SPEED_RISE_PORTION = 0.75F;
    private static final int MODULES_PER_ROW = 4;
    private static final float MODULE_START_X = -3.5F / 16.0F;
    private static final float MODULE_START_Y = 28.5F / 16.0F;
    private static final float MODULE_START_Z = 1.5F / 16.0F;
    private static final float MODULE_STEP_X = -3.0F / 16.0F;
    private static final float MODULE_STEP_Y = 3.5F / 16.0F;
    private static final float MODULE_RENDER_SCALE = 1.0F;
    private static final float MOUNTED_MODULE_FACE_OFFSET = -0.01F;

    public SeismicStationRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SeismicStationBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        SeismicStationBoundingBlockEntity input = blockEntity.getInputNode();
        float angle = shaftAngleFor(input, facing);
        float drumAngle = drumAngleFor(input, facing, 0.125F);
        RenderType renderType = getRenderType(blockEntity, state);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_TOP_SHAFT, state);
        orientToFacing(shaft, facing);
        rotateAroundLocalPivot(shaft, angle, LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
        shaft.light(light).renderInto(ms, vertexConsumer);

        SuperByteBuffer piston = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_HAMMER_PISTON, state);
        piston.translate(0.0F, pistonTravelFor(blockEntity, partialTicks), 0.0F);
        orientToFacing(piston, facing);
        piston.light(light).renderInto(ms, vertexConsumer);

        SuperByteBuffer drum = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_DRUM, state);
        orientToFacing(drum, facing);
        rotateAroundLocalPivot(drum, drumAngle, Direction.EAST, DRUM_PIVOT_X, DRUM_PIVOT_Y, DRUM_PIVOT_Z);
        drum.light(light).renderInto(ms, vertexConsumer);

        SuperByteBuffer swingingPart = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_SWINGING_PART, state);
        orientToFacing(swingingPart, facing);
        rotateAroundLocalPivot(swingingPart, swingingAngleFor(blockEntity, partialTicks), LOCAL_SWING_AXIS, SWING_PIVOT_X, SWING_PIVOT_Y, SWING_PIVOT_Z);
        swingingPart.light(light).renderInto(ms, vertexConsumer);

        SuperByteBuffer lever = CachedBuffers.partial(
            blockEntity.isStartLeverDown()
                ? GeoResonancePartialModels.SEISMIC_STATION_START_LEVER_DOWN
                : GeoResonancePartialModels.SEISMIC_STATION_START_LEVER_UP,
            state
        );
        orientToFacing(lever, facing);
        lever.light(light).renderInto(ms, vertexConsumer);

        if (blockEntity.hasPaperInput()) {
            SuperByteBuffer paper = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_INPUT_PAPER, state);
            orientToFacing(paper, facing);
            paper.light(light).renderInto(ms, vertexConsumer);
        }
        if (blockEntity.hasInkInput()) {
            SuperByteBuffer ink = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_INPUT_INK, state);
            orientToFacing(ink, facing);
            ink.light(light).renderInto(ms, vertexConsumer);
        }
        if (blockEntity.hasSeismogramOutput()) {
            SuperByteBuffer output = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            orientToFacing(output, facing);
            output.light(light).renderInto(ms, vertexConsumer);
        }

        renderInstalledModules(blockEntity, facing, ms, buffer, light, overlay);
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                           ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (context.world == null) {
            return;
        }

        if (context.blockEntityData == null) {
            context.blockEntityData = new CompoundTag();
        }

        SeismicStationData stationData = new SeismicStationData();
        stationData.readFromTag(context.blockEntityData);
        SeismicStationMountedVisualState visualState = SeismicStationMountedVisualState.getOrCreate(context);
        visualState.ensureCountdownBootstrapped(stationData, context.world.getGameTime());

        BlockState state = context.state;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        VertexConsumer vertexConsumer = buffer.getBuffer(resolveRenderType(state));
        int baseLight = LevelRenderer.getLightColor(renderWorld, context.localPos);

        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_TOP_SHAFT, state);
        shaft.transform(matrices.getModel());
        orientToFacing(shaft, facing);
        rotateAroundLocalPivot(shaft, mountedContinuousAngle(context, facing, 1.0F), LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
        shaft.light(baseLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        SuperByteBuffer piston = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_HAMMER_PISTON, state);
        piston.transform(matrices.getModel());
        piston.translate(0.0F, mountedPistonTravel(visualState, context), 0.0F);
        orientToFacing(piston, facing);
        piston.light(baseLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        SuperByteBuffer drum = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_DRUM, state);
        drum.transform(matrices.getModel());
        orientToFacing(drum, facing);
        rotateAroundLocalPivot(drum, mountedContinuousAngle(context, facing, 0.125F), Direction.EAST, DRUM_PIVOT_X, DRUM_PIVOT_Y, DRUM_PIVOT_Z);
        drum.light(baseLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        SuperByteBuffer swingingPart = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_SWINGING_PART, state);
        swingingPart.transform(matrices.getModel());
        orientToFacing(swingingPart, facing);
        rotateAroundLocalPivot(swingingPart, mountedSwingingAngle(visualState, context), LOCAL_SWING_AXIS, SWING_PIVOT_X, SWING_PIVOT_Y, SWING_PIVOT_Z);
        swingingPart.light(baseLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        SuperByteBuffer lever = CachedBuffers.partial(
            stationData.scanRunning() || stationData.awaitingScanResult()
                ? GeoResonancePartialModels.SEISMIC_STATION_START_LEVER_DOWN
                : GeoResonancePartialModels.SEISMIC_STATION_START_LEVER_UP,
            state
        );
        lever.transform(matrices.getModel());
        orientToFacing(lever, facing);
        lever.light(baseLight)
            .useLevelLight(context.world, matrices.getWorld())
            .renderInto(matrices.getViewProjection(), vertexConsumer);

        if (!stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_PAPER_INPUT).isEmpty()) {
            SuperByteBuffer paper = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_INPUT_PAPER, state);
            paper.transform(matrices.getModel());
            orientToFacing(paper, facing);
            paper.light(baseLight)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), vertexConsumer);
        }
        if (!stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_INK_INPUT).isEmpty()) {
            SuperByteBuffer ink = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_INPUT_INK, state);
            ink.transform(matrices.getModel());
            orientToFacing(ink, facing);
            ink.light(baseLight)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), vertexConsumer);
        }
        if (!stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            SuperByteBuffer output = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_OUTPUT_SEISMOGRAM, state);
            output.transform(matrices.getModel());
            orientToFacing(output, facing);
            output.light(baseLight)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), vertexConsumer);
        }

        BlockPos stationPos = mountedGlobalPos(context);
        int moduleLight = LevelRenderer.getLightColor(context.world, stationPos.above());
        renderMountedModules(stationData, facing, context, matrices, buffer, moduleLight);
    }

    private static void renderInstalledModules(SeismicStationBlockEntity blockEntity, Direction stationFacing,
                                               PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        List<ItemStack> modules = blockEntity.getInstalledModuleStacks();
        if (modules.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ms.pushPose();
        orientPoseToFacing(ms, stationFacing);

        for (int i = 0; i < modules.size(); i++) {
            int row = i / MODULES_PER_ROW;
            int col = i % MODULES_PER_ROW;
            float x = MODULE_START_X + col * MODULE_STEP_X;
            float y = MODULE_START_Y - row * MODULE_STEP_Y;

            ms.pushPose();
            ms.translate(x, y, MODULE_START_Z);
            ms.mulPose(Axis.YP.rotationDegrees(180.0F));
            ms.scale(MODULE_RENDER_SCALE, MODULE_RENDER_SCALE, MODULE_RENDER_SCALE);
            itemRenderer.renderStatic(modules.get(i), ItemDisplayContext.NONE, light, overlay, ms, buffer,
                blockEntity.getLevel(), i);
            ms.popPose();
        }
        ms.popPose();
    }

    private static float shaftAngleFor(SeismicStationBoundingBlockEntity input, Direction facing) {
        if (input == null || input.getLevel() == null) {
            return 0.0F;
        }
        Direction.Axis inputAxis = KineticBlockEntityRenderer.getRotationAxisOf(input);
        float rawAngle = sourceShaftAngle(input, facing, inputAxis);
        Direction shaftSide = facing.getOpposite();
        float sideSign = shaftSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        return rawAngle * sideSign;
    }

    private static float drumAngleFor(SeismicStationBoundingBlockEntity input, Direction facing, float speedMultiplier) {
        if (input == null || input.getLevel() == null) {
            return 0.0F;
        }

        Direction.Axis inputAxis = KineticBlockEntityRenderer.getRotationAxisOf(input);
        float rawAngle = continuousSourceAngle(input, facing, inputAxis, speedMultiplier);
        Direction shaftSide = facing.getOpposite();
        float sideSign = shaftSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        return rawAngle * sideSign;
    }

    private static float sourceShaftAngle(SeismicStationBoundingBlockEntity input, Direction facing, Direction.Axis inputAxis) {
        var level = input.getLevel();
        if (level == null) {
            return 0.0F;
        }

        var sourcePos = input.getBlockPos().relative(facing.getOpposite());
        if (level.getBlockEntity(sourcePos) instanceof KineticBlockEntity source) {
            Direction.Axis sourceAxis = KineticBlockEntityRenderer.getRotationAxisOf(source);
            if (sourceAxis == inputAxis) {
                return KineticBlockEntityRenderer.getAngleForBe(source, sourcePos, sourceAxis);
            }
        }

        return KineticBlockEntityRenderer.getAngleForBe(input, input.getBlockPos(), inputAxis);
    }

    private static float continuousSourceAngle(SeismicStationBoundingBlockEntity input, Direction facing, Direction.Axis inputAxis,
                                               float speedMultiplier) {
        var level = input.getLevel();
        if (level == null) {
            return 0.0F;
        }

        KineticBlockEntity angleEntity = input;
        var anglePos = input.getBlockPos();
        float speed = input.getSpeed();

        var sourcePos = input.getBlockPos().relative(facing.getOpposite());
        if (level.getBlockEntity(sourcePos) instanceof KineticBlockEntity source) {
            Direction.Axis sourceAxis = KineticBlockEntityRenderer.getRotationAxisOf(source);
            if (sourceAxis == inputAxis) {
                angleEntity = source;
                anglePos = sourcePos;
                speed = source.getSpeed();
            }
        }

        float time = AnimationTickHolder.getRenderTime(level);
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(angleEntity, anglePos, inputAxis);
        float rawDegrees = time * speed * 3f / 10 + offset;
        float scaledDegrees = rawDegrees * speedMultiplier;
        return (scaledDegrees % 360f) / 180f * (float) Math.PI;
    }

    private static float pistonTravelFor(SeismicStationBlockEntity blockEntity, float partialTicks) {
        float phase = blockEntity.getClientStrikeAnimationPhase(partialTicks);
        if (phase <= 0.0F) {
            return 0.0F;
        }
        return PISTON_TRAVEL_BLOCKS * pistonCurve(phase, blockEntity.getClientStrikeIntervalTicks());
    }

    private static float pistonCurve(float phase, int intervalTicks) {
        int minIntervalTicks = Math.max(1, Config.STATION_MIN_STRIKE_INTERVAL_TICKS.get() * Config.STATION_STRIKE_INTERVAL_MULTIPLIER.get());
        float descentTicks = (1.0F - MAX_SPEED_RISE_PORTION) * minIntervalTicks;
        float descentPortion = Math.min(0.95F, Math.max(0.05F, descentTicks / Math.max(1, intervalTicks)));
        float risePortion = 1.0F - descentPortion;

        if (phase <= risePortion) {
            float t = phase / risePortion;
            return t * t;
        }

        float t = (phase - risePortion) / descentPortion;
        return 1.0F - (t * t * t);
    }

    private static float swingingAngleFor(SeismicStationBlockEntity blockEntity, float partialTicks) {
        float phase = blockEntity.getClientSwingAnimationPhase(partialTicks);
        if (phase <= 0.0F) {
            return 0.0F;
        }

        float smoothPhase = phase * phase * (3.0F - 2.0F * phase);
        float swingDegrees = SWING_MAX_ANGLE_DEGREES * (float) Math.sin(smoothPhase * ((float) Math.PI * 2.0F));
        return swingDegrees * ((float) Math.PI / 180.0F);
    }

    private static float mountedPistonTravel(SeismicStationMountedVisualState visualState, MovementContext context) {
        float phase = visualState.strikePhase(context.world.getGameTime(), AnimationTickHolder.getPartialTicks());
        if (phase <= 0.0F) {
            return 0.0F;
        }
        return PISTON_TRAVEL_BLOCKS * pistonCurve(phase, SeismicStationMountedRuntime.calculateStrikeIntervalTicks());
    }

    private static float mountedSwingingAngle(SeismicStationMountedVisualState visualState, MovementContext context) {
        float phase = visualState.swingPhase(context.world.getGameTime(), AnimationTickHolder.getPartialTicks());
        if (phase <= 0.0F) {
            return 0.0F;
        }
        float smoothPhase = phase * phase * (3.0F - 2.0F * phase);
        float swingDegrees = SWING_MAX_ANGLE_DEGREES * (float) Math.sin(smoothPhase * ((float) Math.PI * 2.0F));
        return swingDegrees * ((float) Math.PI / 180.0F);
    }

    private static float mountedContinuousAngle(MovementContext context, Direction facing, float speedMultiplier) {
        Direction.Axis axis = KineticBlockEntityVisual.rotationAxis(context.state);
        BlockPos anglePos = context.localPos;
        BlockState angleState = context.state;
        float speed = readMountedSpeed(context.blockEntityData);

        if (context.contraption != null) {
            BlockPos sourcePos = context.localPos.relative(facing.getOpposite());
            var sourceInfo = context.contraption.getBlocks().get(sourcePos);
            if (sourceInfo != null) {
                Direction.Axis sourceAxis = KineticBlockEntityVisual.rotationAxis(sourceInfo.state());
                if (sourceAxis == axis) {
                    anglePos = sourcePos;
                    angleState = sourceInfo.state();
                    speed = readMountedSpeed(sourceInfo.nbt(), speed);
                }
            }
        }

        float time = AnimationTickHolder.getRenderTime(context.world);
        float offset = KineticBlockEntityVisual.rotationOffset(angleState, axis, anglePos);
        float degrees = (time * speed * 3.0F / 10.0F + offset) * speedMultiplier;
        float angle = (degrees % 360.0F) / 180.0F * (float) Math.PI;
        Direction shaftSide = facing.getOpposite();
        float sideSign = shaftSide.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : -1.0F;
        return angle * sideSign;
    }

    private static float readMountedSpeed(CompoundTag tag) {
        return readMountedSpeed(tag, 0.0F);
    }

    private static float readMountedSpeed(CompoundTag tag, float fallback) {
        if (tag == null || !tag.contains("Speed")) {
            return fallback;
        }
        return tag.getFloat("Speed");
    }

    private static BlockPos mountedGlobalPos(MovementContext context) {
        if (context.position != null) {
            return BlockPos.containing(context.position);
        }
        if (context.contraption != null && context.contraption.entity != null) {
            Vec3 globalCenter = context.contraption.entity.toGlobalVector(
                Vec3.atCenterOf(context.localPos),
                AnimationTickHolder.getPartialTicks()
            );
            return BlockPos.containing(globalCenter);
        }
        return context.localPos;
    }

    private static void renderMountedModules(SeismicStationData stationData, Direction stationFacing, MovementContext context,
                                             ContraptionMatrices matrices, MultiBufferSource buffer, int light) {
        List<ItemStack> modules = SeismicStationControllerLogic.getInstalledModuleStacks(stationData.inventory());
        if (modules.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        PoseStack projected = new PoseStack();
        ContraptionMatrices.transform(projected, matrices.getViewProjection());
        ContraptionMatrices.transform(projected, matrices.getModel());
        projected.pushPose();
        orientPoseToFacing(projected, stationFacing);
        Lighting.setupForFlatItems();

        for (int i = 0; i < modules.size(); i++) {
            int row = i / MODULES_PER_ROW;
            int col = i % MODULES_PER_ROW;
            float x = MODULE_START_X + col * MODULE_STEP_X;
            float y = MODULE_START_Y - row * MODULE_STEP_Y;

            projected.pushPose();
            projected.translate(x, y, MODULE_START_Z + MOUNTED_MODULE_FACE_OFFSET);
            projected.mulPose(Axis.YP.rotationDegrees(180.0F));
            projected.scale(MODULE_RENDER_SCALE, MODULE_RENDER_SCALE, MODULE_RENDER_SCALE);
            itemRenderer.renderStatic(modules.get(i), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, projected, buffer,
                context.world, i);
            projected.popPose();
        }

        Lighting.setupFor3DItems();
        projected.popPose();
    }

    private static RenderType resolveRenderType(BlockState state) {
        BakedModel model = Minecraft.getInstance()
            .getBlockRenderer()
            .getBlockModel(state);
        ChunkRenderTypeSet typeSet = model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY);
        for (RenderType type : KineticBlockEntityRenderer.REVERSED_CHUNK_BUFFER_LAYERS) {
            if (typeSet.contains(type)) {
                return type;
            }
        }
        return RenderType.cutoutMipped();
    }

    private static void orientToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.center()
            .rotateYDegrees(180 + AngleHelper.horizontalAngle(facing))
            .uncenter();
    }

    private static void orientPoseToFacing(PoseStack ms, Direction facing) {
        ms.translate(0.5D, 0.5D, 0.5D);
        ms.mulPose(Axis.YP.rotationDegrees(180 + AngleHelper.horizontalAngle(facing)));
        ms.translate(-0.5D, -0.5D, -0.5D);
    }

    private static void rotateAroundLocalPivot(SuperByteBuffer buffer, float angle, Direction axisDirection,
                                               float pivotX, float pivotY, float pivotZ) {
        float px = pivotX / 16.0F;
        float py = pivotY / 16.0F;
        float pz = pivotZ / 16.0F;
        buffer.translate(px, py, pz)
            .rotate(angle, axisDirection)
            .translate(-px, -py, -pz);
    }
}
