package net.goldskinmc.creategeoresonance.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.network.packet.C2SStartSeismicStationPacket;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SeismicStationScreen extends AbstractContainerScreen<SeismicStationMenu> {
    private static final int MAP_SIZE = 80;
    private Button startButton;

    public SeismicStationScreen(SeismicStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        startButton = addRenderableWidget(Button.builder(Component.translatable("block.creategeoresonance.seismic_station.start"),
            button -> GeoResonancePackets.sendToServer(new C2SStartSeismicStationPacket(menu.getStationPos())))
            .bounds(leftPos + 8, topPos + 16, 60, 20)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC1C1C1C);
        graphics.fill(leftPos + 7, topPos + 15, leftPos + 69, topPos + 37, 0xCC303030);
        graphics.fill(leftPos + 80, topPos + 15, leftPos + 80 + MAP_SIZE, topPos + 15 + MAP_SIZE, 0xCC202020);
        graphics.fill(leftPos + 81, topPos + 16, leftPos + 79 + MAP_SIZE, topPos + 14 + MAP_SIZE, 0xFF101010);
        renderMap(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, 0xF0F0F0, false);
        graphics.drawString(font, Component.translatable("block.creategeoresonance.seismic_station.paper"), 8, 40, 0xD0D0D0, false);
        float speed = Math.abs(menu.getOperationalSpeed());
        int roundedSpeed = Math.round(speed);
        int speedColor = menu.hasRequiredSpeed() ? 0x8FD8FF : 0xFF8C8C;
        graphics.drawString(font, Component.translatable("block.creategeoresonance.seismic_station.speed",
            roundedSpeed, Config.STATION_MIN_SPEED.get()), 8, 52, speedColor, false);
        graphics.drawString(font, Component.translatable("block.creategeoresonance.seismic_station.stress",
            Math.round((float) (Config.STATION_STRESS_IMPACT.get() * speed))), 8, 64, 0xE5C98A, false);
        graphics.drawString(font, Component.translatable("block.creategeoresonance.seismic_station.rhythm",
            menu.getCurrentStrikeIntervalTicks() / 20.0F), 8, 76, 0xC0C0C0, false);

        Component status = statusText();
        graphics.drawString(font, status, 8, 88, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("block.creategeoresonance.seismic_station.map"), 80, 5, 0xD0D0D0, false);

        List<SeismicStationBlockEntity.MapEntry> entries = menu.getMapEntries();
        int listY = 98;
        int listLimit = Math.min(4, entries.size());
        for (int i = 0; i < listLimit; i++) {
            SeismicStationBlockEntity.MapEntry entry = entries.get(i);
            ChatFormatting color = switch (entry.type()) {
                case CAVE -> ChatFormatting.GRAY;
                case WATER -> ChatFormatting.AQUA;
                case LAVA -> ChatFormatting.GOLD;
                case SOLID -> ChatFormatting.DARK_GRAY;
            };
            Component line = Component.literal(symbol(entry.type()) + "  x:" + signed(entry.offsetX()) + " z:" + signed(entry.offsetZ())
                    + "  ~" + entry.approxY())
                .withStyle(color);
            graphics.drawString(font, line, 80, listY + i * 10, 0xFFFFFF, false);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (startButton != null) {
            startButton.active = !menu.isScanRunning() && !menu.isAwaitingScanResult() && menu.getCooldownTicks() <= 0;
        }
    }

    private void renderMap(GuiGraphics graphics) {
        List<SeismicStationBlockEntity.MapEntry> entries = menu.getMapEntries();
        if (entries.isEmpty()) {
            return;
        }

        int mapLeft = leftPos + 80;
        int mapTop = topPos + 15;
        int centerX = mapLeft + MAP_SIZE / 2;
        int centerY = mapTop + MAP_SIZE / 2;
        int radius = Math.max(1, menu.getStationRadius());

        graphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xFF555555);
        for (SeismicStationBlockEntity.MapEntry entry : entries) {
            int px = centerX + Math.round((entry.offsetX() / (float) radius) * (MAP_SIZE / 2.0F - 2));
            int pz = centerY + Math.round((entry.offsetZ() / (float) radius) * (MAP_SIZE / 2.0F - 2));
            int color = switch (entry.type()) {
                case CAVE -> 0xFFC0C0C0;
                case WATER -> 0xFF3EA4F2;
                case LAVA -> 0xFFFF8A33;
                case SOLID -> 0xFF6F6A63;
            };
            graphics.fill(px - 1, pz - 1, px + 1, pz + 1, color);
        }
    }

    private Component statusText() {
        if (menu.isAwaitingScanResult()) {
            return Component.translatable("block.creategeoresonance.seismic_station.status_awaiting").withStyle(ChatFormatting.YELLOW);
        }
        if (menu.isScanRunning()) {
            if (menu.isStartingStrikeSequence()) {
                return Component.translatable("block.creategeoresonance.seismic_station.status_starting").withStyle(ChatFormatting.YELLOW);
            }
            if (!menu.hasRequiredSpeed()) {
                return Component.translatable("block.creategeoresonance.seismic_station.status_waiting_speed",
                    Config.STATION_MIN_SPEED.get()).withStyle(ChatFormatting.RED);
            }
            return Component.translatable("block.creategeoresonance.seismic_station.status_scanning").withStyle(ChatFormatting.GOLD);
        }
        if (menu.getCooldownTicks() > 0) {
            return Component.translatable("block.creategeoresonance.seismic_station.status_cooldown", menu.getCooldownTicks() / 20.0F)
                .withStyle(ChatFormatting.GRAY);
        }
        if (menu.isMapReady()) {
            return Component.translatable("block.creategeoresonance.seismic_station.status_ready_map").withStyle(ChatFormatting.AQUA);
        }
        if (!menu.hasRequiredSpeed()) {
            return Component.translatable("block.creategeoresonance.seismic_station.status_no_rotation",
                Config.STATION_MIN_SPEED.get()).withStyle(ChatFormatting.RED);
        }
        return Component.translatable("block.creategeoresonance.seismic_station.status_idle").withStyle(ChatFormatting.GREEN);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String symbol(SeismicAnomalyType type) {
        return switch (type) {
            case CAVE -> "C";
            case WATER -> "W";
            case LAVA -> "L";
            case SOLID -> "S";
        };
    }
}
