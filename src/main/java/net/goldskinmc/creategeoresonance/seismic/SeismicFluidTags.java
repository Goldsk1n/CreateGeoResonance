package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class SeismicFluidTags {
    public static final TagKey<Fluid> CRUDE_OIL = FluidTags.create(ResourceLocation.fromNamespaceAndPath(
        CreateGeoResonanceMod.MODID, "deposits/crude_oil"));

    private SeismicFluidTags() {
    }
}

