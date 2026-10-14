package net.goldskinmc.creategeoresonance.client;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.ModList;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonanceTinkersModels {
    private GeoResonanceTinkersModels() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GeoResonanceTinkersModels::onRegisterAdditional);
        modEventBus.addListener(GeoResonanceTinkersModels::onModifyBakingResult);
    }

    private static boolean hasIconItem() {
        return ModList.get().isLoaded("tconstruct") && BuiltInRegistries.ITEM.containsKey(
            ResourceLocation.fromNamespaceAndPath("tconstruct", "raw_cobalt"));
    }

    private static ResourceLocation iconModel() {
        return ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "item/compat/tinkers/cobalt_resonance_module");
    }

    private static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // Do not request optional native textures when their provider is absent.
        if (hasIconItem()) {
            event.register(iconModel());
        }
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!hasIconItem()) {
            return;
        }
        BakedModel model = event.getModels().get(iconModel());
        if (model == null) {
            throw new IllegalStateException("Missing registered Tinkers' Construct Cobalt module model");
        }
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "cobalt_resonance_module");
        event.getModels().put(new ModelResourceLocation(item, "inventory"), model);
    }
}
