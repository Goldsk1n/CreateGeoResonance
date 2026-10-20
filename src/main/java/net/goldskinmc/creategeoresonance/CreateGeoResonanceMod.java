package net.goldskinmc.creategeoresonance;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceCreativeTabs;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceSoundEvents;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.goldskinmc.creategeoresonance.seismic.SeismicPressureStorage;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorMovingInteraction;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorMovementBehaviour;
import net.goldskinmc.creategeoresonance.seismic.SeismicScanQueue;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMovingInteraction;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMovementBehaviour;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
        modEventBus.addListener(EventPriority.LOWEST, this::onBuildCreativeTab);
        modEventBus.addListener(this::onCommonSetup);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        GeoResonanceRegistrate.init();
        GeoResonanceBlocks.register();
        GeoResonanceBlockEntityTypes.register();
        GeoResonanceItems.register();
        GeoResonanceCreativeTabs.register(modEventBus);
        GeoResonanceSoundEvents.register();
        GeoResonancePackets.register();
        SeismicCustomModules.register(MinecraftForge.EVENT_BUS);
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

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CreateGeoResonanceMod::registerContraptionBehaviours);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        Iterator<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> modules = event.getEntries().iterator();
        while (modules.hasNext()) {
            ItemStack moduleStack = modules.next().getKey();
            if (moduleStack.getItem() instanceof SeismicModuleItem module
                && (!module.moduleType().isAvailable()
                    || module instanceof SeismicCustomModuleItem && !SeismicCustomModuleItem.isConfigured(moduleStack))) {
                modules.remove();
            }
        }
        if (event.getTabKey().equals(GeoResonanceCreativeTabs.MAIN.getKey())
            || event.getTabKey() == CreativeModeTabs.SEARCH) {
            for (var definition : SeismicCustomModules.definitions()) {
                var target = net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType.custom(definition.id());
                boolean present = false;
                for (var existing = event.getEntries().iterator(); existing.hasNext();) {
                    if (SeismicCustomModuleItem.target(existing.next().getKey()) == target) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    event.accept(SeismicCustomModuleItem.create(definition.id()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
        }
        if (event.getTabKey() == CreativeModeTabs.SEARCH) {
            normalizeSearchHammerEntries(event);
            return;
        }
        replaceEmptyHammerEntries(event);
    }

    private static void registerContraptionMovementRestrictions() {
        ContraptionMovementSetting.REGISTRY.registerProvider(block -> {
            var blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null || !MODID.equals(blockId.getNamespace())) {
                return null;
            }
            return null;
        });
    }

    private static void registerContraptionBehaviours() {
        MovementBehaviour.REGISTRY.register(GeoResonanceBlocks.SEISMIC_PROJECTOR.get(), new SeismicProjectorMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(GeoResonanceBlocks.SEISMIC_PROJECTOR.get(), new SeismicProjectorMovingInteraction());
        MovementBehaviour.REGISTRY.register(GeoResonanceBlocks.SEISMIC_STATION.get(), new SeismicStationMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(GeoResonanceBlocks.SEISMIC_STATION.get(), new SeismicStationMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get(), new SeismicStationMovingInteraction());
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

    private static void normalizeSearchHammerEntries(BuildCreativeModeTabContentsEvent event) {
        boolean foundAnyHammer = false;
        Iterator<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry = iterator.next();
            if (!entry.getKey().is(GeoResonanceItems.SEISMIC_HAMMER.get())) {
                continue;
            }
            foundAnyHammer = true;
            iterator.remove();
        }

        if (foundAnyHammer) {
            event.accept(createFilledHammerStack(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }
}
