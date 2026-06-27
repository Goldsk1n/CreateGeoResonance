package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

public class SeismicStationMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        StructureTemplate.StructureBlockInfo clickedInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (clickedInfo == null || !SeismicStationBlock.isStationPart(clickedInfo.state())) {
            return false;
        }

        BlockPos controllerLocalPos = SeismicStationBlock.getControllerPos(clickedInfo.state(), localPos);
        StructureTemplate.StructureBlockInfo controllerInfo = contraptionEntity.getContraption().getBlocks().get(controllerLocalPos);
        if (controllerInfo == null || controllerInfo.state().getBlock() != GeoResonanceBlocks.SEISMIC_STATION.get()) {
            return false;
        }

        MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor =
            contraptionEntity.getContraption().getActorAt(controllerLocalPos);
        CompoundTag controllerTag = actor != null && actor.getRight().blockEntityData != null
            ? actor.getRight().blockEntityData
            : controllerInfo.nbt() == null ? new CompoundTag() : controllerInfo.nbt();
        SeismicStationData stationData = new SeismicStationData();
        stationData.readFromTag(controllerTag);
        MountedHost host = new MountedHost(contraptionEntity, controllerLocalPos, stationData);
        BlockState clickedState = clickedInfo.state();
        ItemStack held = player.getItemInHand(hand);

        if (clickedState.getBlock() instanceof SeismicStationBoundingBlock) {
            SeismicStationBoundingBlock.BoundingPart part = clickedState.getValue(SeismicStationBoundingBlock.PART);
            if (part == SeismicStationBoundingBlock.BoundingPart.UPPER_LEFT && player.isShiftKeyDown()) {
                return flushIfChanged(contraptionEntity, controllerLocalPos, controllerInfo, stationData,
                    SeismicStationControllerLogic.tryExtractModule(host, player, hand));
            }

            if (SeismicModuleItem.getModuleType(held) != null) {
                return flushIfChanged(contraptionEntity, controllerLocalPos, controllerInfo, stationData,
                    SeismicStationControllerLogic.tryInsertModule(host, player, hand));
            }

            if (part == SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    return SeismicStationMountedRuntime.tryStartScan(serverPlayer, contraptionEntity, controllerLocalPos, stationData);
                }
                return true;
            }

            if (part == SeismicStationBoundingBlock.BoundingPart.LOWER_LEFT) {
                boolean isIoInteraction = held.is(Items.PAPER) || held.is(Items.INK_SAC) || held.isEmpty();
                if (!isIoInteraction) {
                    return false;
                }
                boolean changed = held.is(Items.PAPER)
                    ? SeismicStationControllerLogic.tryInsertPaper(host, player, hand)
                    : held.is(Items.INK_SAC)
                        ? SeismicStationControllerLogic.tryInsertInk(host, player, hand)
                        : SeismicStationControllerLogic.tryTakeOutputWithBareHand(host, player, hand);
                return flushIfChanged(contraptionEntity, controllerLocalPos, controllerInfo, stationData, changed);
            }

            return false;
        }

        if (clickedState.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()
            && SeismicModuleItem.getModuleType(held) != null) {
            return flushIfChanged(contraptionEntity, controllerLocalPos, controllerInfo, stationData,
                SeismicStationControllerLogic.tryInsertModule(host, player, hand));
        }

        return false;
    }

    private boolean flushIfChanged(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos,
                                   StructureTemplate.StructureBlockInfo controllerInfo, SeismicStationData stationData,
                                   boolean changed) {
        if (!changed) {
            return false;
        }
        SeismicStationMountedRuntime.persistData(contraptionEntity, controllerLocalPos, stationData);
        return true;
    }

    private static final class MountedHost implements SeismicStationControllerLogic.Host {
        private final AbstractContraptionEntity contraptionEntity;
        private final BlockPos controllerLocalPos;
        private final SeismicStationData stationData;

        private MountedHost(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos, SeismicStationData stationData) {
            this.contraptionEntity = contraptionEntity;
            this.controllerLocalPos = controllerLocalPos;
            this.stationData = stationData;
        }

        @Override
        public @Nullable net.minecraft.world.level.Level level() {
            return contraptionEntity.level();
        }

        @Override
        public BlockPos feedbackPos() {
            return BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(controllerLocalPos), 1.0F));
        }

        @Override
        public ItemStackHandler inventory() {
            return stationData.inventory();
        }

        @Override
        public void onDataChanged() {
        }
    }
}
