package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.Set;

public final class SeismicScanQueue {
    private static final Deque<SeismicScanJob> JOBS = new ArrayDeque<>();

    private SeismicScanQueue() {
    }

    private static TagKey<Block> createZincOresTag() {
        return TagHolder.CREATE_ZINC_ORES;
    }

    private static final class TagHolder {
        private static final TagKey<Block> CREATE_ZINC_ORES = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath("forge", "ores/zinc"));

        private TagHolder() {
        }
    }

    public static void enqueue(SeismicScanRequest request) {
        SeismicScanJob job = new SeismicScanJob(request);
        if (request.resultConsumer() != null) {
            JOBS.addFirst(job);
            return;
        }
        JOBS.addLast(job);
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
        int timeBudgetMicros = Config.SCAN_TIME_BUDGET_MICROS.get();
        long deadlineNanos = timeBudgetMicros <= 0
            ? Long.MAX_VALUE
            : System.nanoTime() + (long) timeBudgetMicros * 1_000L;
        int rotations = JOBS.size();

        while (budget > 0 && rotations-- > 0 && !JOBS.isEmpty() && System.nanoTime() < deadlineNanos) {
            SeismicScanJob job = JOBS.pollFirst();
            int sliceCap = maxPerJob;
            if (job.isPriorityJob()) {
                sliceCap = Math.min(budget, Math.max(maxPerJob, maxPerJob * 6));
            }
            int consumed = job.process(Math.min(sliceCap, budget), deadlineNanos);
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
            case COAL, IRON, COPPER, GOLD, REDSTONE, LAPIS, EMERALD, DIAMOND, ZINC, AMETHYST -> 3;
            case CHEST, SPAWNER -> 2;
            case CAVE -> 2;
            case SOLID -> 0;
        };
    }

    @FunctionalInterface
    public interface ResultConsumer {
        void accept(SeismicScanRequest request, SeismicScanResult result);
    }

    public record SeismicScanResult(List<SeismicAnomaly> anomalies, List<ExactCluster> exactClusters) {
    }

    public record ExactCluster(SeismicAnomalyType type, List<BlockPos> blocks) {
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
        boolean allowBelowZeroOres,
        @Nullable ResultConsumer resultConsumer
    ) {
        private static final Set<SeismicAnomalyType> DEFAULT_DETECTABLE_TYPES = Set.of(
            SeismicAnomalyType.CAVE,
            SeismicAnomalyType.WATER,
            SeismicAnomalyType.LAVA
        );

        public SeismicScanRequest {
            // `null` means "use default ambient detections". An explicit empty set means "detect nothing".
            detectableTypes = detectableTypes == null
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
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure, DEFAULT_DETECTABLE_TYPES, false, null);
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
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure, DEFAULT_DETECTABLE_TYPES, false, resultConsumer);
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
            Set<SeismicAnomalyType> detectableTypes,
            @Nullable ResultConsumer resultConsumer
        ) {
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure, detectableTypes, false, resultConsumer);
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
        private final Map<SeismicAnomalyType, Set<BlockPos>> exactHitsByType;
        private final boolean detectCave;
        private final boolean detectWater;
        private final boolean detectLava;
        private final boolean detectAmethyst;
        private final boolean detectSpawner;
        private final boolean detectChest;
        private final boolean detectCoal;
        private final boolean detectIron;
        private final boolean detectCopper;
        private final boolean detectGold;
        private final boolean detectRedstone;
        private final boolean detectLapis;
        private final boolean detectEmerald;
        private final boolean detectDiamond;
        private final boolean detectZinc;
        private final boolean detectAnyOre;
        private final boolean detectAnySolidTarget;
        private int index;

        private SeismicScanJob(SeismicScanRequest request) {
            this.request = request;
            this.random = RandomSource.create(request.origin().asLong() ^ request.startTick());
            this.width = request.radius() * 2 + 1;
            this.totalCells = width * width * request.depth();
            this.aggregates = new HashMap<>();
            this.exactHitsByType = new HashMap<>();
            this.detectCave = request.canDetect(SeismicAnomalyType.CAVE);
            this.detectWater = request.canDetect(SeismicAnomalyType.WATER);
            this.detectLava = request.canDetect(SeismicAnomalyType.LAVA);
            this.detectAmethyst = request.canDetect(SeismicAnomalyType.AMETHYST);
            this.detectSpawner = request.canDetect(SeismicAnomalyType.SPAWNER);
            this.detectChest = request.canDetect(SeismicAnomalyType.CHEST);
            this.detectCoal = request.canDetect(SeismicAnomalyType.COAL);
            this.detectIron = request.canDetect(SeismicAnomalyType.IRON);
            this.detectCopper = request.canDetect(SeismicAnomalyType.COPPER);
            this.detectGold = request.canDetect(SeismicAnomalyType.GOLD);
            this.detectRedstone = request.canDetect(SeismicAnomalyType.REDSTONE);
            this.detectLapis = request.canDetect(SeismicAnomalyType.LAPIS);
            this.detectEmerald = request.canDetect(SeismicAnomalyType.EMERALD);
            this.detectDiamond = request.canDetect(SeismicAnomalyType.DIAMOND);
            this.detectZinc = request.canDetect(SeismicAnomalyType.ZINC);
            this.detectAnyOre = detectCoal
                || detectIron
                || detectCopper
                || detectGold
                || detectRedstone
                || detectLapis
                || detectEmerald
                || detectDiamond
                || detectZinc;
            this.detectAnySolidTarget = detectAmethyst
                || detectSpawner
                || detectChest
                || detectAnyOre;
            this.index = 0;
        }

        private int process(int budget, long deadlineNanos) {
            int consumed = 0;
            while (consumed < budget && index < totalCells) {
                if ((consumed & 63) == 0 && System.nanoTime() >= deadlineNanos) {
                    break;
                }

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
                    if (fluid.is(FluidTags.LAVA)) {
                        if (!detectLava) {
                            continue;
                        }
                        type = SeismicAnomalyType.LAVA;
                    } else {
                        if (!detectWater) {
                            continue;
                        }
                        type = SeismicAnomalyType.WATER;
                    }
                } else if (state.isAir()) {
                    if (!detectCave) {
                        continue;
                    }
                    type = SeismicAnomalyType.CAVE;
                } else {
                    if (!detectAnySolidTarget) {
                        continue;
                    }
                    if (detectAmethyst && (state.is(Blocks.BUDDING_AMETHYST)
                        || state.is(Blocks.AMETHYST_CLUSTER)
                        || state.is(Blocks.LARGE_AMETHYST_BUD)
                        || state.is(Blocks.MEDIUM_AMETHYST_BUD)
                        || state.is(Blocks.SMALL_AMETHYST_BUD))) {
                        type = SeismicAnomalyType.AMETHYST;
                    } else if (detectSpawner && state.is(Blocks.SPAWNER)) {
                        type = SeismicAnomalyType.SPAWNER;
                    } else if (detectChest && (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))) {
                        type = SeismicAnomalyType.CHEST;
                    } else {
                        boolean oreDepthAllowed = scanPos.getY() >= 0 || request.allowBelowZeroOres();
                        if (!detectAnyOre || !oreDepthAllowed) {
                            continue;
                        }
                        if (detectCoal && state.is(BlockTags.COAL_ORES)) {
                            type = SeismicAnomalyType.COAL;
                        } else if (detectIron && state.is(BlockTags.IRON_ORES)) {
                            type = SeismicAnomalyType.IRON;
                        } else if (detectCopper && state.is(BlockTags.COPPER_ORES)) {
                            type = SeismicAnomalyType.COPPER;
                        } else if (detectGold && state.is(BlockTags.GOLD_ORES)) {
                            type = SeismicAnomalyType.GOLD;
                        } else if (detectRedstone && state.is(BlockTags.REDSTONE_ORES)) {
                            type = SeismicAnomalyType.REDSTONE;
                        } else if (detectLapis && state.is(BlockTags.LAPIS_ORES)) {
                            type = SeismicAnomalyType.LAPIS;
                        } else if (detectEmerald && state.is(BlockTags.EMERALD_ORES)) {
                            type = SeismicAnomalyType.EMERALD;
                        } else if (detectDiamond && state.is(BlockTags.DIAMOND_ORES)) {
                            type = SeismicAnomalyType.DIAMOND;
                        } else if (detectZinc && state.is(createZincOresTag())) {
                            type = SeismicAnomalyType.ZINC;
                        } else {
                            continue;
                        }
                    }
                }
                trackExactHit(type, scanPos);

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

        private boolean isPriorityJob() {
            return request.resultConsumer() != null;
        }

        private void finish() {
            SeismicScanResult result = buildResult();
            if (request.resultConsumer() != null) {
                request.resultConsumer().accept(request, result);
            } else {
                GeoResonancePackets.sendSeismicResult(
                    request.level(),
                    request.origin(),
                    request.scannerEntityId(),
                    request.lowPressure(),
                    request.depth(),
                    result.anomalies()
                );
            }
        }

        private SeismicScanResult buildResult() {
            List<SeismicAnomaly> anomalies = buildAnomalies(aggregates.values());
            List<ExactCluster> exactClusters = buildExactClusters();
            return new SeismicScanResult(anomalies, exactClusters);
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

            appendChestMinecartAnomalies(rawAnomalies);
            return prioritizeAndLimitAnomalies(rawAnomalies, Config.ECHO_MERGE_DISTANCE.get(), Config.MAX_ECHOES.get());
        }

        private void appendChestMinecartAnomalies(List<SeismicAnomaly> rawAnomalies) {
            if (!request.canDetect(SeismicAnomalyType.CHEST)) {
                return;
            }
            BlockPos origin = request.origin();
            AABB bounds = new AABB(
                origin.getX() - request.radius(),
                origin.getY() - request.depth(),
                origin.getZ() - request.radius(),
                origin.getX() + request.radius() + 1.0D,
                origin.getY() + 1.0D,
                origin.getZ() + request.radius() + 1.0D
            );
            for (MinecartChest chestMinecart : request.level().getEntitiesOfClass(MinecartChest.class, bounds)) {
                if (chestMinecart.blockPosition().getY() < 0 && !request.allowBelowZeroOres()) {
                    continue;
                }
                int offsetX = Mth.floor(chestMinecart.getX()) - origin.getX();
                int offsetZ = Mth.floor(chestMinecart.getZ()) - origin.getZ();
                if (offsetX * offsetX + offsetZ * offsetZ > request.radius() * request.radius()) {
                    continue;
                }
                int depth = Mth.clamp(origin.getY() - Mth.floor(chestMinecart.getY()), 1, request.depth());
                rawAnomalies.add(new SeismicAnomaly(SeismicAnomalyType.CHEST, offsetX, offsetZ, depth, 1, 1.0F));
                trackExactHit(SeismicAnomalyType.CHEST, chestMinecart.blockPosition());
            }
        }

        private void trackExactHit(SeismicAnomalyType type, BlockPos scanPos) {
            if (!shouldTrackExactType(type)) {
                return;
            }
            exactHitsByType.computeIfAbsent(type, ignored -> new HashSet<>())
                .add(scanPos.immutable());
        }

        private List<ExactCluster> buildExactClusters() {
            if (exactHitsByType.isEmpty()) {
                return List.of();
            }

            List<ExactCluster> clusters = new ArrayList<>();
            for (Map.Entry<SeismicAnomalyType, Set<BlockPos>> entry : exactHitsByType.entrySet()) {
                SeismicAnomalyType type = entry.getKey();
                Set<BlockPos> unvisited = new HashSet<>(entry.getValue());
                if (unvisited.isEmpty()) {
                    continue;
                }

                while (!unvisited.isEmpty()) {
                    BlockPos seed = unvisited.iterator().next();
                    Deque<BlockPos> queue = new ArrayDeque<>();
                    queue.add(seed);
                    List<BlockPos> component = new ArrayList<>();

                    while (!queue.isEmpty()) {
                        BlockPos current = queue.removeFirst();
                        if (!unvisited.remove(current)) {
                            continue;
                        }
                        component.add(current);
                        for (Direction direction : Direction.values()) {
                            BlockPos neighbour = current.relative(direction);
                            if (unvisited.contains(neighbour)) {
                                queue.addLast(neighbour);
                            }
                        }
                    }

                    if (!component.isEmpty()) {
                        clusters.add(new ExactCluster(type, List.copyOf(component)));
                    }
                }
            }
            return List.copyOf(clusters);
        }

        private static boolean shouldTrackExactType(SeismicAnomalyType type) {
            return switch (type) {
                case WATER, LAVA, COAL, IRON, COPPER, GOLD, REDSTONE, LAPIS, EMERALD, DIAMOND, ZINC, AMETHYST, CHEST, SPAWNER -> true;
                default -> false;
            };
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
