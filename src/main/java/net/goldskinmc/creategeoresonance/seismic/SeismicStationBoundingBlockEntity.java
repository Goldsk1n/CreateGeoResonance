package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SeismicStationBoundingBlockEntity extends KineticBlockEntity {
    public SeismicStationBoundingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        BlockPos controllerPos = SeismicStationBoundingBlock.getControllerPos(getBlockState(), worldPosition);
        BlockEntity controller = level.getBlockEntity(controllerPos);
        if (controller instanceof SeismicStationBlockEntity station) {
            return station.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public boolean isInputPart() {
        return getBlockState().getValue(SeismicStationBoundingBlock.PART) == SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT;
    }
}
