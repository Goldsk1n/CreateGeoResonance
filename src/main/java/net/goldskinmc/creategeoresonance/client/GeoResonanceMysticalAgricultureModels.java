package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class GeoResonanceMysticalAgricultureModels {
    private static final List<SeismicModuleType> MODULES = List.of(
        SeismicModuleType.INFERIUM, SeismicModuleType.PROSPERITY, SeismicModuleType.SOULIUM);

    private GeoResonanceMysticalAgricultureModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceMysticalAgricultureModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceMysticalAgricultureModels::onModifyBakingResult);
    }

    private static ModelResourceLocation iconModel(SeismicModuleType module) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/mysticalagriculture/" + module.itemName()));
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
                throw new IllegalStateException("Missing registered Mystical Agriculture module model: " + module.itemName());
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, module.itemName());
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
