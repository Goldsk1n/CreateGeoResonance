package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SeismicStationBlockEntity extends KineticBlockEntity {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String MAP_ENTRIES_TAG = "MapEntries";
    private static final String MAP_READY_TAG = "MapReady";
    private static final String SCAN_RUNNING_TAG = "ScanRunning";
    private static final String AWAITING_RESULT_TAG = "AwaitingResult";
    private static final String STRIKE_TIMER_TAG = "StrikeTimer";
    private static final String REVEAL_INDEX_TAG = "RevealIndex";
    private static final String COOLDOWN_TAG = "CooldownTicks";
    private static final String STORED_PRESSURE_TAG = "StoredPressure";
    private static final String CHARGE_TIMER_TAG = "ChargeTimer";

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sendData();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.PAPER);
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final List<SeismicAnomaly> queuedAnomalies = new ArrayList<>();
    private final List<MapEntry> mapEntries = new ArrayList<>();

    private float storedPressure;
    private int chargeTimer;
    private boolean scanRunning;
    private boolean awaitingScanResult;
    private boolean mapReady;
    private int strikeTimer;
    private int revealIndex;
    private int cooldownTicks;

    public SeismicStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        tickCharging();
        if (level.isClientSide) {
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (!scanRunning || awaitingScanResult) {
            return;
        }
        if (strikeTimer > 0) {
            strikeTimer--;
            return;
        }

        replayStrike();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public List<MapEntry> getMapEntries() {
        return List.copyOf(mapEntries);
    }

    public int getStationRadius() {
        return Config.STATION_RADIUS.get();
    }

    public boolean isScanRunning() {
        return scanRunning;
    }

    public boolean isAwaitingScanResult() {
        return awaitingScanResult;
    }

    public boolean isMapReady() {
        return mapReady;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public float getStoredPressure() {
        return storedPressure;
    }

    public boolean tryStartScan(ServerPlayer player) {
        if (scanRunning || awaitingScanResult) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.busy")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (cooldownTicks > 0) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.cooldown")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (Math.abs(getOperationalSpeed()) < Config.STATION_MIN_SPEED.get()) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_station.no_rotation",
                    Config.STATION_MIN_SPEED.get())
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (inventory.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_paper")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        float cost = scanPressureCost();
        if (storedPressure < cost) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_pressure")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        inventory.extractItem(0, 1, false);
        storedPressure = Math.max(0.0F, storedPressure - cost);
        scanRunning = true;
        awaitingScanResult = true;
        mapReady = false;
        strikeTimer = 0;
        revealIndex = 0;
        mapEntries.clear();
        queuedAnomalies.clear();

        if (level instanceof ServerLevel serverLevel) {
            BlockPos origin = worldPosition.immutable();
            SeismicScanQueue.enqueue(new SeismicScanQueue.SeismicScanRequest(
                serverLevel,
                origin,
                -1,
                Config.STATION_RADIUS.get(),
                Config.STATION_DEPTH.get(),
                Config.STATION_NOISE.get().floatValue(),
                serverLevel.getGameTime(),
                false,
                (request, anomalies) -> {
                    BlockEntity blockEntity = request.level().getBlockEntity(origin);
                    if (blockEntity instanceof SeismicStationBlockEntity station) {
                        station.acceptScanResult(anomalies);
                    }
                }
            ));
        }

        setChanged();
        sendData();
        return true;
    }

    private void acceptScanResult(List<SeismicAnomaly> anomalies) {
        queuedAnomalies.clear();
        queuedAnomalies.addAll(anomalies);
        queuedAnomalies.sort(Comparator.comparingInt(SeismicAnomaly::depth));
        revealIndex = 0;
        strikeTimer = 0;
        awaitingScanResult = false;
        setChanged();
        sendData();
    }

    private void replayStrike() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        GeoResonancePackets.sendSeismicImpact(serverLevel, worldPosition, -1, false);
        if (revealIndex < queuedAnomalies.size()) {
            SeismicAnomaly anomaly = queuedAnomalies.get(revealIndex++);
            GeoResonancePackets.sendSeismicResult(
                serverLevel,
                worldPosition,
                -1,
                false,
                Config.STATION_DEPTH.get(),
                List.of(anomaly)
            );
        } else {
            generateMapEntries(serverLevel);
            scanRunning = false;
            awaitingScanResult = false;
            cooldownTicks = Config.STATION_COOLDOWN_TICKS.get();
            queuedAnomalies.clear();
        }

        strikeTimer = Config.STATION_STRIKE_INTERVAL_TICKS.get();
        setChanged();
        sendData();
    }

    private void generateMapEntries(ServerLevel serverLevel) {
        mapEntries.clear();
        for (SeismicAnomaly anomaly : queuedAnomalies) {
            int spread = 5 + Mth.floor(anomaly.depth() / 20.0F);
            int sampledOffset = serverLevel.random.nextInt(spread * 2 + 1) - spread;
            int sampledY = worldPosition.getY() - anomaly.depth() + sampledOffset;
            sampledY = Math.max(-64, sampledY);
            sampledY = Math.max(serverLevel.getMinBuildHeight(), sampledY);
            mapEntries.add(new MapEntry(anomaly.type(), anomaly.offsetX(), anomaly.offsetZ(), sampledY));
        }
        mapReady = true;
    }

    private void tickCharging() {
        float operationalSpeed = getOperationalSpeed();
        if (level == null || operationalSpeed == 0) {
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

        float absSpeed = Math.abs(operationalSpeed);
        int increment = Mth.clamp(((int) absSpeed - 100) / 20, 1, 5);
        storedPressure = Math.min(maxPressure, storedPressure + increment);
        chargeTimer = Mth.clamp((int) (128f - absSpeed / 5f) - 108, 0, 20);
        setChanged();
        sendData();
    }

    private float getOperationalSpeed() {
        if (level == null) {
            return getSpeed();
        }
        BlockPos inputPos = SeismicStationBlock.getUpperRightPos(worldPosition);
        BlockEntity blockEntity = level.getBlockEntity(inputPos);
        if (blockEntity instanceof SeismicStationBoundingBlockEntity input && input.isInputPart()) {
            return input.getSpeed();
        }
        return getSpeed();
    }

    private float scanPressureCost() {
        return Mth.clamp(BacktankUtil.maxAirWithoutEnchants() / (float) Config.SCANS_PER_BACKTANK.get(), 1.0F, SeismicPressureStorage.maxPressure());
    }

    public void dropInventory() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stack.copy());
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> inventory);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put(INVENTORY_TAG, inventory.serializeNBT());
        compound.putFloat(STORED_PRESSURE_TAG, storedPressure);
        compound.putInt(CHARGE_TIMER_TAG, chargeTimer);
        compound.putBoolean(SCAN_RUNNING_TAG, scanRunning);
        compound.putBoolean(AWAITING_RESULT_TAG, awaitingScanResult);
        compound.putBoolean(MAP_READY_TAG, mapReady);
        compound.putInt(STRIKE_TIMER_TAG, strikeTimer);
        compound.putInt(REVEAL_INDEX_TAG, revealIndex);
        compound.putInt(COOLDOWN_TAG, cooldownTicks);

        ListTag entriesTag = new ListTag();
        for (MapEntry entry : mapEntries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Type", entry.type().name());
            entryTag.putInt("X", entry.offsetX());
            entryTag.putInt("Z", entry.offsetZ());
            entryTag.putInt("Y", entry.approxY());
            entriesTag.add(entryTag);
        }
        compound.put(MAP_ENTRIES_TAG, entriesTag);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        inventory.deserializeNBT(compound.getCompound(INVENTORY_TAG));
        storedPressure = Mth.clamp(compound.getFloat(STORED_PRESSURE_TAG), 0.0F, SeismicPressureStorage.maxPressure());
        chargeTimer = compound.getInt(CHARGE_TIMER_TAG);
        scanRunning = compound.getBoolean(SCAN_RUNNING_TAG);
        awaitingScanResult = compound.getBoolean(AWAITING_RESULT_TAG);
        mapReady = compound.getBoolean(MAP_READY_TAG);
        strikeTimer = compound.getInt(STRIKE_TIMER_TAG);
        revealIndex = compound.getInt(REVEAL_INDEX_TAG);
        cooldownTicks = compound.getInt(COOLDOWN_TAG);

        mapEntries.clear();
        ListTag entriesTag = compound.getList(MAP_ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag tag : entriesTag) {
            CompoundTag entryTag = (CompoundTag) tag;
            SeismicAnomalyType type = SeismicAnomalyType.valueOf(entryTag.getString("Type"));
            mapEntries.add(new MapEntry(type, entryTag.getInt("X"), entryTag.getInt("Z"), entryTag.getInt("Y")));
        }
    }

    public record MapEntry(SeismicAnomalyType type, int offsetX, int offsetZ, int approxY) {
    }
}
