package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SeismicStationControllerLogic {
    private SeismicStationControllerLogic() {
    }

    public static boolean tryInsertModule(Host host, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SeismicModuleType moduleType = SeismicModuleItem.getModuleType(held);
        if (moduleType == null) {
            return false;
        }
        if (hasModuleInstalled(host.inventory(), moduleType)) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_duplicate")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }
        int targetSlot = findEmptyModuleSlot(host.inventory());
        if (targetSlot < 0) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        ItemStack inserted = held.copyWithCount(1);
        host.inventory().setStackInSlot(targetSlot, inserted);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        host.onDataChanged();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_loaded", inserted.getHoverName())
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(host, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.05F);
        spawnFeedbackParticles(host, ParticleTypes.WAX_ON, 5, 0.18D, 0.01D);
        return true;
    }

    public static boolean tryExtractModule(Host host, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        int slot = findLastFilledModuleSlot(host.inventory());
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_module")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        ItemStack extracted = host.inventory().getStackInSlot(slot).copy();
        host.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.setItemInHand(hand, extracted);
        } else if (!player.addItem(extracted.copy())) {
            player.drop(extracted.copy(), false);
        }
        host.onDataChanged();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.module_unloaded", extracted.getHoverName())
            .withStyle(ChatFormatting.AQUA), true);
        playFeedbackSound(host, SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 0.72F);
        spawnFeedbackParticles(host, ParticleTypes.WAX_OFF, 5, 0.18D, 0.01D);
        return true;
    }

    public static boolean tryInsertPaper(Host host, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.PAPER)) {
            return false;
        }
        if (!host.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_PAPER_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.paper_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        host.inventory().setStackInSlot(SeismicStationBlockEntity.SLOT_PAPER_INPUT, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        host.onDataChanged();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.paper_loaded")
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(host, SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 1.35F);
        spawnFeedbackParticles(host, ParticleTypes.CLOUD, 6, 0.22D, 0.01D);
        return true;
    }

    public static boolean tryInsertInk(Host host, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.INK_SAC)) {
            return false;
        }
        if (!host.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_INK_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.ink_full")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        host.inventory().setStackInSlot(SeismicStationBlockEntity.SLOT_INK_INPUT, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        host.onDataChanged();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.ink_loaded")
            .withStyle(ChatFormatting.GREEN), true);
        playFeedbackSound(host, SoundEvents.NOTE_BLOCK_HAT.value(), 0.55F, 0.8F);
        spawnFeedbackParticles(host, ParticleTypes.SQUID_INK, 4, 0.18D, 0.0D);
        return true;
    }

    public static boolean tryTakeOutputWithBareHand(Host host, Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return false;
        }

        ItemStack output = host.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT);
        if (output.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_output_ready")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        player.setItemInHand(hand, output.copy());
        host.inventory().setStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT, ItemStack.EMPTY);
        host.onDataChanged();
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.output_taken")
            .withStyle(ChatFormatting.AQUA), true);
        playFeedbackSound(host, SoundEvents.NOTE_BLOCK_BASS.value(), 0.65F, 1.3F);
        spawnFeedbackParticles(host, ParticleTypes.GLOW, 8, 0.2D, 0.01D);
        return true;
    }

    public static List<ItemStack> getInstalledModuleStacks(ItemStackHandler inventory) {
        List<ItemStack> modules = new ArrayList<>();
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                modules.add(stack.copy());
            }
        }
        return modules;
    }

    public static boolean hasModuleInstalled(ItemStackHandler inventory, SeismicModuleType moduleType) {
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            SeismicModuleType installed = SeismicModuleItem.getModuleType(inventory.getStackInSlot(slot));
            if (installed == moduleType) {
                return true;
            }
        }
        return false;
    }

    public static int getInstalledModuleCount(ItemStackHandler inventory) {
        int count = 0;
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static int findEmptyModuleSlot(ItemStackHandler inventory) {
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public static int findLastFilledModuleSlot(ItemStackHandler inventory) {
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_END; slot >= SeismicStationBlockEntity.SLOT_MODULE_START; slot--) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean isModuleSlot(int slot) {
        return slot >= SeismicStationBlockEntity.SLOT_MODULE_START && slot <= SeismicStationBlockEntity.SLOT_MODULE_END;
    }

    private static void playFeedbackSound(Host host, SoundEvent sound, float volume, float pitch) {
        Level level = host.level();
        if (level == null) {
            return;
        }
        level.playSound(null, host.feedbackPos(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    private static void spawnFeedbackParticles(Host host, ParticleOptions particle, int count, double spread, double speed) {
        Level level = host.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = host.feedbackPos();
        serverLevel.sendParticles(particle,
            pos.getX() + 0.5D,
            pos.getY() + 1.05D,
            pos.getZ() + 0.5D,
            count,
            spread,
            0.12D,
            spread,
            speed);
    }

    public interface Host {
        @Nullable Level level();

        BlockPos feedbackPos();

        ItemStackHandler inventory();

        void onDataChanged();
    }
}
