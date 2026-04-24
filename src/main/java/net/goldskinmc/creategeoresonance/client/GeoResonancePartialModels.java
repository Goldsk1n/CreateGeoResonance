package net.goldskinmc.creategeoresonance.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.resources.ResourceLocation;

public final class GeoResonancePartialModels {
    public static final PartialModel PLACED_SEISMIC_HAMMER_ROTOR = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "block/placed_seismic_hammer_rotor"));
    public static final PartialModel SEISMIC_STATION_TOP_SHAFT = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "block/seismic_station_top_shaft"));
    public static final PartialModel SEISMIC_STATION_DRUM = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "block/seismic_station_drum"));
    public static final PartialModel SEISMIC_WAVE = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "block/seismic_wave"));

    private GeoResonancePartialModels() {
    }

    public static void init() {
    }
}
