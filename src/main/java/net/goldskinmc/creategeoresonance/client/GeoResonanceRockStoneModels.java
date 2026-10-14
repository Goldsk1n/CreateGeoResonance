package net.goldskinmc.creategeoresonance.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.seismic.SeismicRockStoneDeposits;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class GeoResonanceRockStoneModels {
    private GeoResonanceRockStoneModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceRockStoneModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceRockStoneModels::onModifyBakingResult);
    }

    private static boolean hasNuclearItem(String item) {
        return ModList.get().isLoaded("createnuclear") && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath("createnuclear", item));
    }

    private static boolean hasMekanismItem(String item) {
        return ModList.get().isLoaded("mekanism") && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath("mekanism", item));
    }

    private static List<String> activeModules() {
        List<String> modules = new ArrayList<>();
        for (String mineral : List.of("tin", "silver", "platinum", "osmium", "cobalt",
                "lead", "nickel", "uranium", "thorium", "wolframite")) {
            if (!SeismicRockStoneDeposits.hasDepositBlock(mineral)) {
                continue;
            }
            boolean primaryIconAvailable = switch (mineral) {
                case "lead" -> ModList.get().isLoaded("tfmg") || hasNuclearItem("raw_lead")
                    || hasMekanismItem("raw_lead") || GeoResonanceOreganizedModels.hasIconItem("lead")
                    || GeoResonanceThermalModels.hasIconItem("lead")
                    || GeoResonanceImmersiveEngineeringModels.hasIconItem("lead");
                case "nickel" -> ModList.get().isLoaded("tfmg") || GeoResonanceThermalModels.hasIconItem("nickel")
                    || GeoResonanceImmersiveEngineeringModels.hasIconItem("nickel");
                case "uranium" -> hasNuclearItem("raw_uranium") || hasMekanismItem("raw_uranium")
                    || GeoResonanceImmersiveEngineeringModels.hasIconItem("uranium");
                case "tin" -> hasMekanismItem("raw_tin") || GeoResonanceThermalModels.hasIconItem("tin");
                case "osmium" -> hasMekanismItem("raw_osmium");
                case "cobalt" -> ModList.get().isLoaded("tconstruct") && BuiltInRegistries.ITEM.containsKey(
                    ResourceLocation.fromNamespaceAndPath("tconstruct", "raw_cobalt"));
                case "thorium" -> ModList.get().isLoaded("create_new_age") || hasNuclearItem("raw_thorium");
                case "wolframite" -> ModList.get().isLoaded("createmetallurgy");
                case "silver" -> GeoResonanceOreganizedModels.hasIconItem("silver")
                    || GeoResonanceThermalModels.hasIconItem("silver")
                    || GeoResonanceImmersiveEngineeringModels.hasIconItem("silver");
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
            "item/compat/rock_stone/" + module + "_resonance_module");
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Optional textures depend on installed providers, not on world item tags.
        for (String module : activeModules()) {
            event.register(iconModel(module));
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (String module : activeModules()) {
            BakedModel model = event.getModels().get(iconModel(module));
            if (model == null) {
                throw new IllegalStateException("Missing registered Rock & Stone module model: " + module);
            }
            ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
                module + "_resonance_module");
            event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
        }
    }
}
