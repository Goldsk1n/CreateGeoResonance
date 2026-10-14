package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePropulsionModels {
    private GeoResonancePropulsionModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonancePropulsionModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonancePropulsionModels::onModifyBakingResult);
    }

    private static boolean hasIconItem() {
        return ModList.get().isLoaded("createpropulsion") && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath("createpropulsion", "raw_platinum"));
    }

    private static ModelResourceLocation iconModel() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/propulsion/platinum_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Do not request optional native textures when their provider is absent.
        if (hasIconItem()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!hasIconItem()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered Propulsion Platinum module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "platinum_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
