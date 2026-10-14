package net.goldskinmc.creategeoresonance.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class GeoResonanceThermalModels {
    private GeoResonanceThermalModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceThermalModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceThermalModels::onModifyBakingResult);
    }

    static boolean hasIconItem(String mineral) {
        String item = switch (mineral) {
            case "tin", "lead", "nickel", "silver" -> "raw_" + mineral;
            case "crude_oil" -> "bitumen";
            default -> mineral;
        };
        return ModList.get().isLoaded("thermal_foundation") && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath("thermal", item));
    }

    private static boolean hasItem(String modId, String item) {
        return ModList.get().isLoaded(modId) && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath(modId, item));
    }

    private static List<String> activeModules() {
        List<String> modules = new ArrayList<>();
        for (String mineral : List.of("tin", "lead", "nickel", "silver", "sulfur", "crude_oil",
                "apatite", "cinnabar", "niter")) {
            if (!hasIconItem(mineral)) {
                continue;
            }
            boolean primaryIconAvailable = switch (mineral) {
                case "lead" -> ModList.get().isLoaded("tfmg") || hasItem("createnuclear", "raw_lead")
                    || hasItem("mekanism", "raw_lead") || GeoResonanceOreganizedModels.hasIconItem("lead");
                case "tin" -> hasItem("mekanism", "raw_tin");
                case "silver" -> GeoResonanceOreganizedModels.hasIconItem("silver");
                case "nickel", "sulfur", "crude_oil" -> ModList.get().isLoaded("tfmg");
                default -> false;
            };
            if (!primaryIconAvailable) {
                modules.add(mineral);
            }
        }
        return modules;
    }

    private static ResourceLocation iconModel(String module) {
        return ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/thermal/" + module + "_resonance_module");
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Optional native textures are requested only when their provider's items exist.
        for (String module : activeModules()) {
            event.register(iconModel(module));
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (String module : activeModules()) {
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Thermal module model: " + module);
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
                module + "_resonance_module");
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
