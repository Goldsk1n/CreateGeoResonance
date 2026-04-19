package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (!added) {
            CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        }

        SpeedLevel.getFormattedSpeedText(getTheoreticalSpeed(), isOverStressed()).forGoggles(tooltip, 1);

        CreateLang.builder()
            .add(Component.translatable("creategeoresonance.gui.goggles.pressure")
                .withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip, 1);
        CreateLang.builder()
            .add(CreateLang.number(Mth.floor(storedPressure)).style(ChatFormatting.AQUA))
            .text(ChatFormatting.GRAY, " / ")
            .add(CreateLang.number(Mth.floor(SeismicPressureStorage.maxPressure())).style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 2);
        return true;
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
        if (level.isClientSide) {
            if (storedPressure == maxPressure) {
                return;
            }
            Vec3 center = VecHelper.getCenterOf(worldPosition);
            Vec3 spawnPos = VecHelper.offsetRandomly(center, level.random, .65f);
            Vec3 motion = center.subtract(spawnPos);
            level.addParticle(new AirParticleData(1, .05f), spawnPos.x, spawnPos.y, spawnPos.z, motion.x, motion.y, motion.z);
            return;
        }

        if (storedPressure >= maxPressure) {
            return;
        }

        int previousComparatorLevel = getComparatorOutput();
        float absSpeed = Math.abs(getSpeed());
        int increment = Mth.clamp(((int) absSpeed - 100) / 20, 1, 5);
        storedPressure = Math.min(maxPressure, storedPressure + increment);
        setChanged();
        sendData();

        if (level != null && getComparatorOutput() != previousComparatorLevel) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
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
        float previousPressure = storedPressure;
        storedPressure = Mth.clamp(compound.getFloat(SeismicPressureStorage.STORED_PRESSURE_TAG), 0.0F, SeismicPressureStorage.maxPressure());
        chargeTimer = compound.getInt("ChargeTimer");
        if (clientPacket && previousPressure > 0.0F && previousPressure != storedPressure
            && storedPressure == SeismicPressureStorage.maxPressure()) {
            playFilledEffect();
        }
    }

    private void playFilledEffect() {
        if (level == null) {
            return;
        }
        AllSoundEvents.CONFIRM.playAt(level, worldPosition, 0.4f, 1, true);
        Vec3 baseMotion = new Vec3(.25, 0.1, 0);
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        for (int i = 0; i < 360; i += 10) {
            Vec3 rotatedMotion = VecHelper.rotate(baseMotion, i, Axis.Y);
            Vec3 spawnPos = center.add(rotatedMotion.normalize().scale(.25f));
            level.addParticle(ParticleTypes.SPIT, spawnPos.x, spawnPos.y, spawnPos.z,
                rotatedMotion.x, rotatedMotion.y, rotatedMotion.z);
        }
    }
}
