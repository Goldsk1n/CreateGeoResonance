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
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorMovingInteraction;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorMovementBehaviour;
import net.goldskinmc.creategeoresonance.seismic.SeismicScanQueue;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMovingInteraction;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMovementBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import java.util.ArrayList;
import java.util.List;

@Mod(CreateGeoResonanceMod.MODID)
public class CreateGeoResonanceMod {
    public static final String MODID = "creategeoresonance";

    public CreateGeoResonanceMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(EventPriority.LOWEST, BuildCreativeModeTabContentsEvent.class, this::onBuildCreativeTab);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterCapabilities);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        GeoResonanceRegistrate.init();
        GeoResonanceBlocks.register();
        GeoResonanceBlockEntityTypes.register();
        GeoResonanceItems.register();
        GeoResonanceCreativeTabs.register(modEventBus);
        GeoResonanceSoundEvents.register();
        GeoResonancePackets.register();
        registerContraptionMovementRestrictions();

        NeoForge.EVENT_BUS.addListener(SeismicScanQueue::onServerTick);
        NeoForge.EVENT_BUS.addListener(SeismogramMapService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            SeismicScanQueue.clear();
            SeismogramMapService.clear();
        });

        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.goldskinmc.creategeoresonance.client.GeoResonanceClient.register(modEventBus);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CreateGeoResonanceMod::registerContraptionBehaviours);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> entries = new ArrayList<>(event.getParentEntries());
        entries.addAll(event.getSearchEntries());
        for (ItemStack stack : entries) {
            if (stack.getItem() instanceof SeismicModuleItem module && !module.moduleType().isAvailable()) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.SEARCH) {
            normalizeSearchHammerEntries(event);
            return;
        }
        replaceEmptyHammerEntries(event);
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GeoResonanceBlockEntityTypes.SEISMIC_STATION.get(), SeismicStationBlockEntity::getItemHandler);
    }

    private static void registerContraptionMovementRestrictions() {
        ContraptionMovementSetting.REGISTRY.registerProvider(block -> {
            var blockId = BuiltInRegistries.BLOCK.getKey(block);
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
        for (ItemStack stack : new ArrayList<>(event.getParentEntries())) {
            if (!stack.is(GeoResonanceItems.SEISMIC_HAMMER.get())) {
                continue;
            }
            if (SeismicPressureStorage.getStoredPressure(stack) > 0.0F) {
                hasFilled = true;
                continue;
            }
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            removedEmpty = true;
        }

        if (removedEmpty && !hasFilled) {
            event.accept(createFilledHammerStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static void normalizeSearchHammerEntries(BuildCreativeModeTabContentsEvent event) {
        boolean foundAnyHammer = false;
        for (ItemStack stack : List.copyOf(event.getSearchEntries())) {
            if (!stack.is(GeoResonanceItems.SEISMIC_HAMMER.get())) {
                continue;
            }
            foundAnyHammer = true;
            event.remove(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }

        if (foundAnyHammer) {
            event.accept(createFilledHammerStack(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }
}
