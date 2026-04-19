package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class SeismicPressureStorage {
    public static final String STORED_PRESSURE_TAG = "StoredPressure";

    private SeismicPressureStorage() {
    }

    public static float maxPressure() {
        return Math.max(1.0F, BacktankUtil.maxAirWithoutEnchants());
    }

    public static float getStoredPressure(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(STORED_PRESSURE_TAG)) {
            return 0.0F;
        }
        return Mth.clamp(tag.getFloat(STORED_PRESSURE_TAG), 0.0F, maxPressure());
    }

    public static void setStoredPressure(ItemStack stack, float pressure) {
        stack.getOrCreateTag().putFloat(STORED_PRESSURE_TAG, Mth.clamp(pressure, 0.0F, maxPressure()));
    }
}
