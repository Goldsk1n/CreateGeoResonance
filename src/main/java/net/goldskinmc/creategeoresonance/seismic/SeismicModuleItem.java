package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SeismicModuleItem extends Item {
    private final SeismicAnomalyType detectsType;

    public SeismicModuleItem(Properties properties, SeismicAnomalyType detectsType) {
        super(properties);
        this.detectsType = detectsType;
    }

    public SeismicAnomalyType detectsType() {
        return detectsType;
    }

    @Nullable
    public static SeismicAnomalyType getDetectsType(ItemStack stack) {
        if (stack.getItem() instanceof SeismicModuleItem moduleItem) {
            return moduleItem.detectsType();
        }
        return null;
    }
}
