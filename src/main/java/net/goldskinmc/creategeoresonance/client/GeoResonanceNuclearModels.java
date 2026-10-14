package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

public final class GeoResonanceNuclearModels {
    private GeoResonanceNuclearModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceNuclearModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceNuclearModels::onModifyBakingResult);
    }

    private static boolean hasItem(String path) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("createnuclear", path));
    }

    private static List<String> activeModules() {
        if (!ModList.get().isLoaded("createnuclear")) {
            return List.of();
        }
        List<String> modules = new ArrayList<>();
        if (hasItem("raw_uranium")) {
            modules.add("uranium");
        }
        if (hasItem("autunite")) {
            modules.add("autunite");
        }
        if (hasItem("nitrate")) {
            modules.add("nitrate");
        }
        if (!ModList.get().isLoaded("create_new_age") && hasItem("raw_thorium")) {
            modules.add("thorium");
        }
        if (!ModList.get().isLoaded("tfmg") && hasItem("raw_lead")) {
            modules.add("lead");
        }
        return modules;
    }

    private static ModelResourceLocation iconModel(String module) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/nuclear/" + module + "_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Only request Nuclear's textures when its matching items are present.
        for (String module : activeModules()) {
            event.register(iconModel(module));
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (String module : activeModules()) {
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Nuclear module model: " + module);
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
                module + "_resonance_module");
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
