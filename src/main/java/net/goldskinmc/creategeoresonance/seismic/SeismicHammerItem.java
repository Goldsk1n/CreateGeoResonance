package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeismicHammerItem extends Item {
    private static final ResourceLocation NETHERITE_BACKTANK_ID = ResourceLocation.fromNamespaceAndPath("create", "netherite_backtank");

    public SeismicHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.CONSUME;
        }

        PressureState pressure = PressureState.from(player);
        if (!pressure.canScan()) {
            level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.9f, 0.65f);
            player.displayClientMessage(Component.translatable("item.creategeoresonance.seismic_hammer.no_pressure")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }

        BacktankUtil.consumeAir(player, pressure.backtank(), pressure.airCost());
        boolean lowPressure = pressure.lowPressure();
        int depth = lowPressure ? Math.max(1, Config.DEPTH.get() / 2) : Config.DEPTH.get();
        float noise = (float) (Config.BASE_NOISE.get() * (lowPressure ? 2.0D : 1.0D));

        player.getCooldowns().addCooldown(this, Config.COOLDOWN_TICKS.get());
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
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BacktankUtil.isBarVisible(stack, Config.SCANS_PER_BACKTANK.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BacktankUtil.getBarWidth(stack, Config.SCANS_PER_BACKTANK.get());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BacktankUtil.getBarColor(stack, Config.SCANS_PER_BACKTANK.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.1")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.creategeoresonance.seismic_hammer.tooltip.2")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private record PressureState(ItemStack backtank, float air, float maxAir, boolean lowPressure, boolean netheriteBonus) {
        private static PressureState from(ServerPlayer player) {
            List<ItemStack> tanks = BacktankUtil.getAllWithAir(player);
            if (tanks.isEmpty()) {
                return new PressureState(ItemStack.EMPTY, 0.0F, 1.0F, false, false);
            }
            ItemStack selected = tanks.get(0);
            float maxAir = Math.max(1.0F, BacktankUtil.maxAir(selected));
            float air = BacktankUtil.getAir(selected);
            boolean lowPressure = air / maxAir < Config.LOW_PRESSURE_THRESHOLD.get().floatValue();
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(selected.getItem());
            boolean netheriteBonus = NETHERITE_BACKTANK_ID.equals(itemId);
            return new PressureState(selected, air, maxAir, lowPressure, netheriteBonus);
        }

        private boolean canScan() {
            return !backtank.isEmpty() && air > 0.0F;
        }

        private float airCost() {
            return Mth.clamp(BacktankUtil.maxAirWithoutEnchants() / (float) Config.SCANS_PER_BACKTANK.get(), 1.0F, maxAir);
        }
    }
}
