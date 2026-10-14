package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePneumaticCraftModels {
    private GeoResonancePneumaticCraftModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonancePneumaticCraftModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonancePneumaticCraftModels::onModifyBakingResult);
    }

    private static boolean useNativeIcon() {
        return ModList.get().isLoaded("pneumaticcraft")
            && BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "oil_bucket"))
            && !ModList.get().isLoaded("tfmg");
    }

    private static ModelResourceLocation iconModel() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/pneumaticcraft/crude_oil_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        if (useNativeIcon()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!useNativeIcon()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered PneumaticCraft Crude Oil module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "crude_oil_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
