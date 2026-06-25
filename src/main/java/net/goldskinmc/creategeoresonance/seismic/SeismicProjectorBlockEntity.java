package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.goldskinmc.creategeoresonance.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeismicProjectorBlockEntity extends KineticBlockEntity {
    private static final float TARGET_REQUIRED_SU = 512.0F;

    private final SeismicProjectorData projectorData = new SeismicProjectorData();

    public SeismicProjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean tryLoadSeismogram(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshotWithExactData(held);
        if (snapshot == null || level == null) {
            return false;
        }

        SeismicProjectorData.LoadResult result = projectorData.tryLoadSnapshot(
            level.dimension().location().toString(),
            worldPosition,
            snapshot
        );
        if (!result.success()) {
            sendLoadFailure(player, result.status());
            return true;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        syncClient();
        updatePoweredState();

        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.node_loaded",
                result.loadedNodeCount(),
                SeismicProjectorData.MAX_NODES)
            .withStyle(ChatFormatting.GREEN), true);
        if (projectorData.hasExactNodes()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.exact_ready")
                .withStyle(ChatFormatting.GOLD), true);
        } else if (projectorData.hasPreviewNodes()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.ready")
                .withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    public List<SeismicProjectorData.TriangulatedCandidate> getTriangulatedCandidates() {
        return List.copyOf(projectorData.getTriangulatedCandidates());
    }

    public List<SeismicProjectorData.ExactVein> getConfirmedVeins() {
        if (!hasRequiredSpeed()) {
            return List.of();
        }
        return List.copyOf(projectorData.getConfirmedVeins());
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
                projectorData.getLoadedNodeCount(),
                SeismicProjectorData.MAX_NODES)
            .withStyle(ChatFormatting.GOLD), true);

        if (!projectorData.hasPreviewNodes()) {
            return;
        }

        List<SeismicProjectorData.TriangulatedCandidate> candidates = projectorData.getTriangulatedCandidates();
        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.matches",
                candidates.size())
            .withStyle(ChatFormatting.AQUA), true);

        int lines = Math.min(SeismicProjectorData.MAX_PREVIEW_LINES, candidates.size());
        for (int i = 0; i < lines; i++) {
            SeismicProjectorData.TriangulatedCandidate candidate = candidates.get(i);
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.match_line",
                    Component.translatable(SeismicProjectorData.signalNameKey(candidate.type())),
                    candidate.worldX(),
                    candidate.worldZ(),
                    candidate.approxY(),
                    candidate.error())
                .withStyle(ChatFormatting.GRAY), false);
        }

        if (!projectorData.hasExactNodes()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.need_third_node")
                .withStyle(ChatFormatting.DARK_AQUA), false);
            return;
        }

        List<SeismicProjectorData.ExactVein> exactVeins = projectorData.getConfirmedVeins();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_projector.exact_matches", exactVeins.size())
            .withStyle(ChatFormatting.GOLD), false);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return projectorData.getRenderBoundingBox(worldPosition, isProjecting());
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        projectorData.writeToTag(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        projectorData.readFromTag(tag);
        if (!clientPacket) {
            updatePoweredState();
        }
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
        return projectorData.getLoadedNodeCount();
    }

    @Nullable
    public BlockPos getSingleLoadedStationPos() {
        return projectorData.getSingleLoadedStationPos();
    }

    public SeismicProjectorData getProjectorData() {
        return projectorData;
    }

    private boolean isProjecting() {
        return projectorData.canProjectFrom(worldPosition, hasRequiredSpeed());
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

    private void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void sendLoadFailure(Player player, SeismicProjectorData.LoadStatus status) {
        MutableComponent message = switch (status) {
            case DIMENSION_MISMATCH -> Component.translatable("block.creategeoresonance.seismic_projector.dimension_mismatch");
            case STATION_OUT_OF_RANGE -> Component.translatable(
                "block.creategeoresonance.seismic_projector.station_out_of_range",
                Config.PROJECTOR_STATION_RANGE_CHUNKS.get());
            case DUPLICATE -> Component.translatable("block.creategeoresonance.seismic_projector.node_duplicate");
            case TOO_CLOSE -> Component.translatable(
                "block.creategeoresonance.seismic_projector.node_too_close",
                Config.PROJECTOR_MIN_NODE_DISTANCE_BLOCKS.get());
            case FULL -> Component.translatable("block.creategeoresonance.seismic_projector.node_full");
            case SUCCESS -> Component.empty();
        };
        if (!message.getString().isEmpty()) {
            player.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
        }
    }
}
