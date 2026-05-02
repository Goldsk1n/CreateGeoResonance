package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeismicProjectorBlockEntity extends BlockEntity {
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_CENTER_X = "CenterX";
    private static final String TAG_CENTER_Y = "CenterY";
    private static final String TAG_CENTER_Z = "CenterZ";
    private static final String TAG_SIGNALS = "Signals";
    private static final String TAG_SIGNAL_TYPE = "Type";
    private static final String TAG_SIGNAL_X = "X";
    private static final String TAG_SIGNAL_Z = "Z";
    private static final String TAG_SIGNAL_Y = "Y";
    private static final int REQUIRED_NODES = 2;
    private static final int MAX_PREVIEW_LINES = 5;
    private static final int MATCH_RADIUS_BLOCKS = 12;

    private final List<NodeSnapshot> nodes = new ArrayList<>();

    public SeismicProjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean tryLoadSeismogram(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshot(held);
        if (snapshot == null) {
            return false;
        }

        BlockPos nodeCenter = new BlockPos(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ());
        if (containsNodeCenter(nodeCenter)) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.node_duplicate")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (nodes.size() >= REQUIRED_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.node_full")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }

        nodes.add(NodeSnapshot.fromSnapshot(snapshot));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        syncClient();

        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.node_loaded",
                nodes.size(),
                REQUIRED_NODES)
            .withStyle(ChatFormatting.GREEN), true);
        if (nodes.size() >= REQUIRED_NODES) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.ready")
                .withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    public List<TriangulatedCandidate> getTriangulatedCandidates() {
        return List.copyOf(triangulateCandidates());
    }

    public void sendStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.nodes",
                nodes.size(),
                REQUIRED_NODES)
            .withStyle(ChatFormatting.GOLD), true);

        if (nodes.size() < REQUIRED_NODES) {
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
    }

    @Override
    public AABB getRenderBoundingBox() {
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (NodeSnapshot node : nodes) {
            CompoundTag entry = new CompoundTag();
            entry.putInt(TAG_CENTER_X, node.center().getX());
            entry.putInt(TAG_CENTER_Y, node.center().getY());
            entry.putInt(TAG_CENTER_Z, node.center().getZ());
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
            list.add(entry);
        }
        tag.put(TAG_NODES, list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodes.clear();
        ListTag list = tag.getList(TAG_NODES, Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            BlockPos center = new BlockPos(entry.getInt(TAG_CENTER_X), entry.getInt(TAG_CENTER_Y), entry.getInt(TAG_CENTER_Z));
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
            nodes.add(new NodeSnapshot(center, List.copyOf(signals)));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            load(pkt.getTag());
        }
    }

    private boolean containsNodeCenter(BlockPos center) {
        for (NodeSnapshot node : nodes) {
            if (node.center().equals(center)) {
                return true;
            }
        }
        return false;
    }

    private List<TriangulatedCandidate> triangulateCandidates() {
        if (nodes.size() < REQUIRED_NODES) {
            return List.of();
        }

        NodeSnapshot first = nodes.get(0);
        NodeSnapshot second = nodes.get(1);
        List<TriangulatedCandidate> candidates = new ArrayList<>();
        Set<Integer> matchedSecondIndices = new HashSet<>();
        int maxDistanceSq = MATCH_RADIUS_BLOCKS * MATCH_RADIUS_BLOCKS;

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

    private record NodeSnapshot(BlockPos center, List<SeismogramMapService.MapSignal> signals) {
        private static NodeSnapshot fromSnapshot(SeismogramMapService.MapSnapshot snapshot) {
            return new NodeSnapshot(
                new BlockPos(snapshot.centerX(), snapshot.centerY(), snapshot.centerZ()),
                List.copyOf(snapshot.signals())
            );
        }
    }

    public record TriangulatedCandidate(SeismicAnomalyType type, int worldX, int worldZ, int approxY, int error) {
    }
}
