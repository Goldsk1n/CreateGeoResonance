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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeismicStationBlockEntity extends KineticBlockEntity {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String MAP_ENTRIES_TAG = "MapEntries";
    private static final String EXACT_CLUSTERS_TAG = "ExactClusters";
    private static final String MAP_READY_TAG = "MapReady";
    private static final String SCAN_RUNNING_TAG = "ScanRunning";
    private static final String AWAITING_RESULT_TAG = "AwaitingResult";
    private static final String STRIKE_TIMER_TAG = "StrikeTimer";
    private static final String REVEAL_INDEX_TAG = "RevealIndex";
    private static final String COOLDOWN_TAG = "CooldownTicks";
    private static final String CURRENT_SCAN_DEPTH_TAG = "CurrentScanDepth";
    private static final float NO_CLIENT_STRIKE_PROGRESS = -1.0F;
    public static final int SLOT_PAPER_INPUT = 0;
    public static final int SLOT_INK_INPUT = 1;
    public static final int SLOT_SEISMOGRAM_OUTPUT = 2;
    public static final int SLOT_MODULE_START = 3;
    public static final int MODULE_SLOT_COUNT = 8;
    public static final int SLOT_MODULE_END = SLOT_MODULE_START + MODULE_SLOT_COUNT - 1;
    private static final float BASE_REQUIRED_SU = 256.0F;
    private static final float MODULE_REQUIRED_SU = 64.0F;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_MODULE_START + MODULE_SLOT_COUNT) {
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
            if (isModuleSlot(slot)) {
                return SeismicModuleItem.getModuleType(stack) != null;
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
            List<ItemStack> overflowModules = new ArrayList<>();
            for (Tag tag : tagList) {
                CompoundTag itemTag = (CompoundTag) tag;
                int slot = itemTag.getInt("Slot");
                ItemStack stack = ItemStack.of(itemTag);
                if (slot >= 0 && slot < getSlots()) {
                    stacks.set(slot, stack);
                } else if (slot > SLOT_MODULE_END && slot < SLOT_MODULE_START + 16 && SeismicModuleItem.getModuleType(stack) != null) {
                    overflowModules.add(stack);
                }
            }

            if (!overflowModules.isEmpty()) {
                for (ItemStack moduleStack : overflowModules) {
                    for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
                        if (stacks.get(slot).isEmpty()) {
                            stacks.set(slot, moduleStack.copyWithCount(1));
                            break;
                        }
                    }
                }
            }
            onLoad();
        }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final List<SeismicAnomaly> queuedAnomalies = new ArrayList<>();
    private final List<MapEntry> mapEntries = new ArrayList<>();
    private final List<SeismogramMapService.ExactCluster> exactClusters = new ArrayList<>();

    private boolean scanRunning;
    private boolean awaitingScanResult;
    private boolean mapReady;
    private int strikeTimer;
    private int revealIndex;
    private int cooldownTicks;
    private int currentScanDepth;
    private int comparatorOutputCache = -1;
    private float clientStrikeProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
    private int clientStrikeIntervalTicks = 1;
    private float clientSwingProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
    private int clientSwingDurationTicks = 1;

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

        cooldownTicks = 0;
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

    public List<ItemStack> getInstalledModuleStacks() {
        List<ItemStack> modules = new ArrayList<>();
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                modules.add(stack.copy());
            }
        }
        return modules;
    }

    public boolean hasBelowZeroModule() {
        return hasModuleInstalled(SeismicModuleType.BELOW_ZERO);
    }

    public boolean hasNoiseCancellationModule() {
        return hasModuleInstalled(SeismicModuleType.NOISE_CANCELLATION);
    }

    public boolean tryInsertModule(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismicModuleType moduleType = SeismicModuleItem.getModuleType(held);
        if (moduleType == null) {
            return false;
        }
        if (hasModuleInstalled(moduleType)) {
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
        if (!player.isShiftKeyDown()) {
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
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.setItemInHand(hand, extracted);
        } else if (!player.addItem(extracted.copy())) {
            Containers.dropItemStack(level, player.getX(), player.getY() + 0.5D, player.getZ(), extracted.copy());
        }
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

    public void onClientStrikeCycleStart() {
        onClientStrikeCycleStart(getCurrentStrikeIntervalTicks());
    }

    public void onClientStrikeCycleStart(int intervalTicks) {
        if (level == null || !level.isClientSide) {
            return;
        }
        startClientStrikeCycle(intervalTicks);
    }

    public void onClientStrikeImpact() {
        if (level == null || !level.isClientSide) {
            return;
        }
        onClientStrikeCycleStart();
        startClientSwingCycle(clientSwingDurationFor(getCurrentStrikeIntervalTicks()));
    }

    public void onClientEchoArrival() {
        if (level == null || !level.isClientSide) {
            return;
        }
        startClientSwingCycle(clientSwingDurationFor(getCurrentStrikeIntervalTicks()));
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

    public float getClientSwingAnimationPhase(float partialTicks) {
        if (level == null || !level.isClientSide || clientSwingProgressTicks < 0.0F) {
            return 0.0F;
        }
        float duration = Math.max(1.0F, clientSwingDurationTicks);
        float progress = (clientSwingProgressTicks + partialTicks) / duration;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public boolean tryStartScan(ServerPlayer player) {
        if (scanRunning || awaitingScanResult) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.busy")
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
        exactClusters.clear();
        queuedAnomalies.clear();

        if (level instanceof ServerLevel serverLevel) {
            BlockPos origin = worldPosition.immutable();
            currentScanDepth = Math.max(1, worldPosition.getY() - serverLevel.getMinBuildHeight());
            SeismicScanQueue.enqueue(new SeismicScanQueue.SeismicScanRequest(
                serverLevel,
                origin,
                -1,
                Config.STATION_RADIUS.get(),
                currentScanDepth,
                Config.STATION_NOISE.get().floatValue(),
                serverLevel.getGameTime(),
                false,
                buildDetectableTypes(),
                hasModuleInstalled(SeismicModuleType.BELOW_ZERO),
                (request, result) -> {
                    BlockEntity blockEntity = request.level().getBlockEntity(origin);
                    if (blockEntity instanceof SeismicStationBlockEntity station) {
                        station.acceptScanResult(result);
                    }
                }
            ));
        }

        setChanged();
        sendData();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.starting")
            .withStyle(ChatFormatting.GOLD), true);
        playFeedbackSound(SoundEvents.LEVER_CLICK, 0.6F, 0.6F);
        spawnFeedbackParticles(ParticleTypes.CLOUD, 8, 0.25D, 0.01D);
        updateComparatorOutputIfNeeded();
        return true;
    }

    private void acceptScanResult(SeismicScanQueue.SeismicScanResult result) {
        queuedAnomalies.clear();
        queuedAnomalies.addAll(prioritizeByModuleOrder(result.anomalies()));
        queuedAnomalies.sort(Comparator.comparingInt(SeismicAnomaly::depth));
        exactClusters.clear();
        for (SeismicScanQueue.ExactCluster cluster : result.exactClusters()) {
            exactClusters.add(new SeismogramMapService.ExactCluster(cluster.type(), List.copyOf(cluster.blocks())));
        }
        revealIndex = 0;
        strikeTimer = calculateStrikeIntervalTicks();
        awaitingScanResult = false;
        setChanged();
        sendData();
        updateComparatorOutputIfNeeded();
    }

    private List<SeismicAnomaly> prioritizeByModuleOrder(List<SeismicAnomaly> anomalies) {
        if (anomalies.isEmpty()) {
            return anomalies;
        }

        Map<SeismicAnomalyType, Integer> modulePriority = buildModulePriorityMap();
        List<SeismicAnomaly> ordered = new ArrayList<>(anomalies);
        ordered.sort((left, right) -> comparePriority(left, right, modulePriority));

        int mergeDistance = Config.ECHO_MERGE_DISTANCE.get();
        List<SeismicAnomaly> selected = new ArrayList<>();
        for (SeismicAnomaly candidate : ordered) {
            int overlapIndex = findOverlappingAnomaly(selected, candidate, mergeDistance);
            if (overlapIndex < 0) {
                selected.add(candidate);
                continue;
            }

            SeismicAnomaly current = selected.get(overlapIndex);
            if (comparePriority(candidate, current, modulePriority) < 0) {
                selected.set(overlapIndex, candidate);
            }
        }
        return selected;
    }

    private Map<SeismicAnomalyType, Integer> buildModulePriorityMap() {
        Map<SeismicAnomalyType, Integer> priority = new HashMap<>();
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            SeismicAnomalyType type = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(slot));
            if (type != null) {
                priority.put(type, slot);
            }
        }
        return priority;
    }

    private static int comparePriority(SeismicAnomaly left, SeismicAnomaly right, Map<SeismicAnomalyType, Integer> modulePriority) {
        int typeCompare = Integer.compare(typePriorityScore(left.type(), modulePriority), typePriorityScore(right.type(), modulePriority));
        if (typeCompare != 0) {
            return -typeCompare;
        }
        return -Double.compare(anomalyStrength(left), anomalyStrength(right));
    }

    private static int findOverlappingAnomaly(List<SeismicAnomaly> selected, SeismicAnomaly candidate, int mergeDistance) {
        for (int i = 0; i < selected.size(); i++) {
            SeismicAnomaly existing = selected.get(i);
            int effectiveMergeDistance = Math.max(mergeDistance, (existing.radius() + candidate.radius()) / 2);
            int dx = existing.offsetX() - candidate.offsetX();
            int dz = existing.offsetZ() - candidate.offsetZ();
            if (dx * dx + dz * dz <= effectiveMergeDistance * effectiveMergeDistance) {
                return i;
            }
        }
        return -1;
    }

    private static int typePriorityScore(SeismicAnomalyType type, Map<SeismicAnomalyType, Integer> modulePriority) {
        Integer slotPriority = modulePriority.get(type);
        if (slotPriority != null) {
            return 1000 + slotPriority;
        }

        return switch (type) {
            case LAVA -> 30;
            case WATER -> 20;
            case CAVE -> 10;
            default -> 0;
        };
    }

    private static double anomalyStrength(SeismicAnomaly anomaly) {
        return anomaly.confidence() * (1.0D + anomaly.radius() * 0.2D);
    }

    private void replayStrike() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (queuedAnomalies.isEmpty()) {
            if (revealIndex == 0) {
                GeoResonancePackets.sendSeismicImpact(serverLevel, worldPosition, -1, false);
            }
            finishReplayScan(serverLevel);
            return;
        }

        if (revealIndex >= queuedAnomalies.size()) {
            finishReplayScan(serverLevel);
            return;
        }

        GeoResonancePackets.sendSeismicImpact(serverLevel, worldPosition, -1, false);
        SeismicAnomaly anomaly = queuedAnomalies.get(revealIndex++);
        GeoResonancePackets.sendSeismicResult(
            serverLevel,
            worldPosition,
            -1,
            false,
            currentScanDepth,
            List.of(anomaly)
        );

        if (revealIndex >= queuedAnomalies.size()) {
            finishReplayScan(serverLevel);
            return;
        }

        strikeTimer = calculateStrikeIntervalTicks();
        setChanged();
        sendData();
        updateComparatorOutputIfNeeded();
    }

    private void finishReplayScan(ServerLevel serverLevel) {
        generateMapEntries(serverLevel);
        scanRunning = false;
        awaitingScanResult = false;
        cooldownTicks = 0;
        strikeTimer = 0;
        playFeedbackSound(SoundEvents.LEVER_CLICK, 0.6F, 0.5F);
        queuedAnomalies.clear();
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
        return SeismogramMapService.createMap(serverLevel, worldPosition, mapEntries, exactClusters);
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
        float requiredSu = BASE_REQUIRED_SU + (getInstalledModuleCount() * MODULE_REQUIRED_SU);
        float absSpeed = Math.max(1.0F, Math.abs(getOperationalSpeed()));
        float impact = requiredSu / absSpeed;
        lastStressApplied = impact;
        return impact;
    }

    private int calculateStrikeIntervalTicks() {
        int intervalMultiplier = Math.max(1, Config.STATION_STRIKE_INTERVAL_MULTIPLIER.get());
        int baseInterval = Config.STATION_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier;
        int minInterval = Math.min(baseInterval, Config.STATION_MIN_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier);
        return Math.max(1, minInterval);
    }

    private void tickClientStrikeAnimation() {
        if (clientStrikeProgressTicks >= 0.0F) {
            clientStrikeProgressTicks++;
            if (clientStrikeProgressTicks >= Math.max(1, clientStrikeIntervalTicks)) {
                clientStrikeProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
            }
        }
        if (clientSwingProgressTicks >= 0.0F) {
            clientSwingProgressTicks++;
            if (clientSwingProgressTicks >= Math.max(1, clientSwingDurationTicks)) {
                clientSwingProgressTicks = NO_CLIENT_STRIKE_PROGRESS;
            }
        }
    }

    private void startClientStrikeCycle(int intervalTicks) {
        clientStrikeIntervalTicks = Math.max(1, intervalTicks);
        clientStrikeProgressTicks = 0.0F;
    }

    private void startClientSwingCycle(int durationTicks) {
        clientSwingDurationTicks = Math.max(1, durationTicks);
        clientSwingProgressTicks = 0.0F;
    }

    private static int clientSwingDurationFor(int strikeIntervalTicks) {
        return Mth.clamp(Math.round(strikeIntervalTicks * 0.8F), 8, 20);
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
        compound.putInt(CURRENT_SCAN_DEPTH_TAG, currentScanDepth);

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

        ListTag exactTag = new ListTag();
        for (SeismogramMapService.ExactCluster cluster : exactClusters) {
            CompoundTag clusterTag = new CompoundTag();
            clusterTag.putString("Type", cluster.type().name());
            ListTag blockList = new ListTag();
            for (BlockPos blockPos : cluster.blocks()) {
                CompoundTag blockTag = new CompoundTag();
                blockTag.putInt("X", blockPos.getX());
                blockTag.putInt("Y", blockPos.getY());
                blockTag.putInt("Z", blockPos.getZ());
                blockList.add(blockTag);
            }
            clusterTag.put("Blocks", blockList);
            exactTag.add(clusterTag);
        }
        compound.put(EXACT_CLUSTERS_TAG, exactTag);
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
        currentScanDepth = Math.max(1, compound.getInt(CURRENT_SCAN_DEPTH_TAG));

        mapEntries.clear();
        ListTag entriesTag = compound.getList(MAP_ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag tag : entriesTag) {
            CompoundTag entryTag = (CompoundTag) tag;
            SeismicAnomalyType type = SeismicAnomalyType.valueOf(entryTag.getString("Type"));
            mapEntries.add(new MapEntry(type, entryTag.getInt("X"), entryTag.getInt("Z"), entryTag.getInt("Y")));
        }

        exactClusters.clear();
        ListTag exactTag = compound.getList(EXACT_CLUSTERS_TAG, Tag.TAG_COMPOUND);
        for (Tag rawCluster : exactTag) {
            CompoundTag clusterTag = (CompoundTag) rawCluster;
            SeismicAnomalyType type = SeismicAnomalyType.valueOf(clusterTag.getString("Type"));
            ListTag blockList = clusterTag.getList("Blocks", Tag.TAG_COMPOUND);
            List<BlockPos> blocks = new ArrayList<>(blockList.size());
            for (Tag rawBlock : blockList) {
                CompoundTag blockTag = (CompoundTag) rawBlock;
                blocks.add(new BlockPos(blockTag.getInt("X"), blockTag.getInt("Y"), blockTag.getInt("Z")));
            }
            if (!blocks.isEmpty()) {
                exactClusters.add(new SeismogramMapService.ExactCluster(type, List.copyOf(blocks)));
            }
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
        EnumSet<SeismicAnomalyType> detectable = EnumSet.noneOf(SeismicAnomalyType.class);
        if (!hasNoiseCancellationModule()) {
            detectable.add(SeismicAnomalyType.CAVE);
            detectable.add(SeismicAnomalyType.WATER);
            detectable.add(SeismicAnomalyType.LAVA);
        }
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            SeismicAnomalyType detects = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(slot));
            if (detects != null) {
                detectable.add(detects);
            }
        }
        return detectable;
    }

    private boolean hasInstalledModules() {
        return getInstalledModuleCount() > 0;
    }

    private boolean hasModuleInstalled(SeismicModuleType moduleType) {
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            SeismicModuleType installed = SeismicModuleItem.getModuleType(inventory.getStackInSlot(slot));
            if (installed == moduleType) {
                return true;
            }
        }
        return false;
    }

    private int getInstalledModuleCount() {
        int count = 0;
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int findEmptyModuleSlot() {
        for (int slot = SLOT_MODULE_START; slot <= SLOT_MODULE_END; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private int findLastFilledModuleSlot() {
        for (int slot = SLOT_MODULE_END; slot >= SLOT_MODULE_START; slot--) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isModuleSlot(int slot) {
        return slot >= SLOT_MODULE_START && slot <= SLOT_MODULE_END;
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
