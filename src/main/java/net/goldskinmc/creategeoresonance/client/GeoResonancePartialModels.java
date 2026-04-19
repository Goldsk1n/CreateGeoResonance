package net.goldskinmc.creategeoresonance.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePartialModels {
    public static final PartialModel PLACED_SEISMIC_HAMMER_ROTOR = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "block/placed_seismic_hammer_rotor"));

    private GeoResonancePartialModels() {
    }

    public static void init() {
    }
}
