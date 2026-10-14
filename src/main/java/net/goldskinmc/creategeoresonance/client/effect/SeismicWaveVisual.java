package net.goldskinmc.creategeoresonance.client.effect;

import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.goldskinmc.creategeoresonance.client.GeoResonancePartialModels;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SeismicWaveVisual extends AbstractVisual implements EffectVisual<SeismicWaveEffect>, SimpleDynamicVisual {
    // Avoid OIT's depth-writing composite: fading waves must not occlude water.
    private static final Material WAVE_MATERIAL = SimpleMaterial.builderOf(Materials.TRANSLUCENT_BLOCK)
        .transparency(Transparency.TRANSLUCENT)
        .depthTest(DepthTest.LEQUAL)
        .writeMask(WriteMask.COLOR)
        .build();
    private static final RendererReloadCache<PartialModel, Model> WAVE_MODELS = new RendererReloadCache<>(partial ->
        new BakedModelBuilder(partial.get())
            .materialFunc((renderType, shaded, ambientOcclusion) -> WAVE_MATERIAL)
            .build());

    private final SeismicWaveEffect effect;
    private final TransformedInstance wave;

    public SeismicWaveVisual(VisualizationContext context, SeismicWaveEffect effect, float partialTick) {
        super(context, effect.clientLevel(), partialTick);
        this.effect = effect;
        this.wave = instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, WAVE_MODELS.get(GeoResonancePartialModels.SEISMIC_WAVE), 4)
            .createInstance();
        updateTransform(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        updateTransform(ctx.partialTick());
    }

    private void updateTransform(float partialTick) {
        float progress = Mth.clamp(effect.age(partialTick) / effect.lifetimeTicks(), 0.0F, 1.0F);
        if (progress >= 1.0F) {
            wave.setZeroTransform().setChanged();
            return;
        }

        Vec3 center = effect.center();
        float x = (float) (center.x - renderOrigin().getX());
        float y = (float) (center.y - renderOrigin().getY());
        float z = (float) (center.z - renderOrigin().getZ());

        float radius = 0.35F + progress * effect.maxRadius();
        float blur = effect.blur();
        float thickness = 0.04F + blur * 0.08F;
        float alpha = Mth.clamp(effect.opacity() * (1.0F - progress), 0.0F, 1.0F);
        int argb = ((int) (alpha * 255.0F) << 24) | (effect.rgb() & 0x00FFFFFF);

        wave.setIdentityTransform()
            .translate(x, y + 0.02F + blur * 0.03F, z)
            .translate(-radius, 0.0F, -radius)
            .scale(radius * 2.0F, thickness, radius * 2.0F)
            .colorArgb(argb)
            .light(0xF000F0)
            .setChanged();
    }

    @Override
    protected void _delete() {
        wave.delete();
    }
}
