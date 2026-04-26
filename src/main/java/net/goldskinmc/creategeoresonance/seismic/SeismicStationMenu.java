package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeismicStationMenu extends AbstractContainerMenu {
    private final Level level;
    private final ContainerLevelAccess access;
    private final SeismicStationBlockEntity station;

    public SeismicStationMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos stationPos) {
        super(type, containerId);
        this.level = inventory.player.level();
        this.access = ContainerLevelAccess.create(level, stationPos);

        if (!(level.getBlockEntity(stationPos) instanceof SeismicStationBlockEntity station)) {
            throw new IllegalStateException("Missing seismic station block entity at " + stationPos);
        }
        this.station = station;

        addSlot(new SlotItemHandler(station.getInventory(), SeismicStationBlockEntity.SLOT_PAPER_INPUT, 17, 35));
        addSlot(new SlotItemHandler(station.getInventory(), SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inventory);
    }

    public static SeismicStationMenu create(MenuType<SeismicStationMenu> type, int containerId, Inventory inventory,
                                            @Nullable FriendlyByteBuf buffer) {
        if (buffer == null) {
            throw new IllegalStateException("Expected block position in seismic station menu buffer");
        }
        return new SeismicStationMenu(type, containerId, inventory, buffer.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return empty;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int containerSlots = 2;
        if (index < containerSlots) {
            if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.is(net.minecraft.world.item.Items.PAPER)) {
                if (!moveItemStackTo(stack, SeismicStationBlockEntity.SLOT_PAPER_INPUT, SeismicStationBlockEntity.SLOT_PAPER_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, GeoResonanceBlocks.SEISMIC_STATION.get());
    }

    public boolean isScanRunning() {
        return station.isScanRunning();
    }

    public boolean isAwaitingScanResult() {
        return station.isAwaitingScanResult();
    }

    public boolean isMapReady() {
        return station.isMapReady();
    }

    public boolean isStartingStrikeSequence() {
        return station.isStartingStrikeSequence();
    }

    public int getCooldownTicks() {
        return station.getCooldownTicks();
    }

    public float getOperationalSpeed() {
        return station.getOperationalSpeed();
    }

    public boolean hasRequiredSpeed() {
        return station.hasRequiredSpeed();
    }

    public int getCurrentStrikeIntervalTicks() {
        return station.getCurrentStrikeIntervalTicks();
    }

    public int getStationRadius() {
        return station.getStationRadius();
    }

    public List<SeismicStationBlockEntity.MapEntry> getMapEntries() {
        return station.getMapEntries();
    }

    private void addPlayerInventory(Inventory inventory) {
        int playerInventoryY = 84;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, playerInventoryY + row * 18));
            }
        }
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            addSlot(new Slot(inventory, hotbarSlot, 8 + hotbarSlot * 18, playerInventoryY + 58));
        }
    }
}
