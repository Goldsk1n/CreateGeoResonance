package net.goldskinmc.creategeoresonance.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class PlacedSeismicHammerRenderer extends KineticBlockEntityRenderer<PlacedSeismicHammerBlockEntity> {
    public PlacedSeismicHammerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PlacedSeismicHammerBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = getRenderedBlockState(blockEntity);
        SuperByteBuffer rotor = getRotatedModel(blockEntity, state);
        renderRotatingBuffer(blockEntity, rotor, ms, buffer.getBuffer(getRenderType(blockEntity, state)), light);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PlacedSeismicHammerBlockEntity blockEntity, BlockState state) {
        PartialModel rotor = GeoResonancePartialModels.PLACED_SEISMIC_HAMMER_ROTOR;
        return CachedBuffers.partial(rotor, state);
    }
}
