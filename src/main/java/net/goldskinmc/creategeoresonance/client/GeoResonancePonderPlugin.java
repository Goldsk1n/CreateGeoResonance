package net.goldskinmc.creategeoresonance.client;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return CreateGeoResonanceMod.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        GeoResonancePonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        GeoResonancePonderTags.register(helper);
    }
}
