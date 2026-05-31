package net.goldskinmc.creategeoresonance.registry;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicPressureStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class GeoResonanceCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateGeoResonanceMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.creategeoresonance"))
        .icon(GeoResonanceCreativeTabs::createCreativeTabIconStack)
        .displayItems((parameters, output) -> {
            output.accept(createFilledHammerStack());
            output.accept(GeoResonanceItems.BELOW_ZERO_MODULE.get());
            output.accept(GeoResonanceItems.NOISE_CANCELLATION_MODULE.get());
            output.accept(GeoResonanceItems.AMETHYST_MODULE.get());
            output.accept(GeoResonanceItems.CHEST_MODULE.get());
            output.accept(GeoResonanceItems.SPAWNER_MODULE.get());
            output.accept(GeoResonanceItems.COAL_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.IRON_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.COPPER_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.GOLD_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.REDSTONE_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.LAPIS_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.EMERALD_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.DIAMOND_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.ZINC_RESONANCE_MODULE.get());
            output.accept(GeoResonanceBlocks.SEISMIC_STATION.get().asItem());
            output.accept(GeoResonanceBlocks.SEISMIC_PROJECTOR.get().asItem());
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
        stack.getOrCreateTag().putBoolean(SeismicHammerItem.HIDE_PRESSURE_BAR_TAG, true);
        return stack;
    }
}
