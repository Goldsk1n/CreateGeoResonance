package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

final class WrenchInteractionHelper {
    private WrenchInteractionHelper() {
    }

    static boolean isWrench(ItemStack stack) {
        return stack.is(Tags.Items.TOOLS_WRENCH);
    }

    static boolean isSneakWrench(ItemStack stack, Player player) {
        return isWrench(stack) && player.isShiftKeyDown();
    }

    static InteractionResult pickupBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack heldStack) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        if (!player.isCreative()) {
            Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), player, heldStack)
                .forEach(player.getInventory()::placeItemBackInInventory);
        }

        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }
}
