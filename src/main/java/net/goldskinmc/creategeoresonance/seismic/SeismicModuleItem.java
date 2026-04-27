package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SeismicModuleItem extends Item {
    private final SeismicModuleType moduleType;

    public SeismicModuleItem(Properties properties, SeismicModuleType moduleType) {
        super(properties);
        this.moduleType = moduleType;
    }

    public SeismicModuleType moduleType() {
        return moduleType;
    }

    @Nullable
    public SeismicAnomalyType detectsType() {
        return moduleType.detectedType();
    }

    @Nullable
    public static SeismicModuleType getModuleType(ItemStack stack) {
        if (stack.getItem() instanceof SeismicModuleItem moduleItem) {
            return moduleItem.moduleType();
        }
        return null;
    }

    @Nullable
    public static SeismicAnomalyType getDetectsType(ItemStack stack) {
        if (stack.getItem() instanceof SeismicModuleItem moduleItem) {
            return moduleItem.detectsType();
        }
        return null;
    }
}
