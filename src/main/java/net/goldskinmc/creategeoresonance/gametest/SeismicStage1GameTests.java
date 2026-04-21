package net.goldskinmc.creategeoresonance.gametest;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceItems;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicPressureStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(CreateGeoResonanceMod.MODID)
@Mod.EventBusSubscriber(modid = CreateGeoResonanceMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SeismicStage1GameTests {
    private SeismicStage1GameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(SeismicStage1GameTests.class);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void punchFailsWithoutPressure(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.moveTo(helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D)));

        ItemStack hammerStack = new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hammerStack);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5D, 2.0D, 2.5D));
        float initialHealth = zombie.getHealth();

        InteractionResult result = ((SeismicHammerItem) hammerStack.getItem()).interactLivingEntity(
            hammerStack,
            player,
            zombie,
            InteractionHand.MAIN_HAND
        );

        helper.assertTrue(result.consumesAction(), "Expected no-pressure punch to consume interaction");
        helper.assertTrue(Math.abs(zombie.getHealth() - initialHealth) < 0.01F, "No-pressure punch should not deal damage");
        helper.assertFalse(player.getCooldowns().isOnCooldown(hammerStack.getItem()), "Cooldown should not apply on failed punch");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void punchConsumesPressureAndAppliesCooldown(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.moveTo(helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D)));

        ItemStack hammerStack = new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get());
        SeismicPressureStorage.setStoredPressure(hammerStack, SeismicPressureStorage.maxPressure());
        float beforePressure = SeismicPressureStorage.getStoredPressure(hammerStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammerStack);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5D, 2.0D, 2.5D));
        float initialHealth = zombie.getHealth();

        InteractionResult result = ((SeismicHammerItem) hammerStack.getItem()).interactLivingEntity(
            hammerStack,
            player,
            zombie,
            InteractionHand.MAIN_HAND
        );

        helper.assertTrue(result.consumesAction(), "Expected pressurized punch to consume interaction");
        helper.assertTrue(zombie.getHealth() <= initialHealth - 9.5F, "Pressurized punch should deal heavy damage");
        helper.assertTrue(player.getCooldowns().isOnCooldown(hammerStack.getItem()), "Cooldown should apply after punch");
        helper.assertTrue(SeismicPressureStorage.getStoredPressure(hammerStack) < beforePressure, "Pressurized punch should consume pressure");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void shiftPlaceTransfersPressureToBlock(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.moveTo(helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D)));
        player.setShiftKeyDown(true);

        ItemStack hammerStack = new ItemStack(GeoResonanceItems.SEISMIC_HAMMER.get());
        float expectedPressure = Math.min(SeismicPressureStorage.maxPressure(), 321.0F);
        SeismicPressureStorage.setStoredPressure(hammerStack, expectedPressure);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammerStack);

        BlockPos basePos = new BlockPos(1, 1, 1);
        helper.setBlock(basePos, Blocks.STONE);

        BlockPos baseAbsolute = helper.absolutePos(basePos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(baseAbsolute), Direction.UP, baseAbsolute, false);
        InteractionResult result = hammerStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(result.consumesAction(), "Shift-right-click should place the hammer block");

        BlockPos placedPos = basePos.above();
        helper.assertBlockPresent(GeoResonanceBlocks.PLACED_SEISMIC_HAMMER.get(), placedPos);
        if (!(helper.getBlockEntity(placedPos) instanceof PlacedSeismicHammerBlockEntity placedHammer)) {
            helper.fail("Placed hammer block entity is missing at expected position", placedPos);
            return;
        }

        float actualPressure = placedHammer.getStoredPressure();
        helper.assertTrue(Math.abs(actualPressure - expectedPressure) < 0.01F, "Placed hammer should retain item pressure");
        helper.succeed();
    }
}
