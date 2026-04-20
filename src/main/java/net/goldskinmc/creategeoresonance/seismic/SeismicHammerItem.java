package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.client.render.SeismicHammerItemRenderer;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class SeismicHammerItem extends Item {
    private static final ResourceLocation NETHERITE_BACKTANK_ID = ResourceLocation.fromNamespaceAndPath("create", "netherite_backtank");
    private static final float PUNCH_DAMAGE = 10.0F;
    private static final double PUNCH_KNOCKBACK = 2.25D;

    public SeismicHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player actor = context.getPlayer();
        if (actor != null && actor.isShiftKeyDown()) {
            return tryPlaceAsBlock(context, actor);
        }

        if (!(actor instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.CONSUME;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.CONSUME;
        }

        ItemStack hammerStack = context.getItemInHand();
        refillFromBacktank(hammerStack, player);

        PressureState pressure = PressureState.from(player, hammerStack);
        if (!pressure.canScan()) {
            level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.88F, 1.22F);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.5F, 1.45F);
            player.displayClientMessage(Component.translatable("item.creategeoresonance.seismic_hammer.no_pressure")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        SeismicPressureStorage.setStoredPressure(hammerStack, pressure.air() - pressure.airCost());
        boolean lowPressure = pressure.lowPressure();
        int depth = lowPressure ? Math.max(1, Config.DEPTH.get() / 2) : Config.DEPTH.get();
        BlockState struckState = level.getBlockState(context.getClickedPos());
        if (!pressure.netheriteBonus() && isSoftImpactBlock(struckState)) {
            depth = Math.max(1, Mth.floor(depth * Config.SOFT_BLOCK_DEPTH_MULTIPLIER.get().floatValue()));
        }
        float noise = (float) (Config.BASE_NOISE.get() * (lowPressure ? 2.0D : 1.0D));
        if (pressure.netheriteBonus()) {
            noise *= 0.75F;
        }

        player.getCooldowns().addCooldown(this, Config.COOLDOWN_TICKS.get());
        applyStandingRecoil(player);
        GeoResonancePackets.sendSeismicImpact(level, context.getClickedPos(), player.getId(), lowPressure);
        SeismicScanQueue.enqueue(new SeismicScanQueue.SeismicScanRequest(
            level,
            context.getClickedPos(),
            player.getId(),
            Config.RADIUS.get(),
            depth,
            noise,
            pressure.netheriteBonus(),
            level.getGameTime(),
            lowPressure
        ));
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand usedHand) {
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        if (!target.hurt(player.damageSources().playerAttack(player), PUNCH_DAMAGE)) {
            return InteractionResult.PASS;
        }

        float yawRadians = player.getYRot() * ((float) Math.PI / 180F);
        target.knockback(PUNCH_KNOCKBACK, Mth.sin(yawRadians), -Mth.cos(yawRadians));
        player.getCooldowns().addCooldown(this, Config.COOLDOWN_TICKS.get());
        player.level().playSound(null, target.blockPosition(), GeoResonanceSoundEvents.SEISMIC_HAMMER_HIT.get(), SoundSource.PLAYERS, 0.9F, 0.98F);
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryPlaceAsBlock(UseOnContext context, Player actor) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (!(actor instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.CONSUME;
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!placeContext.canPlace() || !level.getWorldBorder().isWithinBounds(placeContext.getClickedPos())) {
            return InteractionResult.PASS;
        }
        BlockState placeState = GeoResonanceBlocks.PLACED_SEISMIC_HAMMER.get().getStateForPlacement(placeContext);
        if (placeState == null) {
            return InteractionResult.PASS;
        }
        if (!level.setBlock(placeContext.getClickedPos(), placeState, Block.UPDATE_ALL)) {
            return InteractionResult.PASS;
        }
        placeState.getBlock().setPlacedBy(level, placeContext.getClickedPos(), placeState, player, context.getItemInHand());

        level.playSound(null, placeContext.getClickedPos(), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float fill = SeismicPressureStorage.getStoredPressure(stack) / SeismicPressureStorage.maxPressure();
        return Mth.clamp(Math.round(fill * 13.0F), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fill = Mth.clamp(SeismicPressureStorage.getStoredPressure(stack) / SeismicPressureStorage.maxPressure(), 0.0F, 1.0F);
        return Mth.hsvToRgb(fill / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.1")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.2")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.3")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.4")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            refillFromBacktank(stack, player);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    private static boolean isSoftImpactBlock(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL);
    }

    private static void refillFromBacktank(ItemStack hammerStack, Player player) {
        float currentPressure = SeismicPressureStorage.getStoredPressure(hammerStack);
        float maxPressure = SeismicPressureStorage.maxPressure();
        if (currentPressure >= maxPressure) {
            return;
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.isEmpty() || !AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(chestStack)) {
            return;
        }

        float backtankAir = BacktankUtil.getAir(chestStack);
        if (backtankAir <= 0.0F) {
            return;
        }

        float transfer = Math.min(maxPressure - currentPressure, backtankAir);
        if (transfer <= 0.0F) {
            return;
        }

        SeismicPressureStorage.setStoredPressure(hammerStack, currentPressure + transfer);
        setBacktankAir(chestStack, backtankAir - transfer);
    }

    private static void setBacktankAir(ItemStack backtank, float air) {
        CompoundTag tag = backtank.getOrCreateTag();
        tag.putFloat("Air", Mth.clamp(air, 0.0F, BacktankUtil.maxAir(backtank)));
        backtank.setTag(tag);
    }

    private static void applyStandingRecoil(Player player) {
        if (!player.onGround()) {
            return;
        }
        double horizontalSpeedSqr = player.getDeltaMovement().x * player.getDeltaMovement().x
            + player.getDeltaMovement().z * player.getDeltaMovement().z;
        if (horizontalSpeedSqr > 0.0025D) {
            return;
        }
        player.push(0.0D, 0.11D, 0.0D);
        player.hurtMarked = true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new SeismicHammerItemRenderer()));
    }

    private record PressureState(float air, float maxAir, boolean lowPressure, boolean netheriteBonus) {
        private static PressureState from(ServerPlayer player, ItemStack hammerStack) {
            float maxAir = SeismicPressureStorage.maxPressure();
            float air = SeismicPressureStorage.getStoredPressure(hammerStack);
            boolean lowPressure = air / maxAir < Config.LOW_PRESSURE_THRESHOLD.get().floatValue();
            ResourceLocation chestItemId = ForgeRegistries.ITEMS.getKey(player.getItemBySlot(EquipmentSlot.CHEST).getItem());
            boolean netheriteBonus = NETHERITE_BACKTANK_ID.equals(chestItemId);
            return new PressureState(air, maxAir, lowPressure, netheriteBonus);
        }

        private boolean canScan() {
            return air > 0.0F;
        }

        private float airCost() {
            return Mth.clamp(BacktankUtil.maxAirWithoutEnchants() / (float) Config.SCANS_PER_BACKTANK.get(), 1.0F, maxAir);
        }
    }
}
