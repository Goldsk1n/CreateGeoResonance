package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SeismicStationRenderer extends KineticBlockEntityRenderer<SeismicStationBlockEntity> {
    private static final Direction LOCAL_SHAFT_AXIS = Direction.SOUTH;
    private static final float SHAFT_PIVOT_X = 8.0F;
    private static final float SHAFT_PIVOT_Y = 24.0F;
    private static final float SHAFT_PIVOT_Z = 8.0F;
    private static final float DRUM_PIVOT_X = -8.0F;
    private static final float DRUM_PIVOT_Y = 11.0F;
    private static final float DRUM_PIVOT_Z = 7.915F;
    private static final float PISTON_TRAVEL_BLOCKS = 1.0F;
    private static final float MAX_SPEED_RISE_PORTION = 0.75F;

    public SeismicStationRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SeismicStationBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        SeismicStationBoundingBlockEntity input = blockEntity.getInputNode();
        float angle = angleFor(input, facing, 1.0F);
        float drumAngle = angleFor(input, facing, 0.125F);
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
    }

    private static float angleFor(SeismicStationBoundingBlockEntity input, Direction facing, float speedMultiplier) {
        if (input == null || input.getLevel() == null) {
            return 0.0F;
        }
        Direction.Axis inputAxis = KineticBlockEntityRenderer.getRotationAxisOf(input);
        float time = AnimationTickHolder.getRenderTime(input.getLevel());
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(input, input.getBlockPos(), inputAxis);
        float rawDegrees = time * input.getSpeed() * 3f / 10 + offset;
        float rawAngle = (rawDegrees * speedMultiplier % 360f) / 180f * (float) Math.PI;
        float facingSign = facing.getAxis() == Direction.Axis.X ? -1.0F : 1.0F;
        return rawAngle * facingSign;
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

    private static void orientToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.center()
            .rotateYDegrees(180 + AngleHelper.horizontalAngle(facing))
            .uncenter();
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
