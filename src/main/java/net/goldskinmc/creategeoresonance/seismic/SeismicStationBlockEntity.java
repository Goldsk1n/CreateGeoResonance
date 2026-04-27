package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class SeismicStationBlockEntity extends KineticBlockEntity {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String MAP_ENTRIES_TAG = "MapEntries";
    private static final String MAP_READY_TAG = "MapReady";
    private static final String SCAN_RUNNING_TAG = "ScanRunning";
    private static final String AWAITING_RESULT_TAG = "AwaitingResult";
    private static final String STRIKE_TIMER_TAG = "StrikeTimer";
    private static final String REVEAL_INDEX_TAG = "RevealIndex";
    private static final String COOLDOWN_TAG = "CooldownTicks";
    private static final float NO_CLIENT_STRIKE_PROGRESS = -1.0F;
    public static final int SLOT_PAPER_INPUT = 0;
    public static final int SLOT_INK_INPUT = 1;
    public static final int SLOT_SEISMOGRAM_OUTPUT = 2;
    public static final int SLOT_MODULE_1 = 3;
    public static final int SLOT_MODULE_2 = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sendData();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_PAPER_INPUT) {
                return stack.is(Items.PAPER);
            }
            if (slot == SLOT_INK_INPUT) {
                return stack.is(Items.INK_SAC);
            }
            if (slot == SLOT_MODULE_1 || slot == SLOT_MODULE_2) {
                return SeismicModuleItem.getDetectsType(stack) != null;
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            stacks = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
            ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
            for (Tag tag : tagList) {
                CompoundTag itemTag = (CompoundTag) tag;
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < getSlots()) {
                    stacks.set(slot, ItemStack.of(itemTag));
                }
            }
            onLoad();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final List<SeismicAnomaly> queuedAnomalies = new ArrayList<>();
    private final List<MapEntry> mapEntries = new ArrayList<>();

    private boolean scanRunning;
    private boolean awaitingScanResult;
    private boolean mapReady;
    private int strikeTimer;
    private int revealIndex;
    private int cooldownTicks;
    private int comparatorOutputCache = -1;
    private float clientStrikeProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
    private int clientStrikeIntervalTicks = 1;

    public SeismicStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float speed = getOperationalSpeed();
        boolean overStressed = isStationOverStressed();
        float stressLoad = calculateStressApplied() * Math.abs(speed);

        CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        SpeedLevel.getFormattedSpeedText(speed, overStressed).forGoggles(tooltip, 1);

        CreateLang.translate("tooltip.stressImpact")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip, 1);
        CreateLang.builder()
            .add(CreateLang.number(stressLoad).style(overStressed ? ChatFormatting.RED : ChatFormatting.AQUA))
            .text(ChatFormatting.GRAY, " ")
            .add(CreateLang.translate("generic.unit.stress").style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 2);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            tickClientStrikeAnimation();
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        updateComparatorOutputIfNeeded();

        if (!scanRunning || awaitingScanResult) {
            return;
        }
        if (!hasRequiredSpeed()) {
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

    public boolean isStartLeverDown() {
        return scanRunning || awaitingScanResult;
    }

    public boolean hasPaperInput() {
        return !inventory.getStackInSlot(SLOT_PAPER_INPUT).isEmpty();
    }

    public boolean hasInkInput() {
        return !inventory.getStackInSlot(SLOT_INK_INPUT).isEmpty();
    }

    public boolean hasSeismogramOutput() {
        return !inventory.getStackInSlot(SLOT_SEISMOGRAM_OUTPUT).isEmpty();
    }

    public boolean tryInsertModule(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismicAnomalyType moduleType = SeismicModuleItem.getDetectsType(held);
        if (moduleType == null) {
            return false;
        }
        if (hasModuleType(moduleType)) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_duplicate")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        int targetSlot = findEmptyModuleSlot();
        if (targetSlot < 0) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        ItemStack inserted = held.copyWithCount(1);
        inventory.setStackInSlot(targetSlot, inserted);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_loaded", inserted.getHoverName())
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.05F);
        spawnFeedbackParticles(ParticleTypes.WAX_ON, 5, 0.18D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    public boolean tryExtractModule(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty() || !player.isShiftKeyDown()) {
            return false;
        }
        int slot = findLastFilledModuleSlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_module")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        ItemStack extracted = inventory.getStackInSlot(slot).copy();
        inventory.setStackInSlot(slot, ItemStack.EMPTY);
        player.setItemInHand(hand, extracted);
        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_unloaded", extracted.getHoverName())
            .withStyle(ChatFormatting.AQUA), true);
        playFeedbackSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 0.72F);
        spawnFeedbackParticles(ParticleTypes.WAX_OFF, 5, 0.18D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    public boolean tryInsertPaper(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.PAPER)) {
            return false;
        }
        if (!inventory.getStackInSlot(SLOT_PAPER_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.paper_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        inventory.setStackInSlot(SLOT_PAPER_INPUT, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.paper_loaded")
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 1.35F);
        spawnFeedbackParticles(ParticleTypes.CLOUD, 6, 0.22D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    public boolean tryInsertInk(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.INK_SAC)) {
            return false;
        }
        if (!inventory.getStackInSlot(SLOT_INK_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.ink_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        inventory.setStackInSlot(SLOT_INK_INPUT, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.ink_loaded")
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 0.8F);
        spawnFeedbackParticles(ParticleTypes.SQUID_INK, 4, 0.18D, 0.0D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    public boolean tryTakeOutputWithBareHand(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(SLOT_SEISMOGRAM_OUTPUT);
        if (output.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_output_ready")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        player.setItemInHand(hand, output.copy());
        inventory.setStackInSlot(SLOT_SEISMOGRAM_OUTPUT, ItemStack.EMPTY);
        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.output_taken")
            .withStyle(ChatFormatting.AQUA), true);
        playFeedbackSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.65F, 1.3F);
        spawnFeedbackParticles(ParticleTypes.GLOW, 8, 0.2D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getComparatorOutput() {
        if (!inventory.getStackInSlot(SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            return 15;
        }
        if (scanRunning || awaitingScanResult) {
            return 12;
        }
        if (cooldownTicks > 0) {
            return 4;
        }

        boolean hasPaper = !inventory.getStackInSlot(SLOT_PAPER_INPUT).isEmpty();
        boolean hasInk = !inventory.getStackInSlot(SLOT_INK_INPUT).isEmpty();
        if (hasPaper && hasInk && hasRequiredSpeed()) {
            return 8;
        }
        if (hasPaper || hasInk || hasInstalledModules()) {
            return 2;
        }
        return 0;
    }

    public float getOperationalSpeed() {
        SeismicStationBoundingBlockEntity input = getInputNode();
        if (input != null) {
            return input.getSpeed();
        }
        return getSpeed();
    }

    public boolean hasRequiredSpeed() {
        return Math.abs(getOperationalSpeed()) >= Config.STATION_MIN_SPEED.get();
    }

    public int getCurrentStrikeIntervalTicks() {
        return calculateStrikeIntervalTicks();
    }

    public boolean isStartingStrikeSequence() {
        return scanRunning
            && !awaitingScanResult
            && revealIndex == 0
            && strikeTimer > getCurrentStrikeIntervalTicks();
    }

    public void onClientStrikeImpact() {
        if (level == null || !level.isClientSide) {
            return;
        }
        startClientStrikeCycle(getCurrentStrikeIntervalTicks());
    }

    public float getClientStrikeAnimationPhase(float partialTicks) {
        if (level == null || !level.isClientSide || clientStrikeProgressTicks < 0.0F) {
            return 0.0F;
        }
        float interval = Math.max(1.0F, clientStrikeIntervalTicks);
        float progress = (clientStrikeProgressTicks + partialTicks) / interval;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public int getClientStrikeIntervalTicks() {
        return Math.max(1, clientStrikeIntervalTicks);
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
        if (!hasRequiredSpeed()) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_station.no_rotation",
                    Config.STATION_MIN_SPEED.get())
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (inventory.getStackInSlot(SLOT_PAPER_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_paper")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (inventory.getStackInSlot(SLOT_INK_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_ink")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!inventory.getStackInSlot(SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.output_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        inventory.extractItem(SLOT_PAPER_INPUT, 1, false);
        inventory.extractItem(SLOT_INK_INPUT, 1, false);
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
                buildDetectableTypes(),
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
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.scan_started")
            .withStyle(ChatFormatting.GOLD), true);
        playFeedbackSound(SoundEvents.BELL_BLOCK, 0.6F, 1.15F);
        spawnFeedbackParticles(ParticleTypes.CLOUD, 8, 0.25D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    private void acceptScanResult(List<SeismicAnomaly> anomalies) {
        queuedAnomalies.clear();
        queuedAnomalies.addAll(anomalies);
        queuedAnomalies.sort(Comparator.comparingInt(SeismicAnomaly::depth));
        revealIndex = 0;
        strikeTimer = Math.max(0, Config.STATION_STARTUP_DELAY_TICKS.get()) + calculateStrikeIntervalTicks();
        awaitingScanResult = false;
        setChanged();
        sendData();
        updateComparatorOutputIfNeeded();
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

        strikeTimer = calculateStrikeIntervalTicks();
        setChanged();
        sendData();
        updateComparatorOutputIfNeeded();
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
        writeSeismogramOutput();
        mapReady = true;
    }

    private void writeSeismogramOutput() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!inventory.getStackInSlot(SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack seismogram = createSeismogramMap(serverLevel);
        inventory.setStackInSlot(SLOT_SEISMOGRAM_OUTPUT, seismogram);
        onOutputReady();
    }

    private ItemStack createSeismogramMap(ServerLevel serverLevel) {
        return SeismogramMapService.createMap(serverLevel, worldPosition, mapEntries);
    }

    private void onOutputReady() {
        playFeedbackSound(SoundEvents.BELL_BLOCK, 0.7F, 1.65F);
        spawnFeedbackParticles(ParticleTypes.GLOW, 12, 0.25D, 0.01D);
        notifyNearbyPlayers("block.creategeoresonance.seismic_station.output_ready", ChatFormatting.AQUA);
    }

    private void notifyNearbyPlayers(String messageKey, ChatFormatting style) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Component message = Component.translatable(messageKey).withStyle(style);
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        for (ServerPlayer nearbyPlayer : serverLevel.players()) {
            if (nearbyPlayer.distanceToSqr(x, y, z) <= 24.0D * 24.0D) {
                nearbyPlayer.displayClientMessage(message, true);
            }
        }
    }

    private void playFeedbackSound(SoundEvent sound, float volume, float pitch) {
        if (level == null) {
            return;
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void spawnFeedbackParticles(ParticleOptions particle, int count, double spread, double speed) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(particle,
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 1.05D,
            worldPosition.getZ() + 0.5D,
            count,
            spread,
            0.12D,
            spread,
            speed);
    }

    @Override
    public float calculateStressApplied() {
        float impact = Config.STATION_STRESS_IMPACT.get().floatValue();
        lastStressApplied = impact;
        return impact;
    }

    private int calculateStrikeIntervalTicks() {
        int intervalMultiplier = Math.max(1, Config.STATION_STRIKE_INTERVAL_MULTIPLIER.get());
        int baseInterval = Config.STATION_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier;
        int minInterval = Math.min(baseInterval, Config.STATION_MIN_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier);
        float minSpeed = Math.max(1.0F, Config.STATION_MIN_SPEED.get());
        float absSpeed = Math.abs(getOperationalSpeed());
        float speedBonus = absSpeed / minSpeed;
        float maxBonus = (float) Config.STATION_MAX_SPEED_BONUS.get().doubleValue();
        float clampedBonus = Mth.clamp(speedBonus, 1.0F, maxBonus);
        int scaledInterval = Mth.ceil(baseInterval / clampedBonus);
        return Mth.clamp(scaledInterval, minInterval, baseInterval);
    }

    private void tickClientStrikeAnimation() {
        if (clientStrikeProgressTicks < 0.0F) {
            return;
        }
        clientStrikeProgressTicks++;
        if (clientStrikeProgressTicks >= Math.max(1, clientStrikeIntervalTicks)) {
            clientStrikeProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
        }
    }

    private void startClientStrikeCycle(int intervalTicks) {
        clientStrikeIntervalTicks = Math.max(1, intervalTicks);
        clientStrikeProgressTicks = 0.0F;
    }

    public SeismicStationBoundingBlockEntity getInputNode() {
        if (level == null) {
            return null;
        }
        BlockPos inputPos = SeismicStationBlock.getUpperRightPos(worldPosition);
        BlockEntity blockEntity = level.getBlockEntity(inputPos);
        if (blockEntity instanceof SeismicStationBoundingBlockEntity input && input.isInputPart()) {
            return input;
        }
        return null;
    }

    private boolean isStationOverStressed() {
        SeismicStationBoundingBlockEntity input = getInputNode();
        if (input != null) {
            return input.isOverStressed();
        }
        return isOverStressed();
    }

    @Override
    public AABB getRenderBoundingBox() {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        BlockPos left = SeismicStationBlock.getLeftPos(worldPosition, facing);
        int minX = Math.min(worldPosition.getX(), left.getX());
        int minZ = Math.min(worldPosition.getZ(), left.getZ());
        int maxX = Math.max(worldPosition.getX(), left.getX()) + 1;
        int maxZ = Math.max(worldPosition.getZ(), left.getZ()) + 1;
        return new AABB(minX, worldPosition.getY(), minZ, maxX, worldPosition.getY() + 2, maxZ);
    }

    public void dropInventory() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        updateComparatorOutputIfNeeded();
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
        boolean previousScanRunning = scanRunning;
        boolean previousAwaitingResult = awaitingScanResult;
        int previousRevealIndex = revealIndex;
        int previousStrikeTimer = strikeTimer;

        inventory.deserializeNBT(compound.getCompound(INVENTORY_TAG));
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

        if (clientPacket && level != null && level.isClientSide) {
            if (scanRunning && !awaitingScanResult && strikeTimer > 0) {
                boolean enteredReplay = !previousScanRunning || previousAwaitingResult;
                boolean strikeWindowChanged = revealIndex != previousRevealIndex || strikeTimer != previousStrikeTimer;
                if (enteredReplay || strikeWindowChanged) {
                    startClientStrikeCycle(strikeTimer);
                }
            } else {
                clientStrikeProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
            }
        } else if (!clientPacket) {
            comparatorOutputCache = -1;
            updateComparatorOutputIfNeeded();
        }
    }

    private Set<SeismicAnomalyType> buildDetectableTypes() {
        EnumSet<SeismicAnomalyType> detectable = EnumSet.of(
            SeismicAnomalyType.CAVE,
            SeismicAnomalyType.WATER,
            SeismicAnomalyType.LAVA
        );
        SeismicAnomalyType first = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(SLOT_MODULE_1));
        if (first != null) {
            detectable.add(first);
        }
        SeismicAnomalyType second = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(SLOT_MODULE_2));
        if (second != null) {
            detectable.add(second);
        }
        return detectable;
    }

    private boolean hasModuleType(SeismicAnomalyType moduleType) {
        return moduleType == SeismicModuleItem.getDetectsType(inventory.getStackInSlot(SLOT_MODULE_1))
            || moduleType == SeismicModuleItem.getDetectsType(inventory.getStackInSlot(SLOT_MODULE_2));
    }

    private boolean hasInstalledModules() {
        return !inventory.getStackInSlot(SLOT_MODULE_1).isEmpty()
            || !inventory.getStackInSlot(SLOT_MODULE_2).isEmpty();
    }

    private int findEmptyModuleSlot() {
        if (inventory.getStackInSlot(SLOT_MODULE_1).isEmpty()) {
            return SLOT_MODULE_1;
        }
        if (inventory.getStackInSlot(SLOT_MODULE_2).isEmpty()) {
            return SLOT_MODULE_2;
        }
        return -1;
    }

    private int findLastFilledModuleSlot() {
        if (!inventory.getStackInSlot(SLOT_MODULE_2).isEmpty()) {
            return SLOT_MODULE_2;
        }
        if (!inventory.getStackInSlot(SLOT_MODULE_1).isEmpty()) {
            return SLOT_MODULE_1;
        }
        return -1;
    }

    private void updateComparatorOutputIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }
        int output = getComparatorOutput();
        if (output == comparatorOutputCache) {
            return;
        }
        comparatorOutputCache = output;
        updateComparatorOutputs();
    }

    private void updateComparatorOutputs() {
        if (level == null) {
            return;
        }
        BlockState controllerState = getBlockState();
        level.updateNeighbourForOutputSignal(worldPosition, controllerState.getBlock());

        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        updateComparatorAt(SeismicStationBlock.getLeftPos(worldPosition, facing));
        updateComparatorAt(SeismicStationBlock.getUpperRightPos(worldPosition));
        updateComparatorAt(SeismicStationBlock.getUpperLeftPos(worldPosition, facing));
    }

    private void updateComparatorAt(BlockPos partPos) {
        if (level == null) {
            return;
        }
        BlockState partState = level.getBlockState(partPos);
        level.updateNeighbourForOutputSignal(partPos, partState.getBlock());
    }

    public record MapEntry(SeismicAnomalyType type, int offsetX, int offsetZ, int approxY) {
    }
}
