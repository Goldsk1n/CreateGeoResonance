package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class GeoResonanceForbiddenArcanusModels {
    private static final List<SeismicModuleType> MODULES = List.of(
        SeismicModuleType.ARCANE_CRYSTAL, SeismicModuleType.RUNE, SeismicModuleType.STELLARITE);

    private GeoResonanceForbiddenArcanusModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceForbiddenArcanusModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceForbiddenArcanusModels::onModifyBakingResult);
    }

    private static ModelResourceLocation iconModel(SeismicModuleType module) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/forbidden_arcanus/" + module.itemName()));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (SeismicModuleType module : MODULES) {
            if (module.isAvailable()) {
                event.register(iconModel(module));
            }
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (SeismicModuleType module : MODULES) {
            if (!module.isAvailable()) {
                continue;
            }
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Forbidden & Arcanus module model: " + module.itemName());
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, module.itemName());
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
