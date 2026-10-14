package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonanceAE2Models {
    private GeoResonanceAE2Models() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceAE2Models::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceAE2Models::onModifyBakingResult);
    }

    private static ModelResourceLocation iconModel() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/ae2/certus_quartz_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        if (SeismicModuleType.CERTUS_QUARTZ.isAvailable()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!SeismicModuleType.CERTUS_QUARTZ.isAvailable()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered AE2 Certus Quartz module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "certus_quartz_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
