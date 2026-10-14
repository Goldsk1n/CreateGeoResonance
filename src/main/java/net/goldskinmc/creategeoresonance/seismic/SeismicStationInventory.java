package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

class SeismicStationInventory extends ItemStackHandler {
    private static final String RECOVERED_MODULES_TAG = "RecoveredModules";
    private final List<ItemStack> recoveredModules = new ArrayList<>();

    SeismicStationInventory() {
        super(SeismicStationBlockEntity.SLOT_MODULE_START + SeismicStationBlockEntity.MODULE_SLOT_COUNT);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SeismicStationBlockEntity.SLOT_PAPER_INPUT) {
            return stack.is(Items.PAPER);
        }
        if (slot == SeismicStationBlockEntity.SLOT_INK_INPUT) {
            return stack.is(Items.INK_SAC);
        }
        if (SeismicStationControllerLogic.isModuleSlot(slot)) {
            SeismicModuleType type = SeismicModuleItem.getModuleType(stack);
            return type != null && !type.isRetired();
        }
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    ItemStack takeRecoveredModule() {
        if (recoveredModules.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = recoveredModules.remove(recoveredModules.size() - 1);
        onContentsChanged(SeismicStationBlockEntity.SLOT_MODULE_START);
        return stack;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = super.serializeNBT(registries);
        ListTag recoveredTag = new ListTag();
        for (ItemStack stack : recoveredModules) {
            recoveredTag.add(stack.save(registries));
        }
        tag.put(RECOVERED_MODULES_TAG, recoveredTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        stacks = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
        recoveredModules.clear();
        for (Tag entry : nbt.getList(RECOVERED_MODULES_TAG, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) entry);
            if (!stack.isEmpty()) {
                recoveredModules.add(stack);
            }
        }

        List<ItemStack> overflowModules = new ArrayList<>();
        for (Tag entry : nbt.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag itemTag = (CompoundTag) entry;
            int slot = itemTag.getInt("Slot");
            ItemStack stack = ItemStack.parseOptional(registries, itemTag);
            SeismicModuleType type = SeismicModuleItem.getModuleType(stack);
            // Preserve retired modules outside the eight active slots, including old 16-slot inventories.
            if (slot >= SeismicStationBlockEntity.SLOT_MODULE_START
                && slot < SeismicStationBlockEntity.SLOT_MODULE_START + 16
                && type != null && type.isRetired()) {
                recoveredModules.add(stack);
            } else if (slot >= 0 && slot < getSlots()) {
                stacks.set(slot, stack);
            } else if (slot > SeismicStationBlockEntity.SLOT_MODULE_END
                && slot < SeismicStationBlockEntity.SLOT_MODULE_START + 16 && type != null) {
                overflowModules.add(stack);
            }
        }
        for (ItemStack stack : overflowModules) {
            int slot = SeismicStationControllerLogic.findEmptyModuleSlot(this);
            if (slot >= 0) {
                stacks.set(slot, stack.copyWithCount(1));
                if (stack.getCount() > 1) {
                    recoveredModules.add(stack.copyWithCount(stack.getCount() - 1));
                }
            } else {
                recoveredModules.add(stack);
            }
        }
        onLoad();
    }
}
