package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class GeoResonanceSoundEvents {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final RegistryEntry<SoundEvent> SEISMIC_HAMMER_HIT = REGISTRATE.simple("seismic_hammer_hit",
        Registries.SOUND_EVENT,
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID,
            "seismic_hammer_hit")));

    private GeoResonanceSoundEvents() {
    }

    public static void register() {
    }
}
