package net.goldskinmc.creategeoresonance.client.effect;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class SeismicWaveEffect implements Effect {
    private final ClientLevel level;
    private final Vec3 center;
    private final int rgb;
    private final float opacity;
    private final float blur;
    private final int lifetimeTicks;
    private final float maxRadius;
    private final long startGameTime;

    public SeismicWaveEffect(ClientLevel level, Vec3 center, int rgb, float opacity, float blur, int lifetimeTicks, float maxRadius) {
        this.level = level;
        this.center = center;
        this.rgb = rgb;
        this.opacity = opacity;
        this.blur = blur;
        this.lifetimeTicks = lifetimeTicks;
        this.maxRadius = maxRadius;
        this.startGameTime = level.getGameTime();
    }

    @Override
    public LevelAccessor level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new SeismicWaveVisual(ctx, this, partialTick);
    }

    public ClientLevel clientLevel() {
        return level;
    }

    public Vec3 center() {
        return center;
    }

    public int rgb() {
        return rgb;
    }

    public float opacity() {
        return opacity;
    }

    public float blur() {
        return blur;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public float maxRadius() {
        return maxRadius;
    }

    public float age(float partialTick) {
        return (level.getGameTime() - startGameTime) + partialTick;
    }
}
