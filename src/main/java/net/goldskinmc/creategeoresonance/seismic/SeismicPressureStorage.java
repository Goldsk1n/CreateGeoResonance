package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.goldskinmc.creategeoresonance.ItemStackComponentData;
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
        return Mth.clamp(ItemStackComponentData.getFloat(stack, STORED_PRESSURE_TAG, 0.0F), 0.0F, maxPressure());
    }

    public static void setStoredPressure(ItemStack stack, float pressure) {
        ItemStackComponentData.putFloat(stack, STORED_PRESSURE_TAG, Mth.clamp(pressure, 0.0F, maxPressure()));
    }
}
