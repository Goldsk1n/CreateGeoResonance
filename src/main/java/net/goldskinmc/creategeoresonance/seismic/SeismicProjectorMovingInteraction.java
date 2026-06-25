package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

public class SeismicProjectorMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        MutablePair<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo, MovementContext> actor =
            contraptionEntity.getContraption().getActorAt(localPos);
        if (actor == null) {
            return false;
        }

        MovementContext context = actor.getRight();
        if (!(context.state.getBlock() instanceof SeismicProjectorBlock)) {
            return false;
        }

        BlockPos projectorPos = context.position != null
            ? BlockPos.containing(context.position)
            : BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atLowerCornerOf(localPos), 1.0F));
        if (context.blockEntityData == null) {
            context.blockEntityData = new CompoundTag();
        }
        SeismicProjectorData projectorData = new SeismicProjectorData();
        projectorData.readFromTag(context.blockEntityData);

        ItemStack held = player.getItemInHand(hand);
        SeismogramMapService.MapSnapshot snapshot = SeismogramMapService.readSnapshotWithExactData(held);
        if (snapshot != null) {
            SeismicProjectorData.LoadResult result = projectorData.tryLoadSnapshot(
                contraptionEntity.level().dimension().location().toString(),
                projectorPos,
                snapshot
            );
            if (!result.success()) {
                sendLoadFailure(player, result.status());
                return true;
            }

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            projectorData.writeToTag(context.blockEntityData);
            syncActorData(contraptionEntity, localPos, actor, context);

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

        if (!held.isEmpty()) {
            return false;
        }

        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.nodes",
                projectorData.getLoadedNodeCount(),
                SeismicProjectorData.MAX_NODES)
            .withStyle(ChatFormatting.GOLD), true);

        if (!projectorData.hasPreviewNodes()) {
            return true;
        }

        if (!projectorData.areLoadedStationsWithinRange(projectorPos)) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_projector.station_out_of_range",
                    Config.PROJECTOR_STATION_RANGE_CHUNKS.get())
                .withStyle(ChatFormatting.RED), true);
            return true;
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
            return true;
        }

        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.exact_matches",
                projectorData.getConfirmedVeins().size())
            .withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    private void syncActorData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                               MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor,
                               MovementContext context) {
        StructureTemplate.StructureBlockInfo updatedInfo = new StructureTemplate.StructureBlockInfo(
            localPos,
            actor.getLeft().state().setValue(SeismicProjectorBlock.ACTIVE, projectorDataState(context, localPos, contraptionEntity, actor)),
            context.blockEntityData.copy()
        );
        int actorIndex = contraptionEntity.getContraption().getActors().indexOf(actor);
        if (actorIndex >= 0) {
            setContraptionActorData(contraptionEntity, actorIndex, updatedInfo, context);
        }
        setContraptionBlockData(contraptionEntity, localPos, updatedInfo);
        GeoResonancePackets.sendMountedProjectorState(contraptionEntity, localPos, context.blockEntityData);
    }

    private boolean projectorDataState(MovementContext context, BlockPos localPos, AbstractContraptionEntity contraptionEntity,
                                       MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor) {
        SeismicProjectorData projectorData = new SeismicProjectorData();
        projectorData.readFromTag(context.blockEntityData);
        BlockPos projectorPos = context.position != null
            ? BlockPos.containing(context.position)
            : BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F));
        return projectorData.canProjectFrom(projectorPos, true);
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
