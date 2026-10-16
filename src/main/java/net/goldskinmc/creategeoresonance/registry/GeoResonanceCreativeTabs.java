package net.goldskinmc.creategeoresonance.registry;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
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
            output.accept(GeoResonanceItems.AMETHYST_MODULE.get());
            output.accept(GeoResonanceItems.CHEST_MODULE.get());
            output.accept(GeoResonanceItems.SPAWNER_MODULE.get());
            output.accept(GeoResonanceItems.LAVA_MODULE.get());
            output.accept(GeoResonanceItems.COAL_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.IRON_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.COPPER_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.GOLD_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.REDSTONE_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.LAPIS_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.EMERALD_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.DIAMOND_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.ZINC_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.CRIMSITE_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.ASURINE_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.OCHRUM_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.VERIDIUM_RESONANCE_MODULE.get());
            if (SeismicModuleType.THORIUM.isAvailable()) {
                output.accept(GeoResonanceItems.THORIUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.MAGNETITE.isAvailable()) {
                output.accept(GeoResonanceItems.MAGNETITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.LEAD.isAvailable()) {
                output.accept(GeoResonanceItems.LEAD_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.NICKEL.isAvailable()) {
                output.accept(GeoResonanceItems.NICKEL_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.LITHIUM.isAvailable()) {
                output.accept(GeoResonanceItems.LITHIUM_RESONANCE_MODULE.get());
                output.accept(GeoResonanceItems.BAUXITE_RESONANCE_MODULE.get());
                output.accept(GeoResonanceItems.GALENA_RESONANCE_MODULE.get());
                output.accept(GeoResonanceItems.LIGNITE_RESONANCE_MODULE.get());
                output.accept(GeoResonanceItems.SULFUR_RESONANCE_MODULE.get());
                output.accept(GeoResonanceItems.CRUDE_OIL_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.WOLFRAMITE.isAvailable()) {
                output.accept(GeoResonanceItems.WOLFRAMITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.URANIUM.isAvailable()) {
                output.accept(GeoResonanceItems.URANIUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.AUTUNITE.isAvailable()) {
                output.accept(GeoResonanceItems.AUTUNITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.NITRATE.isAvailable()) {
                output.accept(GeoResonanceItems.NITRATE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.TIN.isAvailable()) {
                output.accept(GeoResonanceItems.TIN_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.SILVER.isAvailable()) {
                output.accept(GeoResonanceItems.SILVER_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.PLATINUM.isAvailable()) {
                output.accept(GeoResonanceItems.PLATINUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.OSMIUM.isAvailable()) {
                output.accept(GeoResonanceItems.OSMIUM_RESONANCE_MODULE.get());
            }
            output.accept(GeoResonanceItems.QUARTZ_RESONANCE_MODULE.get());
            output.accept(GeoResonanceItems.ANCIENT_DEBRIS_RESONANCE_MODULE.get());
            if (SeismicModuleType.FLUORITE.isAvailable()) {
                output.accept(GeoResonanceItems.FLUORITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.CERTUS_QUARTZ.isAvailable()) {
                output.accept(GeoResonanceItems.CERTUS_QUARTZ_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.INFERIUM.isAvailable()) {
                output.accept(GeoResonanceItems.INFERIUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.PROSPERITY.isAvailable()) {
                output.accept(GeoResonanceItems.PROSPERITY_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.SOULIUM.isAvailable()) {
                output.accept(GeoResonanceItems.SOULIUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.ARCANE_CRYSTAL.isAvailable()) {
                output.accept(GeoResonanceItems.ARCANE_CRYSTAL_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.RUNE.isAvailable()) {
                output.accept(GeoResonanceItems.RUNE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.STELLARITE.isAvailable()) {
                output.accept(GeoResonanceItems.STELLARITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.URANINITE.isAvailable()) {
                output.accept(GeoResonanceItems.URANINITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.DRACONIUM.isAvailable()) {
                output.accept(GeoResonanceItems.DRACONIUM_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.XPETRIFIED.isAvailable()) {
                output.accept(GeoResonanceItems.XPETRIFIED_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.COBALT.isAvailable()) {
                output.accept(GeoResonanceItems.COBALT_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.APATITE.isAvailable()) {
                output.accept(GeoResonanceItems.APATITE_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.CINNABAR.isAvailable()) {
                output.accept(GeoResonanceItems.CINNABAR_RESONANCE_MODULE.get());
            }
            if (SeismicModuleType.NITER.isAvailable()) {
                output.accept(GeoResonanceItems.NITER_RESONANCE_MODULE.get());
            }
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
