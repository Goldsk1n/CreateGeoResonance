package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeismicStationBoundingBlockEntity extends KineticBlockEntity {
    private boolean stressRefreshPending = true;

    public SeismicStationBoundingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        refreshStress();
    }

    @Override
    public float calculateStressApplied() {
        if (!isInputPart()) {
            lastStressApplied = 0.0F;
            return 0.0F;
        }
        SeismicStationBlockEntity station = getController();
        float requiredSu = station == null ? SeismicStationBlockEntity.requiredStress(0) : station.getRequiredStress();
        // Actual RPM becomes zero when overstressed; theoretical RPM allows the network to recover.
        float absSpeed = Math.abs(getTheoreticalSpeed());
        lastStressApplied = absSpeed > 0.0F ? requiredSu / absSpeed : 0.0F;
        return lastStressApplied;
    }

    public void refreshStress() {
        if (level == null || level.isClientSide || !isInputPart() || !hasNetwork()) {
            return;
        }
        KineticNetwork network = getOrCreateNetwork();
        Float registeredImpact = network.members.get(this);
        if (registeredImpact == null) {
            return;
        }
        float impact = calculateStressApplied();
        // Recompute once after loading, even if addSilently already registered the new impact.
        if (stressRefreshPending || Float.compare(registeredImpact, impact) != 0) {
            network.updateStressFor(this, impact);
            stressRefreshPending = false;
        }
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isInputPart()) {
            SeismicStationBlockEntity station = getController();
            if (station != null) {
                return station.addToTooltip(tooltip, isPlayerSneaking);
            }
        }
        return super.addToTooltip(tooltip, isPlayerSneaking);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        SeismicStationBlockEntity station = getController();
        if (station != null) {
            return station.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public boolean isInputPart() {
        return getBlockState().getValue(SeismicStationBoundingBlock.PART) == SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT;
    }

    @Nullable
    private SeismicStationBlockEntity getController() {
        if (level == null) {
            return null;
        }
        BlockPos controllerPos = SeismicStationBoundingBlock.getControllerPos(getBlockState(), worldPosition);
        BlockEntity controller = level.getBlockEntity(controllerPos);
        return controller instanceof SeismicStationBlockEntity station ? station : null;
    }
}
