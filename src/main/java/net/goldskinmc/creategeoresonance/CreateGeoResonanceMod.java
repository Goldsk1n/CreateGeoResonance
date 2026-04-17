package net.goldskinmc.creategeoresonance;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreateGeoResonanceMod.MODID)
public class CreateGeoResonanceMod {
    public static final String MODID = "creategeoresonance";

    public CreateGeoResonanceMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
