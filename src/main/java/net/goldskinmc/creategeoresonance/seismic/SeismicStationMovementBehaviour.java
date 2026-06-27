package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.goldskinmc.creategeoresonance.client.render.SeismicStationRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

public class SeismicStationMovementBehaviour implements MovementBehaviour {
    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        SeismicStationMountedRuntime.tick(context);
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        SeismicStationRenderer.renderInContraption(context, renderWorld, matrices, buffer);
    }
}
