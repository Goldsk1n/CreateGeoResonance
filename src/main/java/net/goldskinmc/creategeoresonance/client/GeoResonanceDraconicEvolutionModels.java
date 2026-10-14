package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonanceDraconicEvolutionModels {
    private GeoResonanceDraconicEvolutionModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceDraconicEvolutionModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceDraconicEvolutionModels::onModifyBakingResult);
    }

    private static ModelResourceLocation iconModel() {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/draconicevolution/draconium_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        if (SeismicModuleType.DRACONIUM.isAvailable()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!SeismicModuleType.DRACONIUM.isAvailable()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered Draconic Evolution Draconium module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "draconium_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
