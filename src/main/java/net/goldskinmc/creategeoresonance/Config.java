package net.goldskinmc.creategeoresonance;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder STAGE1 = BUILDER.comment("Stage 1 seismic hammer tuning").push("stage1");
    private static final ForgeConfigSpec.Builder STAGE2 = BUILDER.comment("Stage 2 seismic station tuning").push("stage2");
    private static final ForgeConfigSpec.Builder PROJECTOR_VISUALS = CLIENT_BUILDER
        .comment("Client-side seismic projector hologram rendering.")
        .push("projectorVisuals");

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
    public static final ForgeConfigSpec.IntValue ECHO_MERGE_DISTANCE = STAGE1
        .comment("Minimum horizontal distance in blocks used to merge nearby anomalies into one echo.")
        .defineInRange("echoMergeDistance", 5, 1, 32);
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

    public static final ForgeConfigSpec.IntValue STATION_RADIUS = STAGE2
        .comment("Horizontal scan radius in blocks for the seismic station.")
        .defineInRange("stationRadius", 16, 1, 96);
    public static final ForgeConfigSpec.IntValue STATION_DEPTH = STAGE2
        .comment("Maximum scan depth in blocks for the seismic station.")
        .defineInRange("stationDepth", 40, 1, 192);
    public static final ForgeConfigSpec.IntValue STATION_STRIKE_INTERVAL_TICKS = STAGE2
        .comment("Base ticks between station strikes at minimum required speed.")
        .defineInRange("stationStrikeIntervalTicks", 20, 1, 200);
    public static final ForgeConfigSpec.IntValue STATION_MIN_STRIKE_INTERVAL_TICKS = STAGE2
        .comment("Lower bound for strike interval when the station is overdriven.")
        .defineInRange("stationMinStrikeIntervalTicks", 6, 1, 200);
    public static final ForgeConfigSpec.IntValue STATION_STRIKE_INTERVAL_MULTIPLIER = STAGE2
        .comment("Global multiplier applied to station strike intervals.")
        .defineInRange("stationStrikeIntervalMultiplier", 5, 1, 20);
    public static final ForgeConfigSpec.IntValue STATION_COOLDOWN_TICKS = STAGE2
        .comment("Cooldown in ticks after a station scan is finished.")
        .defineInRange("stationCooldownTicks", 200, 0, 2400);
    public static final ForgeConfigSpec.IntValue STATION_STARTUP_DELAY_TICKS = STAGE2
        .comment("Small delay before the first strike after scan results are ready.")
        .defineInRange("stationStartupDelayTicks", 12, 0, 200);
    public static final ForgeConfigSpec.IntValue STATION_MIN_SPEED = STAGE2
        .comment("Minimum absolute RPM required on the station input shaft to start scanning.")
        .defineInRange("stationMinSpeed", 24, 1, 512);
    public static final ForgeConfigSpec.DoubleValue STATION_MAX_SPEED_BONUS = STAGE2
        .comment("Maximum strike speed multiplier from RPM scaling.")
        .defineInRange("stationMaxSpeedBonus", 2.5D, 1.0D, 10.0D);
    public static final ForgeConfigSpec.DoubleValue STATION_STRESS_IMPACT = STAGE2
        .comment("Base stress impact applied by the seismic station block.")
        .defineInRange("stationStressImpact", 12.0D, 0.0D, 1024.0D);
    public static final ForgeConfigSpec.DoubleValue STATION_NOISE = STAGE2
        .comment("Confidence jitter used for station anomaly evaluation.")
        .defineInRange("stationNoise", 0.08D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.BooleanValue PROJECTOR_FILL_ENABLED = PROJECTOR_VISUALS
        .comment("Render hologram fill in addition to outlines.")
        .define("fillEnabled", true);
    public static final ForgeConfigSpec.IntValue PROJECTOR_FILL_ALPHA = PROJECTOR_VISUALS
        .comment("Fill alpha channel (0-255).")
        .defineInRange("fillAlpha", 96, 0, 255);
    public static final ForgeConfigSpec.IntValue PROJECTOR_EDGE_ALPHA = PROJECTOR_VISUALS
        .comment("Outline alpha channel (0-255).")
        .defineInRange("edgeAlpha", 220, 0, 255);
    public static final ForgeConfigSpec.BooleanValue PROJECTOR_GUIDE_LINES_ENABLED = PROJECTOR_VISUALS
        .comment("Render dashed guide lines from the projector to detected targets.")
        .define("guideLinesEnabled", true);

    static {
        STAGE1.pop();
        STAGE2.pop();
        PROJECTOR_VISUALS.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();
    static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private Config() {
    }
}
