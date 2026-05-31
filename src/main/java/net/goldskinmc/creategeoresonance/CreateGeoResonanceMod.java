package net.goldskinmc.creategeoresonance;

import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceCreativeTabs;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceSoundEvents;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.goldskinmc.creategeoresonance.seismic.SeismicPressureStorage;
import net.goldskinmc.creategeoresonance.seismic.SeismicScanQueue;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.Map;

@Mod(CreateGeoResonanceMod.MODID)
public class CreateGeoResonanceMod {
    public static final String MODID = "creategeoresonance";

    public CreateGeoResonanceMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onBuildCreativeTab);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        GeoResonanceRegistrate.init();
        GeoResonanceBlocks.register();
        GeoResonanceBlockEntityTypes.register();
        GeoResonanceItems.register();
        GeoResonanceCreativeTabs.register(modEventBus);
        GeoResonanceSoundEvents.register();
        GeoResonancePackets.register();
        registerContraptionMovementRestrictions();

        MinecraftForge.EVENT_BUS.addListener(SeismicScanQueue::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(SeismogramMapService::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            SeismicScanQueue.clear();
            SeismogramMapService.clear();
        });

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.goldskinmc.creategeoresonance.client.GeoResonanceClient.register(modEventBus));
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        replaceEmptyHammerEntries(event);
    }

    private static void registerContraptionMovementRestrictions() {
        ContraptionMovementSetting.REGISTRY.registerProvider(block -> {
            var blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null || !MODID.equals(blockId.getNamespace())) {
                return null;
            }
            String path = blockId.getPath();
            if ("seismic_station".equals(path) || "seismic_station_bounding".equals(path)) {
                return () -> ContraptionMovementSetting.UNMOVABLE;
            }
            return null;
        });
    }

    private static ItemStack createFilledHammerStack() {
        return GeoResonanceCreativeTabs.createFilledHammerStack();
    }

    private static void replaceEmptyHammerEntries(BuildCreativeModeTabContentsEvent event) {
        boolean removedEmpty = false;
        boolean hasFilled = false;
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        Iterator<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry = iterator.next();
            ItemStack stack = entry.getKey();
            if (!stack.is(GeoResonanceItems.SEISMIC_HAMMER.get())) {
                continue;
            }

            visibility = entry.getValue();
            if (SeismicPressureStorage.getStoredPressure(stack) > 0.0F) {
                hasFilled = true;
                continue;
            }

            iterator.remove();
            removedEmpty = true;
        }

        if (removedEmpty && !hasFilled) {
            event.accept(createFilledHammerStack(), visibility);
        }
    }
}
