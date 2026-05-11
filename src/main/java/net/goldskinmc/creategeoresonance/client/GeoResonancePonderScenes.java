package net.goldskinmc.creategeoresonance.client;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

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
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        scene.scaleSceneView(0.9F);

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 1, 6), Direction.UP);
        BlockPos impact = util.grid().at(3, 1, 3);
        BlockPos actorPos = util.grid().at(3, 1, 5);
        BlockPos caveEcho = util.grid().at(2, 1, 2);
        BlockPos waterEcho = util.grid().at(5, 1, 3);
        BlockPos lavaEcho = util.grid().at(1, 1, 4);

        ElementLink<EntityElement> actor = scene.world().createEntity(level -> {
            ArmorStand stand = new ArmorStand(level, actorPos.getX() + 0.5D, actorPos.getY(), actorPos.getZ() + 0.5D);
            stand.setNoBasePlate(true);
            stand.setShowArms(true);
            stand.setYRot(180.0F);
            stand.setRightArmPose(new Rotations(-8.0F, 0.0F, 7.0F));
            stand.setLeftArmPose(new Rotations(-14.0F, 0.0F, -5.0F));
            stand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
            return stand;
        });
        scene.idle(15);

        scene.overlay().showText(60)
            .text("Right-click to strike the ground.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().blockSurface(impact, Direction.UP), Pointing.DOWN, 35)
            .rightClick()
            .withItem(new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get()));
        scene.idle(12);

        setActorArmPose(scene, actor, new Rotations(32.0F, 0.0F, 6.0F));
        scene.idle(3);
        setActorArmPose(scene, actor, new Rotations(-8.0F, 0.0F, 7.0F));

        emitEcho(scene, util, impact, new Vector3f(0.78F, 0.78F, 0.78F), 34, 0.18F);
        scene.idle(20);

        scene.overlay().showText(90)
            .colored(PonderPalette.BLUE)
            .text("Returning echoes reveal cave, water, and lava.")
            .pointAt(util.vector().topOf(impact))
            .placeNearTarget();
        emitEcho(scene, util, caveEcho, new Vector3f(0.70F, 0.70F, 0.70F), 26, 0.12F);
        scene.idle(14);

        emitEcho(scene, util, waterEcho, new Vector3f(0.24F, 0.64F, 0.96F), 30, 0.12F);
        scene.idle(16);

        emitEcho(scene, util, lavaEcho, new Vector3f(1.00F, 0.54F, 0.20F), 30, 0.12F);
        scene.idle(32);
    }

    private static void seismicStationOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("seismic_station/operation", "Power and run a Seismic Station");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();

        BlockPos motor = util.grid().at(1, 2, 2);
        BlockPos input = util.grid().at(3, 2, 2);
        BlockPos station = util.grid().at(3, 1, 2);

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 2, 5), Direction.DOWN);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().position(motor), 64.0F);
        scene.effects().rotationSpeedIndicator(input);
        scene.overlay().showText(80)
            .text("Feed rotation into the station's top input shaft.")
            .pointAt(util.vector().centerOf(input))
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

        scene.world().showSection(util.select().fromTo(0, 0, 0, 5, 1, 5), Direction.DOWN);
        scene.idle(20);

        scene.world().setKineticSpeed(util.select().position(motor), 96.0F);
        scene.overlay().showText(80)
            .text("The projector needs rotation from the rear shaft input.")
            .pointAt(util.vector().centerOf(shaft))
            .placeNearTarget();
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(projector, Direction.UP), Pointing.DOWN, 40)
            .rightClick()
            .withItem(createPonderSeismogramStack());
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

    private static void setActorArmPose(CreateSceneBuilder scene, ElementLink<EntityElement> actor, Rotations pose) {
        scene.world().modifyEntity(actor, entity -> {
            if (entity instanceof ArmorStand stand) {
                stand.setRightArmPose(pose);
            }
        });
    }

    private static void emitEcho(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Vector3f color, int count, float speed) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        scene.effects().emitParticles(util.vector().centerOf(pos),
            scene.effects().particleEmitterWithinBlockSpace(dust, util.vector().of(0.45D, 0.06D, 0.45D)), speed, count);
        scene.effects().indicateSuccess(pos);
    }

    private static ItemStack createPonderSeismogramStack() {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("GeoSeismogram", new CompoundTag());
        stack.setTag(tag);
        return stack;
    }
}
