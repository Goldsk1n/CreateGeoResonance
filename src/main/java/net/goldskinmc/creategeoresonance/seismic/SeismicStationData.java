package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SeismicStationData {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String MAP_ENTRIES_TAG = "MapEntries";
    private static final String EXACT_CLUSTERS_TAG = "ExactClusters";
    private static final String QUEUED_ANOMALIES_TAG = "QueuedAnomalies";
    private static final String MAP_READY_TAG = "MapReady";
    private static final String SCAN_RUNNING_TAG = "ScanRunning";
    private static final String AWAITING_RESULT_TAG = "AwaitingResult";
    private static final String STRIKE_TIMER_TAG = "StrikeTimer";
    private static final String REVEAL_INDEX_TAG = "RevealIndex";
    private static final String COOLDOWN_TAG = "CooldownTicks";
    private static final String CURRENT_SCAN_DEPTH_TAG = "CurrentScanDepth";
    private static final String SCAN_ORIGIN_X_TAG = "ScanOriginX";
    private static final String SCAN_ORIGIN_Y_TAG = "ScanOriginY";
    private static final String SCAN_ORIGIN_Z_TAG = "ScanOriginZ";

    private final ItemStackHandler inventory = new ItemStackHandler(
        SeismicStationBlockEntity.SLOT_MODULE_START + SeismicStationBlockEntity.MODULE_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SeismicStationBlockEntity.SLOT_PAPER_INPUT) {
                return stack.is(Items.PAPER);
            }
            if (slot == SeismicStationBlockEntity.SLOT_INK_INPUT) {
                return stack.is(Items.INK_SAC);
            }
            if (SeismicStationControllerLogic.isModuleSlot(slot)) {
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
                } else if (slot > SeismicStationBlockEntity.SLOT_MODULE_END
                    && slot < SeismicStationBlockEntity.SLOT_MODULE_START + 16
                    && SeismicModuleItem.getModuleType(stack) != null) {
                    overflowModules.add(stack);
                }
            }

            if (!overflowModules.isEmpty()) {
                for (ItemStack moduleStack : overflowModules) {
                    int emptySlot = SeismicStationControllerLogic.findEmptyModuleSlot(this);
                    if (emptySlot >= 0) {
                        stacks.set(emptySlot, moduleStack.copyWithCount(1));
                    }
                }
            }
        }
    };

    private final List<SeismicAnomaly> queuedAnomalies = new ArrayList<>();
    private final List<SeismicStationBlockEntity.MapEntry> mapEntries = new ArrayList<>();
    private final List<SeismogramMapService.ExactCluster> exactClusters = new ArrayList<>();

    private boolean scanRunning;
    private boolean awaitingScanResult;
    private boolean mapReady;
    private int strikeTimer;
    private int revealIndex;
    private int cooldownTicks;
    private int currentScanDepth;
    @Nullable
    private BlockPos scanOrigin;

    public ItemStackHandler inventory() {
        return inventory;
    }

    public List<SeismicAnomaly> queuedAnomalies() {
        return queuedAnomalies;
    }

    public List<SeismicStationBlockEntity.MapEntry> mapEntries() {
        return mapEntries;
    }

    public List<SeismogramMapService.ExactCluster> exactClusters() {
        return exactClusters;
    }

    public boolean scanRunning() {
        return scanRunning;
    }

    public void setScanRunning(boolean scanRunning) {
        this.scanRunning = scanRunning;
    }

    public boolean awaitingScanResult() {
        return awaitingScanResult;
    }

    public void setAwaitingScanResult(boolean awaitingScanResult) {
        this.awaitingScanResult = awaitingScanResult;
    }

    public boolean mapReady() {
        return mapReady;
    }

    public void setMapReady(boolean mapReady) {
        this.mapReady = mapReady;
    }

    public int strikeTimer() {
        return strikeTimer;
    }

    public void setStrikeTimer(int strikeTimer) {
        this.strikeTimer = strikeTimer;
    }

    public int revealIndex() {
        return revealIndex;
    }

    public void setRevealIndex(int revealIndex) {
        this.revealIndex = revealIndex;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public int currentScanDepth() {
        return currentScanDepth;
    }

    public void setCurrentScanDepth(int currentScanDepth) {
        this.currentScanDepth = currentScanDepth;
    }

    @Nullable
    public BlockPos scanOrigin() {
        return scanOrigin;
    }

    public void setScanOrigin(@Nullable BlockPos scanOrigin) {
        this.scanOrigin = scanOrigin;
    }

    public void readFromTag(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        scanRunning = tag.getBoolean(SCAN_RUNNING_TAG);
        awaitingScanResult = tag.getBoolean(AWAITING_RESULT_TAG);
        mapReady = tag.getBoolean(MAP_READY_TAG);
        strikeTimer = tag.getInt(STRIKE_TIMER_TAG);
        revealIndex = tag.getInt(REVEAL_INDEX_TAG);
        cooldownTicks = tag.getInt(COOLDOWN_TAG);
        currentScanDepth = Math.max(1, tag.getInt(CURRENT_SCAN_DEPTH_TAG));
        scanOrigin = tag.contains(SCAN_ORIGIN_X_TAG, Tag.TAG_INT)
            && tag.contains(SCAN_ORIGIN_Y_TAG, Tag.TAG_INT)
            && tag.contains(SCAN_ORIGIN_Z_TAG, Tag.TAG_INT)
            ? new BlockPos(tag.getInt(SCAN_ORIGIN_X_TAG), tag.getInt(SCAN_ORIGIN_Y_TAG), tag.getInt(SCAN_ORIGIN_Z_TAG))
            : null;

        queuedAnomalies.clear();
        ListTag queuedTag = tag.getList(QUEUED_ANOMALIES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawQueued : queuedTag) {
            CompoundTag queuedEntry = (CompoundTag) rawQueued;
            SeismicAnomalyType type = SeismicAnomalyType.valueOf(queuedEntry.getString("Type"));
            queuedAnomalies.add(new SeismicAnomaly(
                type,
                queuedEntry.getInt("X"),
                queuedEntry.getInt("Z"),
                queuedEntry.getInt("Depth"),
                queuedEntry.getInt("Radius"),
                queuedEntry.getFloat("Confidence")
            ));
        }

        mapEntries.clear();
        ListTag entriesTag = tag.getList(MAP_ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag tagEntry : entriesTag) {
            CompoundTag entryTag = (CompoundTag) tagEntry;
            SeismicAnomalyType type = SeismicAnomalyType.valueOf(entryTag.getString("Type"));
            mapEntries.add(new SeismicStationBlockEntity.MapEntry(
                type,
                entryTag.getInt("X"),
                entryTag.getInt("Z"),
                entryTag.getInt("Y")
            ));
        }

        exactClusters.clear();
        ListTag exactTag = tag.getList(EXACT_CLUSTERS_TAG, Tag.TAG_COMPOUND);
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
    }

    public void writeToTag(CompoundTag tag) {
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        tag.putBoolean(SCAN_RUNNING_TAG, scanRunning);
        tag.putBoolean(AWAITING_RESULT_TAG, awaitingScanResult);
        tag.putBoolean(MAP_READY_TAG, mapReady);
        tag.putInt(STRIKE_TIMER_TAG, strikeTimer);
        tag.putInt(REVEAL_INDEX_TAG, revealIndex);
        tag.putInt(COOLDOWN_TAG, cooldownTicks);
        tag.putInt(CURRENT_SCAN_DEPTH_TAG, currentScanDepth);
        if (scanOrigin != null) {
            tag.putInt(SCAN_ORIGIN_X_TAG, scanOrigin.getX());
            tag.putInt(SCAN_ORIGIN_Y_TAG, scanOrigin.getY());
            tag.putInt(SCAN_ORIGIN_Z_TAG, scanOrigin.getZ());
        }

        ListTag queuedTag = new ListTag();
        for (SeismicAnomaly anomaly : queuedAnomalies) {
            CompoundTag queuedEntry = new CompoundTag();
            queuedEntry.putString("Type", anomaly.type().name());
            queuedEntry.putInt("X", anomaly.offsetX());
            queuedEntry.putInt("Z", anomaly.offsetZ());
            queuedEntry.putInt("Depth", anomaly.depth());
            queuedEntry.putInt("Radius", anomaly.radius());
            queuedEntry.putFloat("Confidence", anomaly.confidence());
            queuedTag.add(queuedEntry);
        }
        tag.put(QUEUED_ANOMALIES_TAG, queuedTag);

        ListTag entriesTag = new ListTag();
        for (SeismicStationBlockEntity.MapEntry entry : mapEntries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Type", entry.type().name());
            entryTag.putInt("X", entry.offsetX());
            entryTag.putInt("Z", entry.offsetZ());
            entryTag.putInt("Y", entry.approxY());
            entriesTag.add(entryTag);
        }
        tag.put(MAP_ENTRIES_TAG, entriesTag);

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
        tag.put(EXACT_CLUSTERS_TAG, exactTag);
    }
}
