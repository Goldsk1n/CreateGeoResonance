package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class SeismicCustomModuleItem extends SeismicModuleItem {
    public static final String MODULE_ID_TAG = "GeoModule";

    public SeismicCustomModuleItem(Properties properties) {
        super(properties, SeismicModuleType.CUSTOM);
    }

    public static ItemStack create(ResourceLocation id) {
        ItemStack stack = new ItemStack(GeoResonanceItems.CUSTOM_MODULE.get());
        stack.getOrCreateTag().putString(MODULE_ID_TAG, id.toString());
        return stack;
    }

    @Nullable
    public static SeismicAnomalyType target(ItemStack stack) {
        if (!(stack.getItem() instanceof SeismicCustomModuleItem)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation id = tag == null ? null : ResourceLocation.tryParse(tag.getString(MODULE_ID_TAG));
        return id != null ? SeismicAnomalyType.custom(id) : null;
    }

    public static boolean isConfigured(ItemStack stack) {
        SeismicAnomalyType target = target(stack);
        return target != null && SeismicCustomModules.definition(target) != null;
    }

    @Override
    public Component getName(ItemStack stack) {
        SeismicAnomalyType type = target(stack);
        SeismicCustomModuleDefinition definition = type == null ? null : SeismicCustomModules.definition(type);
        return definition != null ? definition.displayName() : type != null
            ? Component.translatable("item.creategeoresonance.custom_module.unavailable", type.name()) : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        SeismicAnomalyType type = target(stack);
        if (type == null || SeismicCustomModules.definition(type) == null) {
            tooltip.add(Component.translatable("item.creategeoresonance.custom_module.missing").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.creategeoresonance.custom_module.detects", SeismicCustomModules.signalName(type))
                .withStyle(ChatFormatting.GRAY));
        }
        if (type != null && flag.isAdvanced()) {
            tooltip.add(Component.literal(type.name()).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
