package net.goldskinmc.creategeoresonance.registry;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.ItemStackComponentData;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicPressureStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GeoResonanceCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateGeoResonanceMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.creategeoresonance"))
        .icon(GeoResonanceCreativeTabs::createCreativeTabIconStack)
        .displayItems((parameters, output) -> {
        })
        .build());

    private GeoResonanceCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    public static ItemStack createFilledHammerStack() {
        ItemStack stack = new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get());
        SeismicPressureStorage.setStoredPressure(stack, SeismicPressureStorage.maxPressure());
        return stack;
    }

    private static ItemStack createCreativeTabIconStack() {
        ItemStack stack = createFilledHammerStack();
        ItemStackComponentData.putBoolean(stack, SeismicHammerItem.HIDE_PRESSURE_BAR_TAG, true);
        return stack;
    }
}
