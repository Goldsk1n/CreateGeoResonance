package net.goldskinmc.creategeoresonance.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePowahModels {
    private GeoResonancePowahModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonancePowahModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonancePowahModels::onModifyBakingResult);
    }

    private static ResourceLocation iconModel() {
        return ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/powah/uraninite_resonance_module");
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        if (SeismicModuleType.URANINITE.isAvailable()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!SeismicModuleType.URANINITE.isAvailable()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered Powah Uraninite module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "uraninite_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
