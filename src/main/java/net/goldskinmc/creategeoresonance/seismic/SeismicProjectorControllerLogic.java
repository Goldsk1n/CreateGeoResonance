package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SeismicProjectorControllerLogic {
    private SeismicProjectorControllerLogic() {
    }

    public static boolean tryTakeSeismogram(SeismicProjectorData data, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()
            || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack stack = data.takeLastSeismogram(level);
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.no_seismogram").withStyle(ChatFormatting.RED), true);
            return true;
        }
        player.setItemInHand(hand, stack);
        player.displayClientMessage(Component.translatable(
                "block.creategeoresonance.seismic_projector.node_unloaded",
                data.getLoadedNodeCount(), SeismicProjectorData.MAX_NODES)
            .withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
            SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }
}
