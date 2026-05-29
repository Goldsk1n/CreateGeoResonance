package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.goldskinmc.creategeoresonance.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SeismicProjectorBlockEntity extends KineticBlockEntity {
    private static final float TARGET_REQUIRED_SU = 512.0F;
    private static final String TAG_NODES = "Nodes";
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
    private static final String TAG_CLUSTER_BLOCKS = "Blocks";
    private static final String TAG_CLUSTER_BLOCK_X = "BlockX";
    private static final String TAG_CLUSTER_BLOCK_Y = "BlockY";
    private static final String TAG_CLUSTER_BLOCK_Z = "BlockZ";
    private static final int MAX_NODES = 2;
    private static final int PREVIEW_REQUIRED_NODES = 2;
    private static final int EXACT_REQUIRED_NODES = 2;
    private static final int MAX_PREVIEW_LINES = 5;

    private final List<NodeSnapshot> nodes = new ArrayList<>();
    private boolean triangulationDirty = true;
    private boolean exactVeinsDirty = true;
    private List<TriangulatedCandidate> cachedTriangulatedCandidates = List.of();
    private List<ExactVein> cachedExactVeins = List.of();

    public SeismicProjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean tryLoadSeismogram(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshotWithExactData(held);
        if (snapshot == null) {
            return false;
        }
        if (level == null) {
            return false;
        }
        String projectorDimension = level.dimension().location().toString();
        if (!projectorDimension.equals(snapshot.stationDimension())) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.dimension_mismatch")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!isWithinStationRange(snapshot.stationPos())) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.station_out_of_range",
                    Config.PROJECTOR_STATION_RANGE_CHUNKS.get())
                .withStyle(ChatFormatting.RED), true);
            return true;
        }

        if (containsStation(snapshot.stationPos(), snapshot.stationDimension())) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.node_duplicate")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!isFarEnoughFromLoadedStations(snapshot.stationPos(), snapshot.stationDimension())) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.node_too_close",
                    Config.PROJECTOR_MIN_NODE_DISTANCE_BLOCKS.get())
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (nodes.size() >= MAX_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.node_full")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }

        nodes.add(NodeSnapshot.fromSnapshot(snapshot));
        invalidateComputedCaches();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        syncClient();
        updatePoweredState();

        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.node_loaded",
                nodes.size(),
                MAX_NODES)
            .withStyle(ChatFormatting.GREEN), true);
        if (nodes.size() >= EXACT_REQUIRED_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.exact_ready")
                .withStyle(ChatFormatting.GOLD), true);
        } else if (nodes.size() >= PREVIEW_REQUIRED_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.ready")
                .withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    public List<TriangulatedCandidate> getTriangulatedCandidates() {
        return List.copyOf(triangulateCandidates());
    }

    public List<ExactVein> getConfirmedVeins() {
        if (!hasRequiredSpeed()) {
            return List.of();
        }
        return List.copyOf(confirmExactVeins());
    }

    public void sendStatus(Player player) {
        if (!hasRequiredSpeed()) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.no_rotation",
                    Config.PROJECTOR_MIN_SPEED.get())
                .withStyle(ChatFormatting.RED), true);
            return;
        }
        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.nodes",
                nodes.size(),
                MAX_NODES)
            .withStyle(ChatFormatting.GOLD), true);

        if (nodes.size() < PREVIEW_REQUIRED_NODES) {
            return;
        }

        List<TriangulatedCandidate> candidates = triangulateCandidates();
        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.matches",
                candidates.size())
            .withStyle(ChatFormatting.AQUA), true);

        int lines = Math.min(MAX_PREVIEW_LINES, candidates.size());
        for (int i = 0; i < lines; i++) {
            TriangulatedCandidate candidate = candidates.get(i);
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.match_line",
                    Component.translatable(signalNameKey(candidate.type())),
                    candidate.worldX(),
                    candidate.worldZ(),
                    candidate.approxY(),
                    candidate.error())
            .withStyle(ChatFormatting.GRAY), false);
        }

        if (nodes.size() < EXACT_REQUIRED_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.need_third_node")
                .withStyle(ChatFormatting.DARK_AQUA), false);
            return;
        }

        List<ExactVein> exactVeins = confirmExactVeins();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.exact_matches", exactVeins.size())
            .withStyle(ChatFormatting.GOLD), false);
    }

    @Override
    public AABB getRenderBoundingBox() {
        List<ExactVein> exactVeins = confirmExactVeins();
        if (!exactVeins.isEmpty()) {
            int minX = worldPosition.getX();
            int minY = worldPosition.getY();
            int minZ = worldPosition.getZ();
            int maxX = worldPosition.getX() + 1;
            int maxY = worldPosition.getY() + 1;
            int maxZ = worldPosition.getZ() + 1;
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

        List<TriangulatedCandidate> candidates = triangulateCandidates();
        if (candidates.isEmpty()) {
            return new AABB(worldPosition).inflate(1.0D);
        }

        int minX = worldPosition.getX();
        int minY = worldPosition.getY();
        int minZ = worldPosition.getZ();
        int maxX = worldPosition.getX() + 1;
        int maxY = worldPosition.getY() + 1;
        int maxZ = worldPosition.getZ() + 1;
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

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        ListTag list = new ListTag();
        for (NodeSnapshot node : nodes) {
            CompoundTag entry = new CompoundTag();
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
                ListTag blockList = new ListTag();
                for (BlockPos blockPos : cluster.blocks()) {
                    CompoundTag blockTag = new CompoundTag();
                    blockTag.putInt(TAG_CLUSTER_BLOCK_X, blockPos.getX());
                    blockTag.putInt(TAG_CLUSTER_BLOCK_Y, blockPos.getY());
                    blockTag.putInt(TAG_CLUSTER_BLOCK_Z, blockPos.getZ());
                    blockList.add(blockTag);
                }
                clusterTag.put(TAG_CLUSTER_BLOCKS, blockList);
                exactClustersTag.add(clusterTag);
            }
            entry.put(TAG_EXACT_CLUSTERS, exactClustersTag);
            list.add(entry);
        }
        tag.put(TAG_NODES, list);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
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
                ListTag blockList = clusterTag.getList(TAG_CLUSTER_BLOCKS, Tag.TAG_COMPOUND);
                List<BlockPos> blocks = new ArrayList<>(blockList.size());
                for (Tag blockRaw : blockList) {
                    CompoundTag blockTag = (CompoundTag) blockRaw;
                    blocks.add(new BlockPos(
                        blockTag.getInt(TAG_CLUSTER_BLOCK_X),
                        blockTag.getInt(TAG_CLUSTER_BLOCK_Y),
                        blockTag.getInt(TAG_CLUSTER_BLOCK_Z)
                    ));
                }
                if (!blocks.isEmpty()) {
                    exactClusters.add(new SeismogramMapService.ExactCluster(type, List.copyOf(blocks)));
                }
            }
            nodes.add(new NodeSnapshot(center, stationPos, stationDimension, List.copyOf(signals), List.copyOf(exactClusters)));
        }
        invalidateComputedCaches();
        if (!clientPacket) {
            updatePoweredState();
        }
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

    private boolean isWithinStationRange(BlockPos stationPos) {
        int maxChunks = Math.max(0, Config.PROJECTOR_STATION_RANGE_CHUNKS.get());
        int maxBlocks = maxChunks * 16;
        long maxDistanceSquared = (long) maxBlocks * maxBlocks;
        long dx = (long) stationPos.getX() - worldPosition.getX();
        long dz = (long) stationPos.getZ() - worldPosition.getZ();
        long distanceSquared = dx * dx + dz * dz;
        return distanceSquared <= maxDistanceSquared;
    }

    private List<TriangulatedCandidate> triangulateCandidates() {
        if (!triangulationDirty) {
            return cachedTriangulatedCandidates;
        }
        cachedTriangulatedCandidates = List.copyOf(computeTriangulateCandidates());
        triangulationDirty = false;
        return cachedTriangulatedCandidates;
    }

    private List<TriangulatedCandidate> computeTriangulateCandidates() {
        if (nodes.size() < PREVIEW_REQUIRED_NODES) {
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

    private List<ExactVein> confirmExactVeins() {
        if (!exactVeinsDirty) {
            return cachedExactVeins;
        }
        cachedExactVeins = List.copyOf(computeExactVeins());
        exactVeinsDirty = false;
        return cachedExactVeins;
    }

    private List<ExactVein> computeExactVeins() {
        if (nodes.size() < EXACT_REQUIRED_NODES) {
            return List.of();
        }

        NodeSnapshot first = nodes.get(0);
        NodeSnapshot second = nodes.get(1);
        List<ExactVein> confirmed = new ArrayList<>();
        Set<Long> dedupe = new HashSet<>();

        for (SeismogramMapService.ExactCluster a : first.exactClusters()) {
            for (SeismogramMapService.ExactCluster b : second.exactClusters()) {
                if (a.type() != b.type() || !clustersIntersect(a.blocks(), b.blocks())) {
                    continue;
                }
                List<BlockPos> merged = mergeClusterBlocks(a.blocks(), b.blocks());
                if (merged.isEmpty()) {
                    continue;
                }
                long signature = clusterSignature(a.type(), merged);
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

    private static long clusterSignature(SeismicAnomalyType type, List<BlockPos> blocks) {
        BlockPos anchor = blocks.get(0);
        for (BlockPos blockPos : blocks) {
            if (blockPos.getY() < anchor.getY()
                || (blockPos.getY() == anchor.getY() && blockPos.getX() < anchor.getX())
                || (blockPos.getY() == anchor.getY() && blockPos.getX() == anchor.getX() && blockPos.getZ() < anchor.getZ())) {
                anchor = blockPos;
            }
        }
        long packed = BlockPos.asLong(anchor.getX(), anchor.getY(), anchor.getZ());
        return packed ^ ((long) type.ordinal() << 58);
    }

    private static String signalNameKey(SeismicAnomalyType type) {
        return switch (type) {
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
            case AMETHYST -> "item.creategeoresonance.seismogram.signal.amethyst";
            case CHEST -> "item.creategeoresonance.seismogram.signal.chest";
            case SPAWNER -> "item.creategeoresonance.seismogram.signal.spawner";
            case SOLID -> "item.creategeoresonance.seismogram.signal.solid";
        };
    }

    private static SeismicAnomalyType parseType(String raw) {
        try {
            return SeismicAnomalyType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void invalidateComputedCaches() {
        triangulationDirty = true;
        exactVeinsDirty = true;
        cachedTriangulatedCandidates = List.of();
        cachedExactVeins = List.of();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        updatePoweredState();
    }

    public boolean hasRequiredSpeed() {
        return Math.abs(getSpeed()) >= Config.PROJECTOR_MIN_SPEED.get();
    }

    public int getLoadedNodeCount() {
        return nodes.size();
    }

    @Nullable
    public BlockPos getSingleLoadedStationPos() {
        if (nodes.size() != 1) {
            return null;
        }
        return nodes.get(0).stationPos().immutable();
    }

    private boolean isProjecting() {
        return hasRequiredSpeed() && nodes.size() >= PREVIEW_REQUIRED_NODES;
    }

    private void updatePoweredState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState current = getBlockState();
        if (!current.hasProperty(SeismicProjectorBlock.ACTIVE)) {
            return;
        }
        boolean shouldBePowered = isProjecting();
        if (current.getValue(SeismicProjectorBlock.ACTIVE) == shouldBePowered) {
            return;
        }
        level.setBlock(worldPosition, current.setValue(SeismicProjectorBlock.ACTIVE, shouldBePowered), Block.UPDATE_ALL);
        setChanged();
    }

    @Override
    public float calculateStressApplied() {
        float minOperationalSpeed = Math.max(1.0F, Config.PROJECTOR_MIN_SPEED.get());
        float absSpeed = Math.max(minOperationalSpeed, Math.abs(getSpeed()));
        lastStressApplied = TARGET_REQUIRED_SU / absSpeed;
        return lastStressApplied;
    }

    private record NodeSnapshot(BlockPos center, BlockPos stationPos, String stationDimension,
                                List<SeismogramMapService.MapSignal> signals,
                                List<SeismogramMapService.ExactCluster> exactClusters) {
        private static NodeSnapshot fromSnapshot(SeismogramMapService.MapSnapshot snapshot) {
            return new NodeSnapshot(
                new BlockPos(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()),
                snapshot.stationPos().immutable(),
                snapshot.stationDimension(),
                List.copyOf(snapshot.signals()),
                List.copyOf(snapshot.exactClusters())
            );
        }
    }

    public record TriangulatedCandidate(SeismicAnomalyType type, int worldX, int worldZ, int approxY, int error) {
    }

    public record ExactVein(SeismicAnomalyType type, List<BlockPos> blocks) {
    }
}
