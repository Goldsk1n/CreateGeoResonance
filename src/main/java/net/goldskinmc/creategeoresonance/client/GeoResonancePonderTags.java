package net.goldskinmc.creategeoresonance.client;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePonderTags {
    public static final ResourceLocation SEISMIC = id("seismic");

    private GeoResonancePonderTags() {
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<ItemProviderEntry<?>> itemHelper = helper.withKeyFunction(ItemProviderEntry::getId);

        helper.registerTag(SEISMIC)
            .item(GeoResonanceItems.SEISMIC_HAMMER.get(), true, false)
            .title("Seismic Exploration")
            .description("Locate caves, fluids and ore signatures without digging blind")
            .addToIndex()
            .register();

        itemHelper.addToTag(SEISMIC)
            .add(GeoResonanceItems.SEISMIC_HAMMER)
            .add(GeoResonanceBlocks.PLACED_SEISMIC_HAMMER)
            .add(GeoResonanceBlocks.SEISMIC_STATION)
            .add(GeoResonanceBlocks.SEISMIC_PROJECTOR);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, path);
    }
}
