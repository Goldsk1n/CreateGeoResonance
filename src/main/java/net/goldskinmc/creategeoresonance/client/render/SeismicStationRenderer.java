package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
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
    private static final Direction LOCAL_SHAFT_AXIS = Direction.EAST;
    private static final float SHAFT_PIVOT_X = 8.0F;
    private static final float SHAFT_PIVOT_Y = 24.0F;
    private static final float SHAFT_PIVOT_Z = 8.0F;
    private static final float DRUM_PIVOT_X = -8.0F;
    private static final float DRUM_PIVOT_Y = 11.0F;
    private static final float DRUM_PIVOT_Z = 7.915F;

    public SeismicStationRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SeismicStationBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        SeismicStationBoundingBlockEntity input = blockEntity.getInputNode();
        float angle = angleFor(input);
        RenderType renderType = getRenderType(blockEntity, state);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        SuperByteBuffer shaft = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_TOP_SHAFT, state);
        rotateAroundLocalPivot(shaft, angle, LOCAL_SHAFT_AXIS, SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z);
        orientToFacing(shaft, facing);
        shaft.light(light).renderInto(ms, vertexConsumer);

        SuperByteBuffer drum = CachedBuffers.partial(GeoResonancePartialModels.SEISMIC_STATION_DRUM, state);
        orientToFacing(drum, facing);
        rotateAroundLocalPivot(drum, angle, Direction.EAST, DRUM_PIVOT_X, DRUM_PIVOT_Y, DRUM_PIVOT_Z);
        drum.light(light).renderInto(ms, vertexConsumer);
    }

    private static float angleFor(SeismicStationBoundingBlockEntity input) {
        if (input == null || input.getLevel() == null) {
            return 0.0F;
        }
        float time = AnimationTickHolder.getRenderTime(input.getLevel());
        return ((time * input.getSpeed() * 3f / 10) % 360) / 180f * (float) Math.PI;
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
