package net.goldskinmc.creategeoresonance;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder STAGE1 = BUILDER.comment("Stage 1 seismic hammer tuning").push("stage1");

    public static final ForgeConfigSpec.IntValue RADIUS = STAGE1
        .comment("Horizontal scan radius in blocks.")
        .defineInRange("radius", 12, 1, 64);
    public static final ForgeConfigSpec.IntValue DEPTH = STAGE1
        .comment("Maximum depth in blocks for normal pressure scans.")
        .defineInRange("depth", 25, 1, 128);
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS = STAGE1
        .comment("Seismic hammer cooldown in ticks.")
        .defineInRange("cooldownTicks", 200, 0, 1200);
    public static final ForgeConfigSpec.IntValue MAX_ECHO_DELAY_TICKS = STAGE1
        .comment("Maximum echo return delay for the deepest anomaly in ticks.")
        .defineInRange("maxEchoDelayTicks", 30, 1, 200);
    public static final ForgeConfigSpec.IntValue MAX_ECHOES = STAGE1
        .comment("Maximum anomalies sent to clients for rendering.")
        .defineInRange("maxEchoes", 8, 1, 32);
    public static final ForgeConfigSpec.IntValue SCAN_BLOCK_BUDGET_PER_TICK = STAGE1
        .comment("Total scan block checks processed globally per server tick.")
        .defineInRange("scanBlockBudgetPerTick", 2400, 64, 250000);
    public static final ForgeConfigSpec.IntValue SCAN_SLICE_PER_JOB = STAGE1
        .comment("Maximum block checks per job before rotating to the next queued scan.")
        .defineInRange("scanSlicePerJob", 400, 16, 50000);
    public static final ForgeConfigSpec.IntValue SCANS_PER_BACKTANK = STAGE1
        .comment("Expected scans from a full copper backtank.")
        .defineInRange("scansPerBacktank", 24, 1, 200);
    public static final ForgeConfigSpec.DoubleValue LOW_PRESSURE_THRESHOLD = STAGE1
        .comment("Backtank fill ratio below which scans become weak.")
        .defineInRange("lowPressureThreshold", 0.25D, 0.01D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue SOFT_BLOCK_DEPTH_MULTIPLIER = STAGE1
        .comment("Depth multiplier when the hammer strikes soft terrain (sand/dirt/gravel).")
        .defineInRange("softBlockDepthMultiplier", 0.7D, 0.1D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue BASE_NOISE = STAGE1
        .comment("Baseline confidence jitter for anomaly evaluation.")
        .defineInRange("baseNoise", 0.1D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue NETHERITE_CLARITY_BONUS = STAGE1
        .comment("Extra confidence scaling for edge anomalies with netherite backtank.")
        .defineInRange("netheriteClarityBonus", 0.3D, 0.0D, 1.0D);

    static {
        STAGE1.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
