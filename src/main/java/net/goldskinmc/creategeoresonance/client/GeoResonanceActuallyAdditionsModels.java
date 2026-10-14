package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonanceActuallyAdditionsModels {
    private GeoResonanceActuallyAdditionsModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceActuallyAdditionsModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceActuallyAdditionsModels::onModifyBakingResult);
    }

    private static ModelResourceLocation iconModel() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/actuallyadditions/black_quartz_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        if (SeismicModuleType.BLACK_QUARTZ.isAvailable()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!SeismicModuleType.BLACK_QUARTZ.isAvailable()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered Actually Additions Black Quartz module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "black_quartz_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
