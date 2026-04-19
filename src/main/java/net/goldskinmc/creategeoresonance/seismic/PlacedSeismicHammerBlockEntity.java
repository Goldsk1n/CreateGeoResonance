package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;

public class PlacedSeismicHammerBlockEntity extends KineticBlockEntity {
    private float storedPressure;
    private int chargeTimer;

    public PlacedSeismicHammerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || getSpeed() == 0) {
            return;
        }
        if (chargeTimer > 0) {
            chargeTimer--;
            return;
        }

        float maxPressure = SeismicPressureStorage.maxPressure();
        if (storedPressure >= maxPressure) {
            return;
        }

        int previousComparatorLevel = getComparatorOutput();
        float absSpeed = Math.abs(getSpeed());
        int increment = Mth.clamp(((int) absSpeed - 100) / 20, 1, 5);
        storedPressure = Math.min(maxPressure, storedPressure + increment);

        if (level != null && getComparatorOutput() != previousComparatorLevel) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (storedPressure >= maxPressure) {
            sendData();
        }
        chargeTimer = Mth.clamp((int) (128f - absSpeed / 5f) - 108, 0, 20);
    }

    public float getStoredPressure() {
        return storedPressure;
    }

    public void setStoredPressure(float pressure) {
        storedPressure = Mth.clamp(pressure, 0.0F, SeismicPressureStorage.maxPressure());
        setChanged();
        sendData();
    }

    public int getComparatorOutput() {
        return Mth.floor((storedPressure / SeismicPressureStorage.maxPressure()) * 15.0F);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat(SeismicPressureStorage.STORED_PRESSURE_TAG, storedPressure);
        compound.putInt("ChargeTimer", chargeTimer);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        storedPressure = Mth.clamp(compound.getFloat(SeismicPressureStorage.STORED_PRESSURE_TAG), 0.0F, SeismicPressureStorage.maxPressure());
        chargeTimer = compound.getInt("ChargeTimer");
    }
}
