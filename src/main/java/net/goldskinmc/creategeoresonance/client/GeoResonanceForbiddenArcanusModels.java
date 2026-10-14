package net.goldskinmc.creategeoresonance.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class GeoResonanceForbiddenArcanusModels {
    private static final List<SeismicModuleType> MODULES = List.of(
        SeismicModuleType.ARCANE_CRYSTAL, SeismicModuleType.RUNE,
        SeismicModuleType.STELLARITE, SeismicModuleType.XPETRIFIED);

    private GeoResonanceForbiddenArcanusModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceForbiddenArcanusModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceForbiddenArcanusModels::onModifyBakingResult);
    }

    private static ResourceLocation iconModel(SeismicModuleType module) {
        return ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/forbidden_arcanus/" + module.itemName());
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
