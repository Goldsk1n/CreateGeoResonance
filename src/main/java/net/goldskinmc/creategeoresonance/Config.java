package net.goldskinmc.creategeoresonance;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue RADIUS;
    public static final ForgeConfigSpec.IntValue DEPTH;
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_ECHO_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_ECHOES;
    public static final ForgeConfigSpec.IntValue ECHO_MERGE_DISTANCE;
    public static final ForgeConfigSpec.IntValue SCANS_PER_BACKTANK;
    public static final ForgeConfigSpec.DoubleValue LOW_PRESSURE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SOFT_BLOCK_DEPTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_NOISE;

    public static final ForgeConfigSpec.IntValue SCAN_BLOCK_BUDGET_PER_TICK;
    public static final ForgeConfigSpec.IntValue SCAN_SLICE_PER_JOB;
    public static final ForgeConfigSpec.IntValue SCAN_TIME_BUDGET_MICROS;

    public static final ForgeConfigSpec.IntValue STATION_RADIUS;
    public static final ForgeConfigSpec.IntValue STATION_STRIKE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue STATION_MIN_STRIKE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue STATION_STRIKE_INTERVAL_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue STATION_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue STATION_MAX_SPEED_BONUS;
    public static final ForgeConfigSpec.DoubleValue STATION_STRESS_IMPACT;
    public static final ForgeConfigSpec.DoubleValue STATION_NOISE;

    public static final ForgeConfigSpec.IntValue PROJECTOR_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue PROJECTOR_STRESS_IMPACT;
    public static final ForgeConfigSpec.IntValue PROJECTOR_STATION_RANGE_CHUNKS;
    public static final ForgeConfigSpec.IntValue PROJECTOR_MIN_NODE_DISTANCE_BLOCKS;
    public static final ForgeConfigSpec.IntValue PROJECTOR_MATCH_RADIUS_BLOCKS;

    public static final ForgeConfigSpec.BooleanValue PROJECTOR_FILL_ENABLED;
    public static final ForgeConfigSpec.IntValue PROJECTOR_FILL_ALPHA;
    public static final ForgeConfigSpec.IntValue PROJECTOR_EDGE_ALPHA;
    public static final ForgeConfigSpec.DoubleValue PROJECTOR_BLOCK_EDGE_INSET;
    public static final ForgeConfigSpec.BooleanValue PROJECTOR_GUIDE_LINES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue PROJECTOR_GUIDE_LINE_WIDTH;
    public static final ForgeConfigSpec.IntValue PROJECTOR_VISIBLE_VEINS_MAX;
    public static final ForgeConfigSpec.DoubleValue PROJECTOR_PONDER_BLOCK_EDGE_INSET;
    public static final ForgeConfigSpec.DoubleValue PROJECTOR_PONDER_GUIDE_LINE_WIDTH;

    public static final ForgeConfigSpec.BooleanValue SEISMOGRAM_SIDEBAR_ENABLED;
    public static final ForgeConfigSpec.IntValue SEISMOGRAM_SIDEBAR_MAX_SIGNALS;
    public static final ForgeConfigSpec.BooleanValue SEISMOGRAM_BOUNDARY_OVERLAY_ENABLED;

    public static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE_SCALE;

    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec CLIENT_SPEC;

    static {
        BUILDER.comment("Stage 1 seismic hammer tuning").push("hammer");
        RADIUS = BUILDER
            .comment("Horizontal scan radius in blocks.")
            .defineInRange("radius", 12, 1, 64);
        DEPTH = BUILDER
            .comment("Maximum depth in blocks for normal pressure scans.")
            .defineInRange("depth", 25, 1, 128);
        COOLDOWN_TICKS = BUILDER
            .comment("Seismic hammer cooldown in ticks.")
            .defineInRange("cooldownTicks", 200, 0, 1200);
        MAX_ECHO_DELAY_TICKS = BUILDER
            .comment("Maximum echo return delay for the deepest anomaly in ticks.")
            .defineInRange("maxEchoDelayTicks", 30, 1, 200);
        MAX_ECHOES = BUILDER
            .comment("Maximum anomalies sent to clients for rendering.")
            .defineInRange("maxEchoes", 8, 1, 32);
        ECHO_MERGE_DISTANCE = BUILDER
            .comment("Minimum horizontal distance in blocks used to merge nearby anomalies into one echo.")
            .defineInRange("echoMergeDistance", 5, 1, 32);
        SCANS_PER_BACKTANK = BUILDER
            .comment("Expected scans from a full copper backtank.")
            .defineInRange("scansPerBacktank", 24, 1, 200);
        LOW_PRESSURE_THRESHOLD = BUILDER
            .comment("Backtank fill ratio below which scans become weak.")
            .defineInRange("lowPressureThreshold", 0.25D, 0.01D, 1.0D);
        SOFT_BLOCK_DEPTH_MULTIPLIER = BUILDER
            .comment("Depth multiplier when the hammer strikes soft terrain (sand/dirt/gravel).")
            .defineInRange("softBlockDepthMultiplier", 0.7D, 0.1D, 1.0D);
        BASE_NOISE = BUILDER
            .comment("Baseline confidence jitter for anomaly evaluation.")
            .defineInRange("baseNoise", 0.1D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.comment("Global seismic scan queue budgeting and pacing.").push("scanQueue");
        SCAN_BLOCK_BUDGET_PER_TICK = BUILDER
            .comment("Total scan block checks processed globally per server tick.")
            .defineInRange("blockBudgetPerTick", 4800, 64, 250000);
        SCAN_SLICE_PER_JOB = BUILDER
            .comment("Maximum block checks per job before rotating to the next queued scan.")
            .defineInRange("slicePerJob", 800, 16, 50000);
        SCAN_TIME_BUDGET_MICROS = BUILDER
            .comment("Soft time cap for seismic scan queue work per server tick in microseconds. Set 0 to disable.")
            .defineInRange("timeBudgetMicros", 3000, 0, 20000);
        BUILDER.pop();

        BUILDER.comment("Stage 2 seismic station tuning").push("station");
        STATION_RADIUS = BUILDER
            .comment("Horizontal scan radius in blocks for the seismic station.")
            .defineInRange("radius", 16, 1, 96);
        STATION_STRIKE_INTERVAL_TICKS = BUILDER
            .comment("Base ticks between station strikes at minimum required speed.")
            .defineInRange("strikeIntervalTicks", 20, 1, 200);
        STATION_MIN_STRIKE_INTERVAL_TICKS = BUILDER
            .comment("Lower bound for strike interval when the station is overdriven.")
            .defineInRange("minStrikeIntervalTicks", 6, 1, 200);
        STATION_STRIKE_INTERVAL_MULTIPLIER = BUILDER
            .comment("Global multiplier applied to station strike intervals.")
            .defineInRange("strikeIntervalMultiplier", 5, 1, 20);
        STATION_MIN_SPEED = BUILDER
            .comment("Minimum absolute RPM required on the station input shaft to start scanning.")
            .defineInRange("minSpeed", 32, 1, 512);
        STATION_MAX_SPEED_BONUS = BUILDER
            .comment("Maximum strike speed multiplier from RPM scaling.")
            .defineInRange("maxSpeedBonus", 2.5D, 1.0D, 10.0D);
        STATION_STRESS_IMPACT = BUILDER
            .comment("Base stress impact at minimum station speed.")
            .defineInRange("stressImpact", 12.0D, 0.0D, 1024.0D);
        STATION_NOISE = BUILDER
            .comment("Confidence jitter used for station anomaly evaluation.")
            .defineInRange("noise", 0.08D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.comment("Seismic projector gameplay tuning.").push("projector");
        PROJECTOR_MIN_SPEED = BUILDER
            .comment("Minimum absolute RPM required for the seismic projector to operate.")
            .defineInRange("minSpeed", 32, 1, 512);
        PROJECTOR_STRESS_IMPACT = BUILDER
            .comment("Base stress impact applied by the seismic projector.")
            .defineInRange("stressImpact", 8.0D, 0.0D, 1024.0D);
        PROJECTOR_STATION_RANGE_CHUNKS = BUILDER
            .comment("Maximum horizontal range in chunks from projector to station for loading seismograms.")
            .defineInRange("stationRangeChunks", 3, 0, 64);
        PROJECTOR_MIN_NODE_DISTANCE_BLOCKS = BUILDER
            .comment("Minimum horizontal distance in blocks between loaded station nodes.")
            .defineInRange("minNodeDistanceBlocks", 8, 1, 64);
        PROJECTOR_MATCH_RADIUS_BLOCKS = BUILDER
            .comment("Maximum horizontal matching radius in blocks when triangulating signals.")
            .defineInRange("matchRadiusBlocks", 12, 1, 64);
        BUILDER.pop();

        CLIENT_BUILDER.comment("Client-side seismic projector hologram rendering.").push("projectorVisuals");
        PROJECTOR_FILL_ENABLED = CLIENT_BUILDER
            .comment("Render hologram fill in addition to outlines.")
            .define("fillEnabled", true);
        PROJECTOR_FILL_ALPHA = CLIENT_BUILDER
            .comment("Fill alpha channel (0-255).")
            .defineInRange("fillAlpha", 96, 0, 255);
        PROJECTOR_EDGE_ALPHA = CLIENT_BUILDER
            .comment("Outline alpha channel (0-255).")
            .defineInRange("edgeAlpha", 255, 0, 255);
        PROJECTOR_BLOCK_EDGE_INSET = CLIENT_BUILDER
            .comment("World hologram face offset from block borders. Negative expands, positive shrinks.")
            .defineInRange("blockEdgeInset", 0.0D, -0.45D, 0.45D);
        PROJECTOR_GUIDE_LINES_ENABLED = CLIENT_BUILDER
            .comment("Render dashed guide lines from the projector to detected targets.")
            .define("guideLinesEnabled", true);
        PROJECTOR_GUIDE_LINE_WIDTH = CLIENT_BUILDER
            .comment("In-world projector dashed guide line thickness.")
            .defineInRange("guideLineWidth", 0.5D, 0.2D, 1.0D);
        PROJECTOR_VISIBLE_VEINS_MAX = CLIENT_BUILDER
            .comment("Maximum number of exact veins displayed at once. Closest veins are prioritized.")
            .defineInRange("visibleVeinsMax", 10, 0, 256);
        PROJECTOR_PONDER_BLOCK_EDGE_INSET = CLIENT_BUILDER
            .comment("Ponder-only hologram face offset from block borders. Negative expands, positive shrinks.")
            .defineInRange("ponderBlockEdgeInset", 0.0D, -0.45D, 0.45D);
        PROJECTOR_PONDER_GUIDE_LINE_WIDTH = CLIENT_BUILDER
            .comment("Ponder projector dashed guide line thickness.")
            .defineInRange("ponderGuideLineWidth", 0.5D, 0.2D, 1.0D);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("Client-side seismogram UI and projector placement guides.").push("seismogramUi");
        SEISMOGRAM_SIDEBAR_ENABLED = CLIENT_BUILDER
            .comment("Show nearest signal sidebar while holding a seismogram.")
            .define("sidebarEnabled", true);
        SEISMOGRAM_SIDEBAR_MAX_SIGNALS = CLIENT_BUILDER
            .comment("Maximum number of nearest signals shown in the seismogram sidebar.")
            .defineInRange("sidebarMaxSignals", 5, 1, 16);
        SEISMOGRAM_BOUNDARY_OVERLAY_ENABLED = CLIENT_BUILDER
            .comment("Show triangulation invalid-distance boundary when a nearby projector has exactly one loaded node.")
            .define("boundaryOverlayEnabled", true);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("Client-side camera feedback.").push("camera");
        CAMERA_SHAKE_ENABLED = CLIENT_BUILDER
            .comment("Enable seismic camera shake effects.")
            .define("shakeEnabled", true);
        CAMERA_SHAKE_SCALE = CLIENT_BUILDER
            .comment("Multiplier applied to seismic camera shake intensity.")
            .defineInRange("shakeScale", 1.0D, 0.0D, 2.0D);
        CLIENT_BUILDER.pop();

        SPEC = BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
    }

    private Config() {
    }
}
