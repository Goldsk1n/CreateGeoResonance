package net.goldskinmc.creategeoresonance;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class ItemStackComponentData {
    private ItemStackComponentData() {
    }

    @Nullable
    public static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : null;
    }

    public static boolean contains(ItemStack stack, String key, int tagType) {
        CompoundTag tag = getCustomData(stack);
        return tag != null && tag.contains(key, tagType);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag tag = getCustomData(stack);
        return tag != null && tag.getBoolean(key);
    }

    public static float getFloat(ItemStack stack, String key, float defaultValue) {
        CompoundTag tag = getCustomData(stack);
        if (tag == null || !tag.contains(key, Tag.TAG_FLOAT)) {
            return defaultValue;
        }
        return tag.getFloat(key);
    }

    @Nullable
    public static CompoundTag getCompound(ItemStack stack, String key) {
        CompoundTag tag = getCustomData(stack);
        if (tag == null || !tag.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(key);
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(key, value));
    }

    public static void putFloat(ItemStack stack, String key, float value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(key, value));
    }

    public static void putCompound(ItemStack stack, String key, CompoundTag value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(key, value.copy()));
    }
}
