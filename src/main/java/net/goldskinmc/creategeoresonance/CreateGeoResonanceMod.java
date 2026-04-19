package net.goldskinmc.creategeoresonance;

import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.SeismicScanQueue;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.DistExecutor;

@Mod(CreateGeoResonanceMod.MODID)
public class CreateGeoResonanceMod {
    public static final String MODID = "creategeoresonance";

    public CreateGeoResonanceMod(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::onBuildCreativeTab);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        GeoResonanceRegistrate.init();
        GeoResonanceBlocks.register();
        GeoResonanceItems.register();
        GeoResonancePackets.register();

        MinecraftForge.EVENT_BUS.addListener(SeismicScanQueue::onServerTick);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SeismicScanQueue.clear());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.goldskinmc.creategeoresonance.client.GeoResonanceClient.register());
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GeoResonanceItems.SEISMIC_HAMMER.get());
        }
    }
}
