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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            ResourceLocation.fromNamespaceAndPath("c", "ores/zinc"));

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

    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) {
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
        return prioritizeAndLimitAnomalies(rawAnomalies, mergeDistance, maxEchoes, Map.of());
    }

    static List<SeismicAnomaly> prioritizeAndLimitAnomalies(List<SeismicAnomaly> rawAnomalies, int mergeDistance,
                                                          int maxEchoes, Map<SeismicAnomalyType, Integer> modulePriorities) {
        if (rawAnomalies.isEmpty()) {
            return rawAnomalies;
        }

        Comparator<SeismicAnomaly> priority = (left, right) -> {
            int priorityCompare = Integer.compare(priorityScore(right.type(), modulePriorities),
                priorityScore(left.type(), modulePriorities));
            return priorityCompare != 0 ? priorityCompare
                : Double.compare(anomalyStrength(right), anomalyStrength(left));
        };
        // Apply module order before either merging signals or enforcing the echo cap.
        rawAnomalies.sort(priority);

        List<SeismicAnomaly> selected = new ArrayList<>();
        for (SeismicAnomaly candidate : rawAnomalies) {
            int overlapIndex = findOverlappingIndex(selected, candidate, mergeDistance, modulePriorities);
            if (overlapIndex < 0) {
                selected.add(candidate);
                continue;
            }

            SeismicAnomaly current = selected.get(overlapIndex);
            if (priority.compare(candidate, current) < 0) {
                selected.set(overlapIndex, candidate);
            }
        }

        selected.sort(priority);
        if (!modulePriorities.isEmpty()) {
            return selectModuleSignalsRoundRobin(selected, maxEchoes, modulePriorities);
        }
        if (selected.size() > maxEchoes) {
            return new ArrayList<>(selected.subList(0, maxEchoes));
        }
        return selected;
    }

    private static List<SeismicAnomaly> selectModuleSignalsRoundRobin(List<SeismicAnomaly> selected, int maxEchoes,
                                                                     Map<SeismicAnomalyType, Integer> modulePriorities) {
        // Input is ordered by module slot and strength, so each mineral's strongest signal goes first.
        Map<SeismicAnomalyType, Deque<SeismicAnomaly>> targets = new LinkedHashMap<>();
        List<SeismicAnomaly> ambient = new ArrayList<>();
        for (SeismicAnomaly anomaly : selected) {
            SeismicAnomalyType mineral = anomaly.type().mineralType();
            if (modulePriorities.containsKey(mineral)) {
                targets.computeIfAbsent(mineral, ignored -> new ArrayDeque<>()).addLast(anomaly);
            } else {
                ambient.add(anomaly);
            }
        }

        List<SeismicAnomaly> balanced = new ArrayList<>();
        while (!targets.isEmpty() && balanced.size() < maxEchoes) {
            var groups = targets.values().iterator();
            while (groups.hasNext() && balanced.size() < maxEchoes) {
                Deque<SeismicAnomaly> group = groups.next();
                balanced.add(group.removeFirst());
                if (group.isEmpty()) {
                    groups.remove();
                }
            }
        }
        for (SeismicAnomaly anomaly : ambient) {
            if (balanced.size() >= maxEchoes) {
                break;
            }
            balanced.add(anomaly);
        }
        return balanced;
    }

    private static int findOverlappingIndex(List<SeismicAnomaly> selected, SeismicAnomaly candidate, int mergeDistance,
                                             Map<SeismicAnomalyType, Integer> modulePriorities) {
        for (int i = 0; i < selected.size(); i++) {
            SeismicAnomaly existing = selected.get(i);
            SeismicAnomalyType existingMineral = existing.type().mineralType();
            SeismicAnomalyType candidateMineral = candidate.type().mineralType();
            if (existingMineral != candidateMineral && modulePriorities.containsKey(existingMineral)
                && modulePriorities.containsKey(candidateMineral)) {
                continue;
            }
            int effectiveMergeDistance = Math.max(mergeDistance, (existing.radius() + candidate.radius()) / 2);
            int dx = existing.offsetX() - candidate.offsetX();
            int dz = existing.offsetZ() - candidate.offsetZ();
            if (dx * dx + dz * dz <= effectiveMergeDistance * effectiveMergeDistance) {
                return i;
            }
        }
        return -1;
    }

    private static int priorityScore(SeismicAnomalyType type, Map<SeismicAnomalyType, Integer> modulePriorities) {
        Integer moduleSlot = modulePriorities.get(type.mineralType());
        return moduleSlot != null ? 1000 + moduleSlot : typePriority(type);
    }

    private static double anomalyStrength(SeismicAnomaly anomaly) {
        return anomaly.confidence() * (1.0D + anomaly.radius() * 0.2D);
    }

    private static int typePriority(SeismicAnomalyType type) {
        return switch (type.kind()) {
            case LAVA -> 5;
            case WATER -> 4;
            case COAL, COAL_DEPOSIT, IRON, IRON_DEPOSIT, COPPER, COPPER_DEPOSIT, GOLD, GOLD_DEPOSIT, REDSTONE,
                REDSTONE_DEPOSIT, LAPIS, LAPIS_DEPOSIT, EMERALD, DIAMOND, ZINC, ZINC_DEPOSIT, AMETHYST, CRIMSITE,
                ASURINE, OCHRUM, VERIDIUM, THORIUM, THORIUM_DEPOSIT, MAGNETITE, LEAD, LEAD_DEPOSIT, NICKEL,
                NICKEL_DEPOSIT, LITHIUM, BAUXITE, GALENA, LIGNITE, SULFUR, CRUDE_OIL, WOLFRAMITE, WOLFRAMITE_DEPOSIT,
                URANIUM, URANIUM_DEPOSIT, AUTUNITE, NITRATE, TIN, TIN_DEPOSIT, SILVER, SILVER_DEPOSIT, PLATINUM,
                PLATINUM_DEPOSIT, OSMIUM, OSMIUM_DEPOSIT, QUARTZ, QUARTZ_DEPOSIT, COBALT, COBALT_DEPOSIT,
                ANCIENT_DEBRIS, FLUORITE, CERTUS_QUARTZ,
                INFERIUM, PROSPERITY, SOULIUM,
                ARCANE_CRYSTAL, RUNE, STELLARITE, URANINITE, DRACONIUM, BLACK_QUARTZ -> 3;
            case CHEST, SPAWNER -> 2;
            case CAVE -> 2;
            case SOLID -> 0;
            case CUSTOM -> 3;
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
        Map<SeismicAnomalyType, Integer> modulePriorities,
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
            modulePriorities = Map.copyOf(modulePriorities);
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
            this(level, origin, scannerEntityId, radius, depth, noise, startTick, lowPressure,
                detectableTypes, Map.of(), resultConsumer);
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
            return detectableTypes.contains(type) || detectableTypes.contains(type.mineralType());
        }
    }

    private static final class SeismicScanJob {
        private final SeismicScanRequest request;
        private final RandomSource random;
        private final int width;
        private final int totalCells;
        private final Map<Long, Aggregate> aggregates;
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
        private final boolean detectCrimsite;
        private final boolean detectAsurine;
        private final boolean detectOchrum;
        private final boolean detectVeridium;
        private final boolean detectThorium;
        private final boolean detectMagnetite;
        private final boolean detectLead;
        private final boolean detectNickel;
        private final boolean detectLithium;
        private final boolean detectBauxite;
        private final boolean detectGalena;
        private final boolean detectLignite;
        private final boolean detectSulfur;
        private final boolean detectCrudeOil;
        private final boolean detectWolframite;
        private final boolean detectUranium;
        private final boolean detectAutunite;
        private final boolean detectNitrate;
        private final boolean detectQuartz;
        private final boolean detectAncientDebris;
        private final boolean detectTin;
        private final boolean detectOsmium;
        private final boolean detectFluorite;
        private final boolean detectCertusQuartz;
        private final boolean detectInferium;
        private final boolean detectProsperity;
        private final boolean detectSoulium;
        private final boolean detectArcaneCrystal;
        private final boolean detectRune;
        private final boolean detectStellarite;
        private final boolean detectUraninite;
        private final boolean detectDraconium;
        private final boolean detectBlackQuartz;
        private final boolean detectSilver;
        private final boolean detectPlatinum;
        private final Map<Block, SeismicAnomalyType> rockStoneTargets;
        private final Map<Block, SeismicAnomalyType> customTargets;
        private final boolean detectAnyOre;
        private final boolean detectAnySolidTarget;
        private final int[] horizontalDxByIndex;
        private final int[] horizontalDzByIndex;
        private final int[] cellXByHorizontalIndex;
        private final int[] cellZByHorizontalIndex;
        private final float[] horizontalWeightBaseByIndex;
        private final boolean[] inRadiusByIndex;
        private final float[] depthFactorByIndex;
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
            this.detectCrimsite = request.canDetect(SeismicAnomalyType.CRIMSITE);
            this.detectAsurine = request.canDetect(SeismicAnomalyType.ASURINE);
            this.detectOchrum = request.canDetect(SeismicAnomalyType.OCHRUM);
            this.detectVeridium = request.canDetect(SeismicAnomalyType.VERIDIUM);
            this.detectThorium = request.canDetect(SeismicAnomalyType.THORIUM);
            this.detectMagnetite = request.canDetect(SeismicAnomalyType.MAGNETITE);
            this.detectLead = request.canDetect(SeismicAnomalyType.LEAD);
            this.detectNickel = request.canDetect(SeismicAnomalyType.NICKEL);
            this.detectLithium = request.canDetect(SeismicAnomalyType.LITHIUM);
            this.detectBauxite = request.canDetect(SeismicAnomalyType.BAUXITE);
            this.detectGalena = request.canDetect(SeismicAnomalyType.GALENA);
            this.detectLignite = request.canDetect(SeismicAnomalyType.LIGNITE);
            this.detectSulfur = request.canDetect(SeismicAnomalyType.SULFUR);
            this.detectCrudeOil = request.canDetect(SeismicAnomalyType.CRUDE_OIL);
            this.detectWolframite = request.canDetect(SeismicAnomalyType.WOLFRAMITE);
            this.detectUranium = request.canDetect(SeismicAnomalyType.URANIUM);
            this.detectAutunite = request.canDetect(SeismicAnomalyType.AUTUNITE);
            this.detectNitrate = request.canDetect(SeismicAnomalyType.NITRATE);
            this.detectQuartz = request.canDetect(SeismicAnomalyType.QUARTZ);
            this.detectAncientDebris = request.canDetect(SeismicAnomalyType.ANCIENT_DEBRIS);
            this.detectTin = request.canDetect(SeismicAnomalyType.TIN);
            this.detectOsmium = request.canDetect(SeismicAnomalyType.OSMIUM);
            this.detectFluorite = request.canDetect(SeismicAnomalyType.FLUORITE);
            this.detectCertusQuartz = request.canDetect(SeismicAnomalyType.CERTUS_QUARTZ);
            this.detectInferium = request.canDetect(SeismicAnomalyType.INFERIUM);
            this.detectProsperity = request.canDetect(SeismicAnomalyType.PROSPERITY);
            this.detectSoulium = request.canDetect(SeismicAnomalyType.SOULIUM);
            this.detectArcaneCrystal = request.canDetect(SeismicAnomalyType.ARCANE_CRYSTAL);
            this.detectRune = request.canDetect(SeismicAnomalyType.RUNE);
            this.detectStellarite = request.canDetect(SeismicAnomalyType.STELLARITE);
            this.detectUraninite = request.canDetect(SeismicAnomalyType.URANINITE);
            this.detectDraconium = request.canDetect(SeismicAnomalyType.DRACONIUM);
            this.detectBlackQuartz = request.canDetect(SeismicAnomalyType.BLACK_QUARTZ);
            this.detectSilver = request.canDetect(SeismicAnomalyType.SILVER);
            this.detectPlatinum = request.canDetect(SeismicAnomalyType.PLATINUM);
            this.rockStoneTargets = SeismicRockStoneDeposits.detectableBlocks(request.detectableTypes());
            this.customTargets = SeismicCustomModules.detectableBlocks(request.detectableTypes(), request.modulePriorities());
            this.detectAnyOre = detectCoal
                || detectIron
                || detectCopper
                || detectGold
                || detectRedstone
                || detectLapis
                || detectEmerald
                || detectDiamond
                || detectZinc
                || detectCrimsite
                || detectAsurine
                || detectOchrum
                || detectVeridium
                || detectThorium
                || detectMagnetite
                || detectLead
                || detectNickel
                || detectLithium
                || detectBauxite
                || detectGalena
                || detectLignite
                || detectSulfur
                || detectWolframite
                || detectUranium
                || detectAutunite
                || detectNitrate
                || detectQuartz
                || detectAncientDebris
                || detectTin
                || detectOsmium
                || detectFluorite
                || detectCertusQuartz
                || detectInferium
                || detectProsperity
                || detectSoulium
                || detectArcaneCrystal
                || detectRune
                || detectStellarite
                || detectUraninite
                || detectDraconium
                || detectBlackQuartz
                || detectSilver
                || detectPlatinum
                || !rockStoneTargets.isEmpty();
            this.detectAnySolidTarget = detectAmethyst
                || detectSpawner
                || detectChest
                || detectAnyOre
                || detectCrudeOil
                || !customTargets.isEmpty();
            this.horizontalDxByIndex = new int[width * width];
            this.horizontalDzByIndex = new int[width * width];
            this.cellXByHorizontalIndex = new int[width * width];
            this.cellZByHorizontalIndex = new int[width * width];
            this.horizontalWeightBaseByIndex = new float[width * width];
            this.inRadiusByIndex = new boolean[width * width];
            this.depthFactorByIndex = new float[Math.max(1, request.depth())];
            precomputeScanCaches();
            this.index = 0;
        }

        private void precomputeScanCaches() {
            int radius = request.radius();
            int radiusSquared = radius * radius;
            float radiusInv = radius > 0 ? 1.0F / radius : 1.0F;
            int cellSize = 4;

            for (int horizontalIndex = 0; horizontalIndex < width * width; horizontalIndex++) {
                int dx = horizontalIndex % width - radius;
                int dz = horizontalIndex / width - radius;
                horizontalDxByIndex[horizontalIndex] = dx;
                horizontalDzByIndex[horizontalIndex] = dz;
                cellXByHorizontalIndex[horizontalIndex] = Math.floorDiv(dx + radius, cellSize);
                cellZByHorizontalIndex[horizontalIndex] = Math.floorDiv(dz + radius, cellSize);

                int distanceSq = dx * dx + dz * dz;
                boolean inRadius = distanceSq <= radiusSquared;
                inRadiusByIndex[horizontalIndex] = inRadius;
                if (!inRadius) {
                    horizontalWeightBaseByIndex[horizontalIndex] = 0.4F;
                    continue;
                }

                float distanceRatio = Mth.sqrt(distanceSq) * radiusInv;
                float distanceFactor = Mth.clamp(
                    1.0F - (float) Math.pow(distanceRatio, 1.9F),
                    0.0F,
                    1.0F
                );
                horizontalWeightBaseByIndex[horizontalIndex] = 0.4F + distanceFactor * 0.6F;
            }

            int depth = Math.max(1, request.depth());
            for (int depthIndex = 0; depthIndex < depthFactorByIndex.length; depthIndex++) {
                float depthRatio = (depthIndex + 1) / (float) depth;
                depthFactorByIndex[depthIndex] = 1.0F - depthRatio;
            }
        }

        private int process(int budget, long deadlineNanos) {
            ServerLevel level = request.level();
            BlockPos origin = request.origin();
            int originX = origin.getX();
            int originY = origin.getY();
            int originZ = origin.getZ();
            int radius = request.radius();
            int radiusSquared = radius * radius;
            int depth = request.depth();
            int widthSquared = width * width;
            float noiseStrength = request.noise();
            BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
            int consumed = 0;
            while (consumed < budget && index < totalCells) {
                if ((consumed & 63) == 0 && System.nanoTime() >= deadlineNanos) {
                    break;
                }

                int depthIndex = index / widthSquared;
                int horizontalIndex = index % widthSquared;
                int dx = horizontalDxByIndex[horizontalIndex];
                int dz = horizontalDzByIndex[horizontalIndex];
                index++;
                consumed++;

                if (!inRadiusByIndex[horizontalIndex]) {
                    continue;
                }

                scanPos.set(originX + dx, originY - (depthIndex + 1), originZ + dz);
                if (!level.isLoaded(scanPos)) {
                    continue;
                }

                BlockState state = level.getBlockState(scanPos);
                FluidState fluid = state.getFluidState();
                SeismicAnomalyType type;
                // Certus buds can be waterlogged; retain the crystal signal instead of reporting water.
                if (detectCertusQuartz && state.is(SeismicMineralTags.CERTUS_QUARTZ)) {
                    type = SeismicAnomalyType.CERTUS_QUARTZ;
                } else if (!fluid.isEmpty()) {
                    if (fluid.is(SeismicFluidTags.CRUDE_OIL)) {
                        if (!detectCrudeOil) {
                            continue;
                        }
                        type = SeismicAnomalyType.CRUDE_OIL;
                    } else if (fluid.is(FluidTags.LAVA)) {
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
                    SeismicAnomalyType depositType = rockStoneTargets.get(state.getBlock());
                    SeismicAnomalyType customType = customTargets.get(state.getBlock());
                    if (customType != null) {
                        type = customType;
                    } else if (depositType != null) {
                        type = depositType;
                    } else if (detectCrudeOil && state.is(SeismicMineralTags.CRUDE_OIL)) {
                        type = SeismicAnomalyType.CRUDE_OIL;
                    } else if (detectAmethyst && (state.is(Blocks.BUDDING_AMETHYST)
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
                        if (!detectAnyOre) {
                            continue;
                        }
                        if (detectQuartz && state.is(Blocks.NETHER_QUARTZ_ORE)) {
                            type = SeismicAnomalyType.QUARTZ;
                        } else if (detectAncientDebris && state.is(Blocks.ANCIENT_DEBRIS)) {
                            type = SeismicAnomalyType.ANCIENT_DEBRIS;
                        } else if (detectTin && state.is(SeismicMineralTags.TIN)) {
                            type = SeismicAnomalyType.TIN;
                        } else if (detectOsmium && state.is(SeismicMineralTags.OSMIUM)) {
                            type = SeismicAnomalyType.OSMIUM;
                        } else if (detectFluorite && state.is(SeismicMineralTags.FLUORITE)) {
                            type = SeismicAnomalyType.FLUORITE;
                        } else if (detectInferium && state.is(SeismicMineralTags.INFERIUM)) {
                            type = SeismicAnomalyType.INFERIUM;
                        } else if (detectProsperity && state.is(SeismicMineralTags.PROSPERITY)) {
                            type = SeismicAnomalyType.PROSPERITY;
                        } else if (detectSoulium && state.is(SeismicMineralTags.SOULIUM)) {
                            type = SeismicAnomalyType.SOULIUM;
                        } else if (detectArcaneCrystal && state.is(SeismicMineralTags.ARCANE_CRYSTAL)) {
                            type = SeismicAnomalyType.ARCANE_CRYSTAL;
                        } else if (detectRune && state.is(SeismicMineralTags.RUNE)) {
                            type = SeismicAnomalyType.RUNE;
                        } else if (detectStellarite && state.is(SeismicMineralTags.STELLARITE)) {
                            type = SeismicAnomalyType.STELLARITE;
                        } else if (detectUraninite && state.is(SeismicMineralTags.URANINITE)) {
                            type = SeismicAnomalyType.URANINITE;
                        } else if (detectDraconium && state.is(SeismicMineralTags.DRACONIUM)) {
                            type = SeismicAnomalyType.DRACONIUM;
                        } else if (detectBlackQuartz && state.is(SeismicMineralTags.BLACK_QUARTZ)) {
                            type = SeismicAnomalyType.BLACK_QUARTZ;
                        } else if (detectSilver && state.is(SeismicMineralTags.SILVER)) {
                            type = SeismicAnomalyType.SILVER;
                        } else if (detectPlatinum && state.is(SeismicMineralTags.PLATINUM)) {
                            type = SeismicAnomalyType.PLATINUM;
                        } else if (detectCoal && state.is(BlockTags.COAL_ORES)) {
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
                        } else if (detectCrimsite && state.is(SeismicMineralTags.CRIMSITE)) {
                            type = SeismicAnomalyType.CRIMSITE;
                        } else if (detectAsurine && state.is(SeismicMineralTags.ASURINE)) {
                            type = SeismicAnomalyType.ASURINE;
                        } else if (detectOchrum && state.is(SeismicMineralTags.OCHRUM)) {
                            type = SeismicAnomalyType.OCHRUM;
                        } else if (detectVeridium && state.is(SeismicMineralTags.VERIDIUM)) {
                            type = SeismicAnomalyType.VERIDIUM;
                        } else if (detectThorium && state.is(SeismicMineralTags.THORIUM)) {
                            type = SeismicAnomalyType.THORIUM;
                        } else if (detectMagnetite && state.is(SeismicMineralTags.MAGNETITE)) {
                            type = SeismicAnomalyType.MAGNETITE;
                        } else if (detectLead && state.is(SeismicMineralTags.LEAD)) {
                            type = SeismicAnomalyType.LEAD;
                        } else if (detectNickel && state.is(SeismicMineralTags.NICKEL)) {
                            type = SeismicAnomalyType.NICKEL;
                        } else if (detectLithium && state.is(SeismicMineralTags.LITHIUM)) {
                            type = SeismicAnomalyType.LITHIUM;
                        } else if (detectBauxite && state.is(SeismicMineralTags.BAUXITE)) {
                            type = SeismicAnomalyType.BAUXITE;
                        } else if (detectGalena && state.is(SeismicMineralTags.GALENA)) {
                            type = SeismicAnomalyType.GALENA;
                        } else if (detectLignite && state.is(SeismicMineralTags.LIGNITE)) {
                            type = SeismicAnomalyType.LIGNITE;
                        } else if (detectSulfur && state.is(SeismicMineralTags.SULFUR)) {
                            type = SeismicAnomalyType.SULFUR;
                        } else if (detectWolframite && state.is(SeismicMineralTags.WOLFRAMITE)) {
                            type = SeismicAnomalyType.WOLFRAMITE;
                        } else if (detectUranium && state.is(SeismicMineralTags.URANIUM)) {
                            type = SeismicAnomalyType.URANIUM;
                        } else if (detectAutunite && state.is(SeismicMineralTags.AUTUNITE)) {
                            type = SeismicAnomalyType.AUTUNITE;
                        } else if (detectNitrate && state.is(SeismicMineralTags.NITRATE)) {
                            type = SeismicAnomalyType.NITRATE;
                        } else {
                            continue;
                        }
                    }
                }
                trackExactHit(type, scanPos);

                float depthFactor = depthFactorByIndex[depthIndex];
                float noise = (random.nextFloat() - 0.5F) * noiseStrength;
                float weight = Mth.clamp(depthFactor * horizontalWeightBaseByIndex[horizontalIndex] + noise, 0.0F, 1.0F);
                if (weight <= 0.02F) {
                    continue;
                }

                int cellX = cellXByHorizontalIndex[horizontalIndex];
                int cellZ = cellZByHorizontalIndex[horizontalIndex];
                // Keep the type above the 28 coordinate bits without truncating it to four bits.
                long key = ((long) type.ordinal() << 28) | ((long) cellX << 14) | cellZ;
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
            return prioritizeAndLimitAnomalies(rawAnomalies, Config.ECHO_MERGE_DISTANCE.get(),
                Config.MAX_ECHOES.get(), request.modulePriorities());
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
            return switch (type.kind()) {
                case WATER, LAVA, COAL, COAL_DEPOSIT, IRON, IRON_DEPOSIT, COPPER, COPPER_DEPOSIT, GOLD, GOLD_DEPOSIT,
                    REDSTONE, REDSTONE_DEPOSIT, LAPIS, LAPIS_DEPOSIT, EMERALD, DIAMOND, ZINC, ZINC_DEPOSIT, AMETHYST,
                    CHEST, SPAWNER, CRIMSITE, ASURINE, OCHRUM, VERIDIUM, THORIUM, THORIUM_DEPOSIT, MAGNETITE, LEAD,
                    LEAD_DEPOSIT, NICKEL, NICKEL_DEPOSIT, LITHIUM, BAUXITE, GALENA, LIGNITE, SULFUR, CRUDE_OIL,
                    WOLFRAMITE, WOLFRAMITE_DEPOSIT, URANIUM, URANIUM_DEPOSIT, AUTUNITE, NITRATE, TIN, TIN_DEPOSIT,
                    SILVER, SILVER_DEPOSIT, PLATINUM, PLATINUM_DEPOSIT, OSMIUM, OSMIUM_DEPOSIT, QUARTZ, QUARTZ_DEPOSIT,
                    COBALT, COBALT_DEPOSIT, ANCIENT_DEBRIS, FLUORITE, CERTUS_QUARTZ,
                    INFERIUM, PROSPERITY, SOULIUM,
                    ARCANE_CRYSTAL, RUNE, STELLARITE, URANINITE, DRACONIUM, BLACK_QUARTZ -> true;
                case CUSTOM -> true;
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
