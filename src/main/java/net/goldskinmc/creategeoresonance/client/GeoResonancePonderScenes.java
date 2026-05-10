package net.goldskinmc.creategeoresonance.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GeoResonancePonderScenes {
    private GeoResonancePonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<net.minecraft.resources.ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> itemHelper = helper.withKeyFunction(ItemProviderEntry::getId);

        itemHelper.forComponents(GeoResonanceItems.SEISMIC_HAMMER, GeoResonanceBlocks.PLACED_SEISMIC_HAMMER)
            .addStoryBoard("seismic_hammer/basics", GeoResonancePonderScenes::seismicHammerBasics, GeoResonancePonderTags.SEISMIC);

        itemHelper.forComponents(GeoResonanceBlocks.SEISMIC_STATION)
            .addStoryBoard("seismic_station/operation", GeoResonancePonderScenes::seismicStationOperation, GeoResonancePonderTags.SEISMIC);

        itemHelper.forComponents(GeoResonanceBlocks.SEISMIC_PROJECTOR)
            .addStoryBoard("seismic_projector/triangulation", GeoResonancePonderScenes::seismicProjectorTriangulation, GeoResonancePonderTags.SEISMIC);
    }

    private static void seismicHammerBasics(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_hammer/basics", "Survey terrain with the Seismic Hammer");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos impact = util.grid().at(2, 1, 2);
        scene.world().setBlock(impact, Blocks.ANDESITE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(impact), Direction.UP);
        scene.idle(15);

        scene.overlay().showText(70)
            .text("Right-click with pressure to emit a seismic strike.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().blockSurface(impact, Direction.UP), Pointing.DOWN, 35)
            .rightClick()
            .withItem(new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
        scene.idle(40);

        scene.overlay().showText(70)
            .colored(PonderPalette.BLUE)
            .text("Echo color identifies anomaly type: cave, water, or lava.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.idle(40);

        scene.overlay().showText(80)
            .colored(PonderPalette.RED)
            .text("Low pressure gives weaker and ambiguous readings.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.idle(50);
    }

    private static void seismicStationOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_station/operation", "Power and run a Seismic Station");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos motor = util.grid().at(1, 2, 2);
        BlockPos shaft = util.grid().at(2, 2, 2);
        BlockPos station = util.grid().at(3, 1, 2);

        BlockState motorState = AllBlocks.CREATIVE_MOTOR.getDefaultState();
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState();
        BlockState stationState = GeoResonanceBlocks.SEISMIC_STATION.getDefaultState();

        scene.world().setBlock(motor, motorState, false);
        scene.world().setBlock(shaft, shaftState, false);
        scene.world().setBlock(station, stationState, false);
        scene.world().showSection(util.select().fromTo(motor, station), Direction.DOWN);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().fromTo(motor, station), 64.0F);
        scene.effects().rotationSpeedIndicator(shaft);
        scene.overlay().showText(80)
            .text("Feed rotation into the station's top input shaft.")
            .pointAt(util.vector().centerOf(shaft))
            .placeNearTarget();
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(station, Direction.NORTH), Pointing.RIGHT, 35)
            .rightClick()
            .withItem(new ItemStack(Items.PAPER));
        scene.overlay().showText(70)
            .text("Insert paper and ink sac on the table side.")
            .pointAt(util.vector().centerOf(station))
            .placeNearTarget();
        scene.idle(40);

        scene.overlay().showText(80)
            .text("Right-click the station to start scanning and generate a seismogram.")
            .pointAt(util.vector().topOf(station))
            .placeNearTarget();
        scene.idle(50);
    }

    private static void seismicProjectorTriangulation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_projector/triangulation", "Project anomalies from two seismograms");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos motor = util.grid().at(1, 1, 2);
        BlockPos shaft = util.grid().at(2, 1, 2);
        BlockPos projector = util.grid().at(3, 1, 2);

        scene.world().setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState(), false);
        scene.world().setBlock(shaft, AllBlocks.SHAFT.getDefaultState(), false);
        scene.world().setBlock(projector, GeoResonanceBlocks.SEISMIC_PROJECTOR.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(motor, projector), Direction.DOWN);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().fromTo(motor, projector), 96.0F);
        scene.overlay().showText(80)
            .text("The projector needs rotation from the rear shaft input.")
            .pointAt(util.vector().centerOf(shaft))
            .placeNearTarget();
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(projector, Direction.UP), Pointing.DOWN, 40)
            .rightClick()
            .withItem(new ItemStack(Items.FILLED_MAP));
        scene.overlay().showText(90)
            .text("Load two seismograms from different stations in the same dimension.")
            .pointAt(util.vector().topOf(projector))
            .placeNearTarget();
        scene.idle(60);

        scene.overlay().showText(90)
            .colored(PonderPalette.GREEN)
            .text("When both maps are valid and spaced apart, holographic veins are projected.")
            .pointAt(util.vector().topOf(projector))
            .placeNearTarget();
        scene.idle(60);
    }
}
