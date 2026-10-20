package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SeismicProjectorData {
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_SEISMOGRAM = "SeismogramItem";
    private static final String TAG_CENTER_X = "CenterX";
    private static final String TAG_CENTER_Y = "CenterY";
    private static final String TAG_CENTER_Z = "CenterZ";
    private static final String TAG_STATION_X = "StationX";
    private static final String TAG_STATION_Y = "StationY";
    private static final String TAG_STATION_Z = "StationZ";
    private static final String TAG_STATION_DIMENSION = "StationDimension";
    private static final String TAG_SIGNALS = "Signals";
    private static final String TAG_SIGNAL_TYPE = "Type";
    private static final String TAG_SIGNAL_X = "X";
    private static final String TAG_SIGNAL_Z = "Z";
    private static final String TAG_SIGNAL_Y = "Y";
    private static final String TAG_EXACT_CLUSTERS = "ExactClusters";
    private static final String TAG_CLUSTER_TYPE = "ClusterType";

    public static final int MAX_NODES = 2;
    public static final int PREVIEW_REQUIRED_NODES = 2;
    public static final int EXACT_REQUIRED_NODES = 2;
    public static final int MAX_PREVIEW_LINES = 5;

    private final List<NodeSnapshot> nodes = new ArrayList<>();
    private boolean triangulationDirty = true;
    private boolean exactVeinsDirty = true;
    private List<TriangulatedCandidate> cachedTriangulatedCandidates = List.of();
    private List<ExactVein> cachedExactVeins = List.of();
    @Nullable
    private AABB cachedRenderBounds;
    @Nullable
    private BlockPos cachedRenderOrigin;

    public LoadResult tryLoadSnapshot(String projectorDimension, BlockPos projectorPos,
                                      SeismogramMapService.MapSnapshot snapshot) {
        return tryLoadSnapshot(projectorDimension, projectorPos, snapshot, null);
    }

    public LoadResult tryLoadSnapshot(String projectorDimension, BlockPos projectorPos,
                                      SeismogramMapService.MapSnapshot snapshot, @Nullable Tag seismogram) {
        if (!projectorDimension.equals(snapshot.stationDimension())) {
            return new LoadResult(LoadStatus.DIMENSION_MISMATCH, nodes.size());
        }
        if (!isWithinStationRange(projectorPos, snapshot.stationPos())) {
            return new LoadResult(LoadStatus.STATION_OUT_OF_RANGE, nodes.size());
        }
        if (containsStation(snapshot.stationPos(), snapshot.stationDimension())) {
            return new LoadResult(LoadStatus.DUPLICATE, nodes.size());
        }
        if (!isFarEnoughFromLoadedStations(snapshot.stationPos(), snapshot.stationDimension())) {
            return new LoadResult(LoadStatus.TOO_CLOSE, nodes.size());
        }
        if (nodes.size() >= MAX_NODES) {
            return new LoadResult(LoadStatus.FULL, nodes.size());
        }

        nodes.add(NodeSnapshot.fromSnapshot(snapshot, seismogram));
        invalidateComputedCaches();
        return new LoadResult(LoadStatus.SUCCESS, nodes.size());
    }

    public ItemStack takeLastSeismogram(ServerLevel level) {
        if (nodes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        NodeSnapshot node = nodes.get(nodes.size() - 1);
        ItemStack stack;
        if (node.seismogram() == null) {
            // Older projectors saved only survey data, not the original map item.
            stack = SeismogramMapService.restoreMap(level, node.toSnapshot());
        } else {
            stack = ItemStack.parse(level.registryAccess(), node.seismogram())
                .orElseThrow(() -> new IllegalStateException("Invalid stored projector seismogram"));
            if (stack.getCount() != 1 || !SeismogramMapService.isSeismogramStack(stack)) {
                throw new IllegalStateException("Invalid stored projector seismogram");
            }
        }
        SeismogramMapService.compactLegacyMap(stack);
        nodes.remove(nodes.size() - 1);
        invalidateComputedCaches();
        return stack;
    }

    public void writeToTag(CompoundTag tag) {
        writeToTag(tag, false);
    }

    public void writeToTag(CompoundTag tag, boolean clientPacket) {
        if (tag == null) {
            return;
        }
        ListTag list = new ListTag();
        for (NodeSnapshot node : nodes) {
            CompoundTag entry = new CompoundTag();
            if (!clientPacket && node.seismogram() != null) {
                entry.put(TAG_SEISMOGRAM, node.seismogram().copy());
            }
            entry.putInt(TAG_CENTER_X, node.center().getX());
            entry.putInt(TAG_CENTER_Y, node.center().getY());
            entry.putInt(TAG_CENTER_Z, node.center().getZ());
            entry.putInt(TAG_STATION_X, node.stationPos().getX());
            entry.putInt(TAG_STATION_Y, node.stationPos().getY());
            entry.putInt(TAG_STATION_Z, node.stationPos().getZ());
            entry.putString(TAG_STATION_DIMENSION, node.stationDimension());

            ListTag signalsTag = new ListTag();
            for (SeismogramMapService.MapSignal signal : node.signals()) {
                CompoundTag signalTag = new CompoundTag();
                signalTag.putString(TAG_SIGNAL_TYPE, signal.type().name());
                signalTag.putInt(TAG_SIGNAL_X, signal.worldX());
                signalTag.putInt(TAG_SIGNAL_Z, signal.worldZ());
                signalTag.putInt(TAG_SIGNAL_Y, signal.approxY());
                signalsTag.add(signalTag);
            }
            entry.put(TAG_SIGNALS, signalsTag);

            ListTag exactClustersTag = new ListTag();
            for (SeismogramMapService.ExactCluster cluster : node.exactClusters()) {
                CompoundTag clusterTag = new CompoundTag();
                clusterTag.putString(TAG_CLUSTER_TYPE, cluster.type().name());
                SeismicSurveyNbt.writeBlocks(clusterTag, cluster.blocks());
                exactClustersTag.add(clusterTag);
            }
            entry.put(TAG_EXACT_CLUSTERS, exactClustersTag);
            list.add(entry);
        }
        tag.put(TAG_NODES, list);
    }

    public void readFromTag(CompoundTag tag) {
        if (tag == null) {
            nodes.clear();
            invalidateComputedCaches();
            return;
        }
        nodes.clear();
        ListTag list = tag.getList(TAG_NODES, Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            BlockPos center = new BlockPos(entry.getInt(TAG_CENTER_X), entry.getInt(TAG_CENTER_Y), entry.getInt(TAG_CENTER_Z));
            BlockPos stationPos = new BlockPos(entry.getInt(TAG_STATION_X), entry.getInt(TAG_STATION_Y), entry.getInt(TAG_STATION_Z));
            String stationDimension = entry.getString(TAG_STATION_DIMENSION);

            List<SeismogramMapService.MapSignal> signals = new ArrayList<>();
            ListTag signalsTag = entry.getList(TAG_SIGNALS, Tag.TAG_COMPOUND);
            for (Tag signalRaw : signalsTag) {
                CompoundTag signalTag = (CompoundTag) signalRaw;
                SeismicAnomalyType type = parseType(signalTag.getString(TAG_SIGNAL_TYPE));
                if (type == null) {
                    continue;
                }
                signals.add(new SeismogramMapService.MapSignal(
                    type,
                    signalTag.getInt(TAG_SIGNAL_X),
                    signalTag.getInt(TAG_SIGNAL_Z),
                    signalTag.getInt(TAG_SIGNAL_Y)
                ));
            }

            List<SeismogramMapService.ExactCluster> exactClusters = new ArrayList<>();
            ListTag exactClustersTag = entry.getList(TAG_EXACT_CLUSTERS, Tag.TAG_COMPOUND);
            for (Tag clusterRaw : exactClustersTag) {
                CompoundTag clusterTag = (CompoundTag) clusterRaw;
                SeismicAnomalyType type = parseType(clusterTag.getString(TAG_CLUSTER_TYPE));
                if (type == null) {
                    continue;
                }
                List<BlockPos> blocks = SeismicSurveyNbt.readBlocks(clusterTag, "BlockX", "BlockY", "BlockZ");
                if (!blocks.isEmpty()) {
                    exactClusters.add(new SeismogramMapService.ExactCluster(type, List.copyOf(blocks)));
                }
            }

            Tag seismogram = entry.get(TAG_SEISMOGRAM);
            nodes.add(new NodeSnapshot(center, stationPos, stationDimension, List.copyOf(signals),
                List.copyOf(exactClusters), seismogram != null ? SeismogramMapService.compactSerializedMap(seismogram) : null));
        }
        invalidateComputedCaches();
    }

    public int getLoadedNodeCount() {
        return nodes.size();
    }

    public boolean hasPreviewNodes() {
        return nodes.size() >= PREVIEW_REQUIRED_NODES;
    }

    public boolean hasExactNodes() {
        return nodes.size() >= EXACT_REQUIRED_NODES;
    }

    public boolean canProjectFrom(BlockPos projectorPos, boolean powered) {
        return powered && hasPreviewNodes() && areLoadedStationsWithinRange(projectorPos);
    }

    public boolean areLoadedStationsWithinRange(BlockPos projectorPos) {
        for (NodeSnapshot node : nodes) {
            if (!isWithinStationRange(projectorPos, node.stationPos())) {
                return false;
            }
        }
        return true;
    }

    public List<TriangulatedCandidate> getTriangulatedCandidates() {
        if (!triangulationDirty) {
            return cachedTriangulatedCandidates;
        }
        cachedTriangulatedCandidates = List.copyOf(computeTriangulatedCandidates());
        triangulationDirty = false;
        return cachedTriangulatedCandidates;
    }

    public List<ExactVein> getConfirmedVeins() {
        if (!exactVeinsDirty) {
            return cachedExactVeins;
        }
        cachedExactVeins = List.copyOf(computeExactVeins());
        exactVeinsDirty = false;
        return cachedExactVeins;
    }

    public AABB getRenderBoundingBox(BlockPos projectorPos, boolean projecting) {
        if (!projecting) {
            return new AABB(projectorPos).inflate(1.0D);
        }
        if (cachedRenderBounds == null || !projectorPos.equals(cachedRenderOrigin)) {
            cachedRenderBounds = computeRenderBoundingBox(projectorPos);
            cachedRenderOrigin = projectorPos.immutable();
        }
        return cachedRenderBounds;
    }

    public boolean isWithinProjectionRenderDistance(BlockPos projectorPos, boolean projecting, Vec3 camera, int viewDistance) {
        return projecting && getRenderBoundingBox(projectorPos, true).distanceToSqr(camera)
            < (double) viewDistance * viewDistance;
    }

    private AABB computeRenderBoundingBox(BlockPos projectorPos) {
        List<ExactVein> exactVeins = getConfirmedVeins();
        if (!exactVeins.isEmpty()) {
            int minX = projectorPos.getX();
            int minY = projectorPos.getY();
            int minZ = projectorPos.getZ();
            int maxX = projectorPos.getX() + 1;
            int maxY = projectorPos.getY() + 1;
            int maxZ = projectorPos.getZ() + 1;
            for (ExactVein vein : exactVeins) {
                for (BlockPos blockPos : vein.blocks()) {
                    minX = Math.min(minX, blockPos.getX());
                    minY = Math.min(minY, blockPos.getY());
                    minZ = Math.min(minZ, blockPos.getZ());
                    maxX = Math.max(maxX, blockPos.getX() + 1);
                    maxY = Math.max(maxY, blockPos.getY() + 1);
                    maxZ = Math.max(maxZ, blockPos.getZ() + 1);
                }
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        List<TriangulatedCandidate> candidates = getTriangulatedCandidates();
        if (candidates.isEmpty()) {
            return new AABB(projectorPos).inflate(1.0D);
        }

        int minX = projectorPos.getX();
        int minY = projectorPos.getY();
        int minZ = projectorPos.getZ();
        int maxX = projectorPos.getX() + 1;
        int maxY = projectorPos.getY() + 1;
        int maxZ = projectorPos.getZ() + 1;
        for (TriangulatedCandidate candidate : candidates) {
            minX = Math.min(minX, candidate.worldX() - 1);
            minY = Math.min(minY, candidate.approxY() - 1);
            minZ = Math.min(minZ, candidate.worldZ() - 1);
            maxX = Math.max(maxX, candidate.worldX() + 2);
            maxY = Math.max(maxY, candidate.approxY() + 2);
            maxZ = Math.max(maxZ, candidate.worldZ() + 2);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    public BlockPos getSingleLoadedStationPos() {
        if (nodes.size() != 1) {
            return null;
        }
        return nodes.get(0).stationPos().immutable();
    }

    public static net.minecraft.network.chat.Component signalName(SeismicAnomalyType type) {
        return type.isCustom() ? SeismicCustomModules.signalName(type)
            : net.minecraft.network.chat.Component.translatable(signalNameKey(type));
    }

    public static String signalNameKey(SeismicAnomalyType type) {
        return switch (type.kind()) {
            case CAVE -> "item.creategeoresonance.seismogram.signal.cave";
            case WATER -> "item.creategeoresonance.seismogram.signal.water";
            case LAVA -> "item.creategeoresonance.seismogram.signal.lava";
            case COAL -> "item.creategeoresonance.seismogram.signal.coal";
            case IRON -> "item.creategeoresonance.seismogram.signal.iron";
            case COPPER -> "item.creategeoresonance.seismogram.signal.copper";
            case GOLD -> "item.creategeoresonance.seismogram.signal.gold";
            case REDSTONE -> "item.creategeoresonance.seismogram.signal.redstone";
            case LAPIS -> "item.creategeoresonance.seismogram.signal.lapis";
            case EMERALD -> "item.creategeoresonance.seismogram.signal.emerald";
            case DIAMOND -> "item.creategeoresonance.seismogram.signal.diamond";
            case ZINC -> "item.creategeoresonance.seismogram.signal.zinc";
            case CRIMSITE -> "item.creategeoresonance.seismogram.signal.crimsite";
            case ASURINE -> "item.creategeoresonance.seismogram.signal.asurine";
            case OCHRUM -> "item.creategeoresonance.seismogram.signal.ochrum";
            case VERIDIUM -> "item.creategeoresonance.seismogram.signal.veridium";
            case THORIUM -> "item.creategeoresonance.seismogram.signal.thorium";
            case MAGNETITE -> "item.creategeoresonance.seismogram.signal.magnetite";
            case LEAD -> "item.creategeoresonance.seismogram.signal.lead";
            case NICKEL -> "item.creategeoresonance.seismogram.signal.nickel";
            case LITHIUM -> "item.creategeoresonance.seismogram.signal.lithium";
            case BAUXITE -> "item.creategeoresonance.seismogram.signal.bauxite";
            case GALENA -> "item.creategeoresonance.seismogram.signal.galena";
            case LIGNITE -> "item.creategeoresonance.seismogram.signal.lignite";
            case SULFUR -> "item.creategeoresonance.seismogram.signal.sulfur";
            case CRUDE_OIL -> "item.creategeoresonance.seismogram.signal.crude_oil";
            case WOLFRAMITE -> "item.creategeoresonance.seismogram.signal.wolframite";
            case URANIUM -> "item.creategeoresonance.seismogram.signal.uranium";
            case AUTUNITE -> "item.creategeoresonance.seismogram.signal.autunite";
            case NITRATE -> "item.creategeoresonance.seismogram.signal.nitrate";
            case TIN -> "item.creategeoresonance.seismogram.signal.tin";
            case SILVER -> "item.creategeoresonance.seismogram.signal.silver";
            case PLATINUM -> "item.creategeoresonance.seismogram.signal.platinum";
            case OSMIUM -> "item.creategeoresonance.seismogram.signal.osmium";
            case QUARTZ -> "item.creategeoresonance.seismogram.signal.quartz";
            case ANCIENT_DEBRIS -> "item.creategeoresonance.seismogram.signal.ancient_debris";
            case FLUORITE -> "item.creategeoresonance.seismogram.signal.fluorite";
            case CERTUS_QUARTZ -> "item.creategeoresonance.seismogram.signal.certus_quartz";
            case INFERIUM -> "item.creategeoresonance.seismogram.signal.inferium";
            case PROSPERITY -> "item.creategeoresonance.seismogram.signal.prosperity";
            case SOULIUM -> "item.creategeoresonance.seismogram.signal.soulium";
            case ARCANE_CRYSTAL -> "item.creategeoresonance.seismogram.signal.arcane_crystal";
            case RUNE -> "item.creategeoresonance.seismogram.signal.rune";
            case STELLARITE -> "item.creategeoresonance.seismogram.signal.stellarite";
            case URANINITE -> "item.creategeoresonance.seismogram.signal.uraninite";
            case DRACONIUM -> "item.creategeoresonance.seismogram.signal.draconium";
            case BLACK_QUARTZ -> "item.creategeoresonance.seismogram.signal.black_quartz";
            case COBALT -> "item.creategeoresonance.seismogram.signal.cobalt";
            case COAL_DEPOSIT -> "item.creategeoresonance.seismogram.signal.coal_deposit";
            case IRON_DEPOSIT -> "item.creategeoresonance.seismogram.signal.iron_deposit";
            case COPPER_DEPOSIT -> "item.creategeoresonance.seismogram.signal.copper_deposit";
            case GOLD_DEPOSIT -> "item.creategeoresonance.seismogram.signal.gold_deposit";
            case REDSTONE_DEPOSIT -> "item.creategeoresonance.seismogram.signal.redstone_deposit";
            case LAPIS_DEPOSIT -> "item.creategeoresonance.seismogram.signal.lapis_deposit";
            case ZINC_DEPOSIT -> "item.creategeoresonance.seismogram.signal.zinc_deposit";
            case LEAD_DEPOSIT -> "item.creategeoresonance.seismogram.signal.lead_deposit";
            case NICKEL_DEPOSIT -> "item.creategeoresonance.seismogram.signal.nickel_deposit";
            case URANIUM_DEPOSIT -> "item.creategeoresonance.seismogram.signal.uranium_deposit";
            case THORIUM_DEPOSIT -> "item.creategeoresonance.seismogram.signal.thorium_deposit";
            case WOLFRAMITE_DEPOSIT -> "item.creategeoresonance.seismogram.signal.wolframite_deposit";
            case TIN_DEPOSIT -> "item.creategeoresonance.seismogram.signal.tin_deposit";
            case SILVER_DEPOSIT -> "item.creategeoresonance.seismogram.signal.silver_deposit";
            case PLATINUM_DEPOSIT -> "item.creategeoresonance.seismogram.signal.platinum_deposit";
            case OSMIUM_DEPOSIT -> "item.creategeoresonance.seismogram.signal.osmium_deposit";
            case QUARTZ_DEPOSIT -> "item.creategeoresonance.seismogram.signal.quartz_deposit";
            case COBALT_DEPOSIT -> "item.creategeoresonance.seismogram.signal.cobalt_deposit";
            case AMETHYST -> "item.creategeoresonance.seismogram.signal.amethyst";
            case CHEST -> "item.creategeoresonance.seismogram.signal.chest";
            case SPAWNER -> "item.creategeoresonance.seismogram.signal.spawner";
            case SOLID -> "item.creategeoresonance.seismogram.signal.solid";
            case CUSTOM -> type.name();
        };
    }

    private boolean containsStation(BlockPos stationPos, String stationDimension) {
        for (NodeSnapshot node : nodes) {
            if (node.stationPos().equals(stationPos) && node.stationDimension().equals(stationDimension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFarEnoughFromLoadedStations(BlockPos stationPos, String stationDimension) {
        int minDistance = Config.PROJECTOR_MIN_NODE_DISTANCE_BLOCKS.get();
        int minDistanceSquared = minDistance * minDistance;
        for (NodeSnapshot node : nodes) {
            if (!node.stationDimension().equals(stationDimension)) {
                continue;
            }
            if (node.stationPos().distSqr(stationPos) < minDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWithinStationRange(BlockPos projectorPos, BlockPos stationPos) {
        int maxChunks = Math.max(0, Config.PROJECTOR_STATION_RANGE_CHUNKS.get());
        int maxBlocks = maxChunks * 16;
        long maxDistanceSquared = (long) maxBlocks * maxBlocks;
        long dx = (long) stationPos.getX() - projectorPos.getX();
        long dz = (long) stationPos.getZ() - projectorPos.getZ();
        long distanceSquared = dx * dx + dz * dz;
        return distanceSquared <= maxDistanceSquared;
    }

    private List<TriangulatedCandidate> computeTriangulatedCandidates() {
        if (!hasPreviewNodes()) {
            return List.of();
        }

        NodeSnapshot first = nodes.get(0);
        NodeSnapshot second = nodes.get(1);
        List<TriangulatedCandidate> candidates = new ArrayList<>();
        Set<Integer> matchedSecondIndices = new HashSet<>();
        int matchRadius = Config.PROJECTOR_MATCH_RADIUS_BLOCKS.get();
        int maxDistanceSq = matchRadius * matchRadius;

        for (SeismogramMapService.MapSignal left : first.signals()) {
            int bestIndex = -1;
            int bestDistanceSq = Integer.MAX_VALUE;

            for (int i = 0; i < second.signals().size(); i++) {
                if (matchedSecondIndices.contains(i)) {
                    continue;
                }

                SeismogramMapService.MapSignal right = second.signals().get(i);
                if (right.type() != left.type()) {
                    continue;
                }

                int dx = left.worldX() - right.worldX();
                int dz = left.worldZ() - right.worldZ();
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > maxDistanceSq || distanceSq >= bestDistanceSq) {
                    continue;
                }
                bestDistanceSq = distanceSq;
                bestIndex = i;
            }

            if (bestIndex < 0) {
                continue;
            }

            matchedSecondIndices.add(bestIndex);
            SeismogramMapService.MapSignal right = second.signals().get(bestIndex);
            int worldX = Mth.floor((left.worldX() + right.worldX()) / 2.0D);
            int worldZ = Mth.floor((left.worldZ() + right.worldZ()) / 2.0D);
            int approxY = Mth.floor((left.approxY() + right.approxY()) / 2.0D);
            int error = Mth.floor(Math.sqrt(bestDistanceSq));
            candidates.add(new TriangulatedCandidate(left.type(), worldX, worldZ, approxY, error));
        }

        candidates.sort(Comparator.comparingInt(TriangulatedCandidate::error));
        return candidates;
    }

    private List<ExactVein> computeExactVeins() {
        if (!hasExactNodes()) {
            return List.of();
        }

        NodeSnapshot first = nodes.get(0);
        NodeSnapshot second = nodes.get(1);
        List<ExactVein> confirmed = new ArrayList<>();
        Set<ClusterKey> dedupe = new HashSet<>();

        for (SeismogramMapService.ExactCluster a : first.exactClusters()) {
            for (SeismogramMapService.ExactCluster b : second.exactClusters()) {
                if (a.type() != b.type() || !clustersIntersect(a.blocks(), b.blocks())) {
                    continue;
                }
                List<BlockPos> merged = mergeClusterBlocks(a.blocks(), b.blocks());
                if (merged.isEmpty()) {
                    continue;
                }
                ClusterKey signature = clusterSignature(a.type(), merged);
                if (dedupe.add(signature)) {
                    confirmed.add(new ExactVein(a.type(), merged));
                }
            }
        }

        return confirmed;
    }

    private static boolean clustersIntersect(List<BlockPos> left, List<BlockPos> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        Set<BlockPos> smaller = left.size() <= right.size() ? new HashSet<>(left) : new HashSet<>(right);
        List<BlockPos> larger = left.size() <= right.size() ? right : left;
        for (BlockPos pos : larger) {
            if (smaller.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static List<BlockPos> mergeClusterBlocks(List<BlockPos> first, List<BlockPos> second) {
        Set<BlockPos> merged = new LinkedHashSet<>(first.size() + second.size());
        merged.addAll(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    private static ClusterKey clusterSignature(SeismicAnomalyType type, List<BlockPos> blocks) {
        BlockPos anchor = blocks.get(0);
        for (BlockPos blockPos : blocks) {
            if (blockPos.getY() < anchor.getY()
                || (blockPos.getY() == anchor.getY() && blockPos.getX() < anchor.getX())
                || (blockPos.getY() == anchor.getY() && blockPos.getX() == anchor.getX() && blockPos.getZ() < anchor.getZ())) {
                anchor = blockPos;
            }
        }
        return new ClusterKey(type, anchor.immutable());
    }

    private record ClusterKey(SeismicAnomalyType type, BlockPos anchor) {
    }

    private static SeismicAnomalyType parseType(String raw) {
        try {
            return SeismicAnomalyType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void invalidateComputedCaches() {
        triangulationDirty = true;
        exactVeinsDirty = true;
        cachedTriangulatedCandidates = List.of();
        cachedExactVeins = List.of();
        cachedRenderBounds = null;
        cachedRenderOrigin = null;
    }

    public enum LoadStatus {
        SUCCESS,
        DIMENSION_MISMATCH,
        STATION_OUT_OF_RANGE,
        DUPLICATE,
        TOO_CLOSE,
        FULL
    }

    public record LoadResult(LoadStatus status, int loadedNodeCount) {
        public boolean success() {
            return status == LoadStatus.SUCCESS;
        }
    }

    private record NodeSnapshot(BlockPos center, BlockPos stationPos, String stationDimension,
                                List<SeismogramMapService.MapSignal> signals,
                                List<SeismogramMapService.ExactCluster> exactClusters, @Nullable Tag seismogram) {
        private SeismogramMapService.MapSnapshot toSnapshot() {
            return new SeismogramMapService.MapSnapshot(center.getX(), center.getY(), center.getZ(),
                stationPos, stationDimension, signals, exactClusters);
        }

        private static NodeSnapshot fromSnapshot(SeismogramMapService.MapSnapshot snapshot, @Nullable Tag seismogram) {
            return new NodeSnapshot(
                new BlockPos(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()),
                snapshot.stationPos().immutable(),
                snapshot.stationDimension(),
                List.copyOf(snapshot.signals()),
                List.copyOf(snapshot.exactClusters()),
                seismogram != null ? SeismogramMapService.compactSerializedMap(seismogram) : null
            );
        }
    }

    public record TriangulatedCandidate(SeismicAnomalyType type, int worldX, int worldZ, int approxY, int error) {
    }

    public record ExactVein(SeismicAnomalyType type, List<BlockPos> blocks) {
    }
}
