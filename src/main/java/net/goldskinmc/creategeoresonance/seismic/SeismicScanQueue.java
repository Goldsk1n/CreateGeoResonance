package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.Set;

public final class SeismicScanQueue {
    private static final Deque<SeismicScanJob> JOBS = new ArrayDeque<>();

    private SeismicScanQueue() {
    }

    public static void enqueue(SeismicScanRequest request) {
        JOBS.addLast(new SeismicScanJob(request));
    }

    public static void clear() {
        JOBS.clear();
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) {
            return;
        }

        int budget = Config.SCAN_BLOCK_BUDGET_PER_TICK.get();
        int maxPerJob = Config.SCAN_SLICE_PER_JOB.get();
        int rotations = JOBS.size();

        while (budget > 0 && rotations-- > 0 && !JOBS.isEmpty()) {
            SeismicScanJob job = JOBS.pollFirst();
            int consumed = job.process(Math.min(maxPerJob, budget));
            budget -= consumed;

            if (job.isComplete()) {
                job.finish();
            } else {
                JOBS.addLast(job);
            }
        }
    }

    static List<SeismicAnomaly> prioritizeAndLimitAnomalies(List<SeismicAnomaly> rawAnomalies, int mergeDistance, int maxEchoes) {
        if (rawAnomalies.isEmpty()) {
            return rawAnomalies;
        }

        rawAnomalies.sort((left, right) -> {
            int priorityCompare = Integer.compare(typePriority(right.type()), typePriority(left.type()));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(anomalyStrength(right), anomalyStrength(left));
        });

        List<SeismicAnomaly> selected = new ArrayList<>();
        for (SeismicAnomaly candidate : rawAnomalies) {
            int overlapIndex = findOverlappingIndex(selected, candidate, mergeDistance);
            if (overlapIndex < 0) {
                selected.add(candidate);
                continue;
            }

            SeismicAnomaly current = selected.get(overlapIndex);
            if (shouldReplace(current, candidate)) {
                selected.set(overlapIndex, candidate);
            }
        }

        selected.sort((left, right) -> {
            int priorityCompare = Integer.compare(typePriority(right.type()), typePriority(left.type()));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(anomalyStrength(right), anomalyStrength(left));
        });

        if (selected.size() > maxEchoes) {
            return new ArrayList<>(selected.subList(0, maxEchoes));
        }
        return selected;
    }

    private static int findOverlappingIndex(List<SeismicAnomaly> selected, SeismicAnomaly candidate, int mergeDistance) {
        for (int i = 0; i < selected.size(); i++) {
            SeismicAnomaly existing = selected.get(i);
            int effectiveMergeDistance = Math.max(mergeDistance, (existing.radius() + candidate.radius()) / 2);
            int dx = existing.offsetX() - candidate.offsetX();
            int dz = existing.offsetZ() - candidate.offsetZ();
            if (dx * dx + dz * dz <= effectiveMergeDistance * effectiveMergeDistance) {
                return i;
            }
        }
        return -1;
    }

    private static boolean shouldReplace(SeismicAnomaly current, SeismicAnomaly candidate) {
        int currentPriority = typePriority(current.type());
        int candidatePriority = typePriority(candidate.type());
        if (candidatePriority != currentPriority) {
            return candidatePriority > currentPriority;
        }
        return anomalyStrength(candidate) > anomalyStrength(current);
    }

    private static double anomalyStrength(SeismicAnomaly anomaly) {
        return anomaly.confidence() * (1.0D + anomaly.radius() * 0.2D);
    }

    private static int typePriority(SeismicAnomalyType type) {
        return switch (type) {
            case LAVA -> 5;
            case WATER -> 4;
            case IRON, COPPER -> 3;
            case CAVE -> 2;
            case SOLID -> 0;
        };
    }

    @FunctionalInterface
    public interface ResultConsumer {
        void accept(SeismicScanRequest request, List<SeismicAnomaly> anomalies);
    }

    public record SeismicScanRequest(
        ServerLevel level,
        BlockPos origin,
        int scannerEntityId,
        int radius,
        int depth,
        float noise,
        long startTick,
        boolean lowPressure,
        Set<SeismicAnomalyType> detectableTypes,
        @Nullable ResultConsumer resultConsumer
    ) {
        private static final Set<SeismicAnomalyType> DEFAULT_DETECTABLE_TYPES = Set.of(
            SeismicAnomalyType.CAVE,
            SeismicAnomalyType.WATER,
            SeismicAnomalyType.LAVA
        );

        public SeismicScanRequest {
            detectableTypes = detectableTypes == null || detectableTypes.isEmpty()
                ? DEFAULT_DETECTABLE_TYPES
                : Set.copyOf(detectableTypes);
        }

        public SeismicScanRequest(
            ServerLevel level,
            BlockPos origin,
            int scannerEntityId,
            int radius,
            int depth,
            float noise,
            long startTick,
            boolean lowPressure
        ) {
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure, DEFAULT_DETECTABLE_TYPES, null);
        }

        public SeismicScanRequest(
            ServerLevel level,
            BlockPos origin,
            int scannerEntityId,
            int radius,
            int depth,
            float noise,
            long startTick,
            boolean lowPressure,
            @Nullable ResultConsumer resultConsumer
        ) {
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure, DEFAULT_DETECTABLE_TYPES, resultConsumer);
        }

        public boolean canDetect(SeismicAnomalyType type) {
            return detectableTypes.contains(type);
        }
    }

    private static final class SeismicScanJob {
        private final SeismicScanRequest request;
        private final RandomSource random;
        private final int width;
        private final int totalCells;
        private final Map<Integer, Aggregate> aggregates;
        private int index;

        private SeismicScanJob(SeismicScanRequest request) {
            this.request = request;
            this.random = RandomSource.create(request.origin().asLong() ^ request.startTick());
            this.width = request.radius() * 2 + 1;
            this.totalCells = width * width * request.depth();
            this.aggregates = new HashMap<>();
            this.index = 0;
        }

        private int process(int budget) {
            int consumed = 0;
            while (consumed < budget && index < totalCells) {
                int depthIndex = index / (width * width);
                int horizontalIndex = index % (width * width);
                int dx = horizontalIndex % width - request.radius();
                int dz = horizontalIndex / width - request.radius();
                index++;
                consumed++;

                if (dx * dx + dz * dz > request.radius() * request.radius()) {
                    continue;
                }

                BlockPos scanPos = request.origin().offset(dx, -(depthIndex + 1), dz);
                if (!request.level().isLoaded(scanPos)) {
                    continue;
                }

                BlockState state = request.level().getBlockState(scanPos);
                FluidState fluid = state.getFluidState();
                SeismicAnomalyType type;
                if (!fluid.isEmpty()) {
                    type = fluid.is(FluidTags.LAVA) ? SeismicAnomalyType.LAVA : SeismicAnomalyType.WATER;
                } else if (state.isAir()) {
                    type = SeismicAnomalyType.CAVE;
                } else if (state.is(BlockTags.IRON_ORES)) {
                    type = SeismicAnomalyType.IRON;
                } else if (state.is(BlockTags.COPPER_ORES)) {
                    type = SeismicAnomalyType.COPPER;
                } else {
                    continue;
                }
                if (!request.canDetect(type)) {
                    continue;
                }

                float depthRatio = (depthIndex + 1) / (float) request.depth();
                float distance = Mth.sqrt(dx * dx + dz * dz);
                float distanceRatio = distance / request.radius();
                float attenuationExponent = 1.9F;
                float distanceFactor = Mth.clamp(1.0F - (float) Math.pow(distanceRatio, attenuationExponent), 0.0F, 1.0F);
                float depthFactor = 1.0F - depthRatio;
                float noise = (random.nextFloat() - 0.5F) * request.noise();
                float weight = Mth.clamp(depthFactor * (0.4F + distanceFactor * 0.6F) + noise, 0.0F, 1.0F);
                if (weight <= 0.02F) {
                    continue;
                }

                int cellSize = 4;
                int cellX = Math.floorDiv(dx + request.radius(), cellSize);
                int cellZ = Math.floorDiv(dz + request.radius(), cellSize);
                int key = (type.ordinal() << 28) | (cellX << 14) | cellZ;
                aggregates.computeIfAbsent(key, ignored -> new Aggregate(type))
                    .add(dx, dz, depthIndex + 1, weight);
            }
            return consumed;
        }

        private boolean isComplete() {
            return index >= totalCells;
        }

        private void finish() {
            List<SeismicAnomaly> anomalies = buildAnomalies(aggregates.values());
            if (request.resultConsumer() != null) {
                request.resultConsumer().accept(request, anomalies);
            } else {
                GeoResonancePackets.sendSeismicResult(
                    request.level(),
                    request.origin(),
                    request.scannerEntityId(),
                    request.lowPressure(),
                    request.depth(),
                    anomalies
                );
            }
        }

        private List<SeismicAnomaly> buildAnomalies(Collection<Aggregate> values) {
            List<SeismicAnomaly> rawAnomalies = new ArrayList<>();

            for (Aggregate aggregate : values) {
                if (aggregate.samples == 0 || aggregate.totalWeight <= 0.0F) {
                    continue;
                }

                float avgDx = aggregate.weightedDx / aggregate.totalWeight;
                float avgDz = aggregate.weightedDz / aggregate.totalWeight;
                float avgDepth = aggregate.weightedDepth / aggregate.totalWeight;
                float distance = Mth.sqrt(avgDx * avgDx + avgDz * avgDz);
                float distanceRatio = Mth.clamp(distance / request.radius(), 0.0F, 1.0F);
                float baseConfidence = aggregate.totalWeight / Math.max(1.0F, aggregate.samples * 0.7F);
                float clarity = Mth.clamp(1.0F - distanceRatio, 0.0F, 1.0F);
                float confidence = Mth.clamp(baseConfidence * (0.5F + clarity * 0.5F), 0.04F, 1.0F);
                int spanX = aggregate.maxDx - aggregate.minDx;
                int spanZ = aggregate.maxDz - aggregate.minDz;
                int radius = Mth.clamp(Math.max(1, Math.max(spanX, spanZ) / 2 + 1), 1, request.radius());

                rawAnomalies.add(new SeismicAnomaly(
                    aggregate.type,
                    Mth.floor(avgDx),
                    Mth.floor(avgDz),
                    Mth.clamp(Math.round(avgDepth), 1, request.depth()),
                    radius,
                    confidence
                ));
            }

            return prioritizeAndLimitAnomalies(rawAnomalies, Config.ECHO_MERGE_DISTANCE.get(), Config.MAX_ECHOES.get());
        }
    }

    private static final class Aggregate {
        private final SeismicAnomalyType type;
        private float weightedDx;
        private float weightedDz;
        private float weightedDepth;
        private float totalWeight;
        private int samples;
        private int minDx;
        private int maxDx;
        private int minDz;
        private int maxDz;

        private Aggregate(SeismicAnomalyType type) {
            this.type = type;
            this.weightedDx = 0.0F;
            this.weightedDz = 0.0F;
            this.weightedDepth = 0.0F;
            this.totalWeight = 0.0F;
            this.samples = 0;
            this.minDx = Integer.MAX_VALUE;
            this.maxDx = Integer.MIN_VALUE;
            this.minDz = Integer.MAX_VALUE;
            this.maxDz = Integer.MIN_VALUE;
        }

        private void add(int dx, int dz, int depth, float weight) {
            weightedDx += dx * weight;
            weightedDz += dz * weight;
            weightedDepth += depth * weight;
            totalWeight += weight;
            samples++;
            minDx = Math.min(minDx, dx);
            maxDx = Math.max(maxDx, dx);
            minDz = Math.min(minDz, dz);
            maxDz = Math.max(maxDz, dz);
        }
    }
}
