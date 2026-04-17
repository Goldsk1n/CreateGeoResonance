package net.goldskinmc.creategeoresonance;

import com.tterrag.registrate.Registrate;

public final class GeoResonanceRegistrate {
    private static final Registrate REGISTRATE = Registrate.create(CreateGeoResonanceMod.MODID);

    private GeoResonanceRegistrate() {
    }

    public static Registrate registrate() {
        return REGISTRATE;
    }

    public static void init() {
    }
}
