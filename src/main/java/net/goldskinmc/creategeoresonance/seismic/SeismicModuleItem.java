package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemDescription description = ItemDescription.create(this, Palette.STANDARD_CREATE);
        if (description == null) {
            return;
        }
        tooltip.addAll(description.getCurrentLines());
    }
}
