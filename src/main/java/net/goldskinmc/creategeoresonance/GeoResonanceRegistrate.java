package net.goldskinmc.creategeoresonance;

import com.tterrag.registrate.Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class GeoResonanceRegistrate {
    private static final Registrate REGISTRATE = Registrate.create(CreateGeoResonanceMod.MODID);
    private static final ResourceKey<CreativeModeTab> MAIN_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "main")
    );

    private GeoResonanceRegistrate() {
    }

    public static Registrate registrate() {
        return REGISTRATE;
    }

    public static void init() {
        REGISTRATE.defaultCreativeTab(MAIN_TAB);
    }
}
