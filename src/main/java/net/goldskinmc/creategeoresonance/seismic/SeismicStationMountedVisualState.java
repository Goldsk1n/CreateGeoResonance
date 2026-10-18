package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class SeismicStationMountedVisualState {
    private static final long NO_CYCLE = Long.MIN_VALUE;

    private long strikeCycleStartTick = NO_CYCLE;
    private int strikeIntervalTicks = 1;
    private final SeismicStationPenSpring penSpring = new SeismicStationPenSpring();

    public static SeismicStationMountedVisualState getOrCreate(MovementContext context) {
        if (context.temporaryData instanceof SeismicStationMountedVisualState state) {
            return state;
        }
        SeismicStationMountedVisualState state = new SeismicStationMountedVisualState();
        context.temporaryData = state;
        return state;
    }

    public void syncFromState(CompoundTag previousTag, CompoundTag currentTag, long gameTime) {
        SeismicStationData previous = new SeismicStationData();
        previous.readFromTag(previousTag == null ? new CompoundTag() : previousTag);
        SeismicStationData current = new SeismicStationData();
        current.readFromTag(currentTag == null ? new CompoundTag() : currentTag);

        if (!current.scanRunning() || current.awaitingScanResult()) {
            strikeCycleStartTick = NO_CYCLE;
            return;
        }

        boolean enteredReplay = !previous.scanRunning() || previous.awaitingScanResult();
        boolean revealAdvanced = current.revealIndex() != previous.revealIndex();
        if ((enteredReplay || revealAdvanced) && current.strikeTimer() > 0) {
            startStrikeCycle(gameTime, current.strikeTimer());
            return;
        }

        ensureCountdownBootstrapped(current, gameTime);
    }

    public void onStrikeImpact(long gameTime) {
        int interval = SeismicStationMountedRuntime.calculateStrikeIntervalTicks();
        startStrikeCycle(gameTime, interval);
        penSpring.onStrike(gameTime);
    }

    public void onEchoArrival(long gameTime) {
        penSpring.onEcho(gameTime);
    }

    public void ensureCountdownBootstrapped(SeismicStationData stationData, long gameTime) {
        if (strikeCycleStartTick != NO_CYCLE) {
            return;
        }
        if (!stationData.scanRunning() || stationData.awaitingScanResult() || stationData.strikeTimer() <= 0) {
            return;
        }
        int interval = SeismicStationMountedRuntime.calculateStrikeIntervalTicks();
        int remaining = Mth.clamp(stationData.strikeTimer(), 0, interval);
        strikeIntervalTicks = Math.max(1, interval);
        strikeCycleStartTick = gameTime - (strikeIntervalTicks - remaining);
    }

    public float strikePhase(long gameTime, float partialTicks) {
        if (strikeCycleStartTick == NO_CYCLE) {
            return 0.0F;
        }
        float progress = (float) ((gameTime + partialTicks - strikeCycleStartTick) / (double) Math.max(1, strikeIntervalTicks));
        if (progress >= 1.0F || progress < 0.0F) {
            return 0.0F;
        }
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public float swingAngle(long gameTime, float partialTicks) {
        return penSpring.angleRadians(gameTime, partialTicks);
    }

    private void startStrikeCycle(long gameTime, int intervalTicks) {
        strikeIntervalTicks = Math.max(1, intervalTicks);
        strikeCycleStartTick = gameTime;
    }

}
