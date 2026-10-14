package net.goldskinmc.creategeoresonance.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class GeoResonanceMekanismModels {
    private GeoResonanceMekanismModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceMekanismModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceMekanismModels::onModifyBakingResult);
    }

    private static boolean hasItem(String modId, String item) {
        return ModList.get().isLoaded(modId) && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath(modId, item));
    }

    private static List<String> activeModules() {
        List<String> modules = new ArrayList<>();
        for (String resource : List.of("tin", "osmium", "fluorite", "lead", "uranium")) {
            String iconItem = resource.equals("fluorite") ? "fluorite_gem" : "raw_" + resource;
            if (!hasItem("mekanism", iconItem)) {
                continue;
            }
            boolean primaryIconAvailable = switch (resource) {
                case "lead" -> ModList.get().isLoaded("tfmg") || hasItem("createnuclear", "raw_lead");
                case "uranium" -> hasItem("createnuclear", "raw_uranium");
                default -> false;
            };
            if (!primaryIconAvailable) {
                modules.add(resource);
            }
        }
        return modules;
    }

    private static ModelResourceLocation iconModel(String module) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/mekanism/" + module + "_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Native icon textures are requested only when their matching optional items exist.
        for (String module : activeModules()) {
            event.register(iconModel(module));
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (String module : activeModules()) {
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Mekanism module model: " + module);
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
                module + "_resonance_module");
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
