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

public final class GeoResonanceImmersiveEngineeringModels {
    private GeoResonanceImmersiveEngineeringModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceImmersiveEngineeringModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceImmersiveEngineeringModels::onModifyBakingResult);
    }

    static boolean hasIconItem(String mineral) {
        String metal = mineral.equals("bauxite") ? "aluminum" : mineral;
        return hasItem("immersiveengineering", "raw_" + metal);
    }

    private static boolean hasItem(String modId, String item) {
        return ModList.get().isLoaded(modId) && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath(modId, item));
    }

    private static List<String> activeModules() {
        List<String> modules = new ArrayList<>();
        for (String mineral : List.of("lead", "nickel", "silver", "uranium", "bauxite")) {
            if (!hasIconItem(mineral)) {
                continue;
            }
            boolean primaryIconAvailable = switch (mineral) {
                case "lead" -> ModList.get().isLoaded("tfmg") || hasItem("createnuclear", "raw_lead")
                    || hasItem("mekanism", "raw_lead") || GeoResonanceOreganizedModels.hasIconItem("lead");
                case "nickel" -> ModList.get().isLoaded("tfmg");
                case "silver" -> GeoResonanceOreganizedModels.hasIconItem("silver");
                case "uranium" -> hasItem("createnuclear", "raw_uranium") || hasItem("mekanism", "raw_uranium");
                case "bauxite" -> ModList.get().isLoaded("tfmg");
                default -> false;
            };
            if (!primaryIconAvailable) {
                modules.add(mineral);
            }
        }
        return modules;
    }

    private static ModelResourceLocation iconModel(String module) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/immersiveengineering/" + module + "_resonance_module"));
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (String module : activeModules()) {
            event.register(iconModel(module));
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (String module : activeModules()) {
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Immersive Engineering module model: " + module);
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
                module + "_resonance_module");
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
