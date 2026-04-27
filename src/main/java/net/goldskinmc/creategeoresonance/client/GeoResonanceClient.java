package net.goldskinmc.creategeoresonance.client;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.client.effect.SeismicWaveEffect;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicImpactPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicResultPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismogramMarkerPacket;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceSoundEvents;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomaly;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismogramMapService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class GeoResonanceClient {
    private static final List<ScheduledEcho> PENDING_ECHOES = new ArrayList<>();
    private static final List<ScheduledSound> PENDING_SOUNDS = new ArrayList<>();
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();
    private static final List<ActiveShake> ACTIVE_SHAKES = new ArrayList<>();
    private static final int SEISMOGRAM_SIDEBAR_MAX_SIGNALS = 5;
    private static final int SEISMOGRAM_SIDEBAR_ROW_HEIGHT = 10;
    private static final int SEISMOGRAM_SIDEBAR_WIDTH = 132;
    private static ClientLevel PREWARMED_LEVEL;
    private static boolean WAVE_RENDERER_PREWARMED;

    private GeoResonanceClient() {
    }

    public static void register() {
        ItemProperties.register(Items.FILLED_MAP,
            ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "seismogram"),
            (stack, level, entity, seed) -> SeismogramMapService.isSeismogramStack(stack) ? 1.0F : 0.0F);
        GeoResonancePartialModels.init();
        MinecraftForge.EVENT_BUS.addListener(GeoResonanceClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(GeoResonanceClient::onCameraAngles);
        MinecraftForge.EVENT_BUS.addListener(GeoResonanceClient::onRenderGuiOverlay);
    }

    public static void handleImpact(S2CSeismicImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        Vec3 sourceAnchor = sourceAnchor(level, packet.origin(), packet.scannerEntityId());
        Vec3 center = resolveWaveCenter(level, packet.origin(), sourceAnchor);
        float blur = packet.lowPressure() ? 0.45F : 0.2F;
        float opacity = packet.lowPressure() ? 0.55F : 0.75F;
        spawnWave(level, center, 0xC2C2C2, opacity, blur, 16, 6.5F);

        if (packet.lowPressure()) {
            level.playLocalSound(center.x, center.y, center.z, GeoResonanceSoundEvents.SEISMIC_HAMMER_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.2F, false);
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.8F, 1.25F, false);
        } else {
            level.playLocalSound(center.x, center.y, center.z, GeoResonanceSoundEvents.SEISMIC_HAMMER_HIT.get(), SoundSource.PLAYERS, 1.25F, 0.95F, false);
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.65F, 0.58F, false);
        }

        if (packet.scannerEntityId() == -1 && level.getBlockEntity(packet.origin()) instanceof SeismicStationBlockEntity station) {
            station.onClientStrikeImpact();
        }

        addShakeForLocalPlayer(packet.scannerEntityId(), center, 1.0F, 0.2F, 8, level.getGameTime());
    }

    public static void handleResult(S2CSeismicResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        long now = level.getGameTime();
        for (SeismicAnomaly anomaly : packet.anomalies()) {
            int delay = Math.max(1, Math.round((anomaly.depth() / (float) packet.maxDepth()) * Config.MAX_ECHO_DELAY_TICKS.get()));
            PENDING_ECHOES.add(new ScheduledEcho(packet.origin(), packet.scannerEntityId(), packet.maxDepth(), packet.lowPressure(), anomaly, now + delay));
        }
    }

    public static void handleSeismogramMarker(S2CSeismogramMarkerPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        MapItemSavedData mapData = MapItem.getSavedData(packet.mapId(), level);
        if (mapData == null) {
            return;
        }
        mapData.addClientSideDecorations(List.of(
            new MapDecoration(MapDecoration.Type.PLAYER, packet.markerX(), packet.markerZ(), packet.markerRot(), null)
        ));
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            PENDING_ECHOES.clear();
            PENDING_SOUNDS.clear();
            ACTIVE_EFFECTS.clear();
            ACTIVE_SHAKES.clear();
            PREWARMED_LEVEL = null;
            WAVE_RENDERER_PREWARMED = false;
            return;
        }

        long now = level.getGameTime();
        if (PREWARMED_LEVEL != level) {
            PREWARMED_LEVEL = level;
            WAVE_RENDERER_PREWARMED = false;
        }
        if (!WAVE_RENDERER_PREWARMED) {
            prewarmWaveRenderer(level, now);
        }

        for (Iterator<ScheduledSound> iterator = PENDING_SOUNDS.iterator(); iterator.hasNext(); ) {
            ScheduledSound scheduledSound = iterator.next();
            if (now < scheduledSound.executeTick()) {
                continue;
            }
            level.playLocalSound(scheduledSound.x(), scheduledSound.y(), scheduledSound.z(), scheduledSound.sound(),
                SoundSource.PLAYERS, scheduledSound.volume(), scheduledSound.pitch(), false);
            iterator.remove();
        }

        for (Iterator<ScheduledEcho> iterator = PENDING_ECHOES.iterator(); iterator.hasNext(); ) {
            ScheduledEcho echo = iterator.next();
            if (now < echo.executeTick()) {
                continue;
            }
            playEcho(level, echo);
            iterator.remove();
        }

        for (Iterator<ActiveEffect> iterator = ACTIVE_EFFECTS.iterator(); iterator.hasNext(); ) {
            ActiveEffect active = iterator.next();
            if (active.expiresAtTick() > now) {
                continue;
            }
            VisualizationManager manager = VisualizationManager.get(level);
            if (manager != null) {
                manager.effects().queueRemove(active.effect());
            }
            iterator.remove();
        }

        ACTIVE_SHAKES.removeIf(shake -> shake.endsAtTick() <= now);
    }

    private static void playEcho(ClientLevel level, ScheduledEcho scheduled) {
        SeismicAnomaly anomaly = scheduled.anomaly();
        BlockPos echoPos = scheduled.origin().offset(anomaly.offsetX(), 0, anomaly.offsetZ());
        Vec3 sourceAnchor = sourceAnchor(level, scheduled.origin(), scheduled.scannerEntityId());
        Vec3 center = resolveWaveCenter(level, echoPos, sourceAnchor);

        float confidence = Mth.clamp(anomaly.confidence(), 0.0F, 1.0F);
        float blur = Math.max(0.05F, 1.0F - confidence);
        if (scheduled.lowPressure()) {
            blur = Math.max(blur, 0.6F);
        }
        int color = scheduled.lowPressure() ? 0x8A8A8A : switch (anomaly.type()) {
            case CAVE -> 0xB0B0B0;
            case WATER -> 0x3EA4F2;
            case LAVA -> 0xFF8A33;
            case SOLID -> 0x8E8378;
        };
        float opacity = scheduled.lowPressure() ? 0.35F + confidence * 0.35F : 0.5F + confidence * 0.5F;
        float radius = Math.max(2.0F, anomaly.radius() + confidence * 2.0F + blur);
        spawnWave(level, center, color, opacity, blur, 12, radius);

        float depthRatio = scheduled.maxDepth() > 0 ? anomaly.depth() / (float) scheduled.maxDepth() : 1.0F;
        SoundEvent echoSound = scheduled.lowPressure() ? SoundEvents.NOTE_BLOCK_HAT.value() : switch (anomaly.type()) {
            case CAVE -> SoundEvents.BELL_BLOCK;
            case WATER -> SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT;
            case LAVA -> SoundEvents.LAVA_POP;
            case SOLID -> SoundEvents.NOTE_BLOCK_BASS.value();
        };
        float basePitch = scheduled.lowPressure() ? 0.9F : switch (anomaly.type()) {
            case CAVE -> 1.12F;
            case WATER -> 0.84F;
            case LAVA -> 0.66F;
            case SOLID -> 0.56F;
        };
        float pitch = Mth.clamp(basePitch - depthRatio * 0.22F, 0.25F, 2.0F);
        if (scheduled.lowPressure()) {
            level.playLocalSound(center.x, center.y, center.z, echoSound, SoundSource.PLAYERS, 0.6F, pitch, false);
        } else if (anomaly.type() == SeismicAnomalyType.WATER) {
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.PLAYERS, 0.95F, pitch, false);
        } else if (anomaly.type() == SeismicAnomalyType.LAVA) {
            float pitchA = Mth.clamp(pitch, 0.25F, 2.0F);
            float pitchB = Mth.clamp(pitch + 0.13F, 0.25F, 2.0F);
            float pitchC = Mth.clamp(pitch - 0.1F, 0.25F, 2.0F);
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.1F, pitchA, false);
            scheduleLocalSound(level.getGameTime() + 4, center, SoundEvents.LAVA_POP, 0.95F, pitchB);
            scheduleLocalSound(level.getGameTime() + 8, center, SoundEvents.LAVA_POP, 0.8F, pitchC);
        } else {
            level.playLocalSound(center.x, center.y, center.z, echoSound, SoundSource.PLAYERS, 0.8F, pitch, false);
        }

        addShakeForLocalPlayer(scheduled.scannerEntityId(), center, 0.18F * confidence, 0.05F * confidence, 6, level.getGameTime());
    }

    private static void scheduleLocalSound(long executeTick, Vec3 center, SoundEvent sound, float volume, float pitch) {
        PENDING_SOUNDS.add(new ScheduledSound(executeTick, center.x, center.y, center.z, sound, volume, pitch));
    }

    private static void prewarmWaveRenderer(ClientLevel level, long now) {
        if (!VisualizationManager.supportsVisualization(level)) {
            WAVE_RENDERER_PREWARMED = true;
            return;
        }
        VisualizationManager manager = VisualizationManager.get(level);
        if (manager == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 center = minecraft.player != null
            ? minecraft.player.position().add(0.0D, 0.05D, 0.0D)
            : new Vec3(0.5D, level.getMinBuildHeight() + 1.05D, 0.5D);
        SeismicWaveEffect effect = new SeismicWaveEffect(level, center, 0xFFFFFF, 0.0F, 0.2F, 1, 0.2F);
        manager.effects().queueAdd(effect);
        ACTIVE_EFFECTS.add(new ActiveEffect(effect, now + 2));
        WAVE_RENDERER_PREWARMED = true;
    }

    private static void spawnWave(ClientLevel level, Vec3 center, int rgb, float opacity, float blur, int lifetimeTicks, float maxRadius) {
        if (VisualizationManager.supportsVisualization(level)) {
            VisualizationManager manager = VisualizationManager.get(level);
            if (manager != null) {
                SeismicWaveEffect effect = new SeismicWaveEffect(level, center, rgb, opacity, blur, lifetimeTicks, maxRadius);
                manager.effects().queueAdd(effect);
                ACTIVE_EFFECTS.add(new ActiveEffect(effect, level.getGameTime() + lifetimeTicks + 1));
                return;
            }
        }

        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0D * i) / 12.0D;
            double radius = 0.5D + maxRadius * 0.25D;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.addParticle(new DustParticleOptions(new Vector3f(red, green, blue), 1.0F), px, center.y + 0.05D, pz,
                0.0D, 0.01D, 0.0D);
        }
    }

    private static void addShakeForLocalPlayer(int scannerEntityId, Vec3 center, float scannerStrength, float nearbyStrength, int durationTicks,
                                                long now) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        float strength = 0.0F;
        if (minecraft.player.getId() == scannerEntityId) {
            strength = scannerStrength;
        } else if (minecraft.player.position().distanceToSqr(center) <= 25.0D) {
            strength = nearbyStrength;
        }

        if (strength > 0.0F) {
            ACTIVE_SHAKES.add(new ActiveShake(now, now + durationTicks, strength, minecraft.level.random.nextFloat() * 9.0F));
        }
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ACTIVE_SHAKES.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            ACTIVE_SHAKES.clear();
            return;
        }

        float partialTick = (float) event.getPartialTick();
        long now = level.getGameTime();
        float yawOffset = 0.0F;
        float pitchOffset = 0.0F;

        for (ActiveShake shake : ACTIVE_SHAKES) {
            float age = (now - shake.startsAtTick()) + partialTick;
            float duration = Math.max(1.0F, shake.endsAtTick() - shake.startsAtTick());
            float progress = Mth.clamp(age / duration, 0.0F, 1.0F);
            float envelope = 1.0F - progress;
            float wave = (float) Math.sin((age + shake.phaseSeed()) * 2.4F);
            float amount = shake.intensity() * envelope * wave;
            yawOffset += amount * 1.5F;
            pitchOffset += amount * 1.1F;
        }

        event.setYaw(event.getYaw() + yawOffset);
        event.setPitch(event.getPitch() + pitchOffset);
    }

    private static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        SeismogramMapService.MapSnapshot snapshot = findHeldSeismogramSnapshot(minecraft);
        if (snapshot == null || snapshot.signals().isEmpty()) {
            return;
        }

        List<SignalDistance> nearestSignals = buildNearestSignals(snapshot, minecraft.player.getX(), minecraft.player.getZ());
        if (nearestSignals.isEmpty()) {
            return;
        }

        renderSeismogramSidebar(event.getGuiGraphics(), minecraft, nearestSignals);
    }

    private static Vec3 sourceAnchor(ClientLevel level, BlockPos origin, int scannerEntityId) {
        Vec3 fallback = new Vec3(origin.getX() + 0.5D, origin.getY() + 0.05D, origin.getZ() + 0.5D);
        if (scannerEntityId == -1) {
            return findNearestOpen(level, fallback, 2, 2).orElse(fallback);
        }
        Entity scanner = level.getEntity(scannerEntityId);
        if (scanner == null) {
            return findNearestOpen(level, fallback, 2, 2).orElse(fallback);
        }
        Vec3 scannerAnchor = new Vec3(scanner.getX(), scanner.getY() + 0.05D, scanner.getZ());
        return findNearestOpen(level, scannerAnchor, 2, 2).orElse(scannerAnchor);
    }

    private static Vec3 resolveWaveCenter(ClientLevel level, BlockPos targetPos, Vec3 sourceAnchor) {
        Vec3 target = new Vec3(targetPos.getX() + 0.5D, sourceAnchor.y, targetPos.getZ() + 0.5D);
        var localOpen = findNearestOpen(level, target, 3, 2);
        if (localOpen.isPresent()) {
            return localOpen.get();
        }

        Vec3 lineDelta = sourceAnchor.subtract(target);
        for (int i = 1; i <= 12; i++) {
            double t = i / 12.0D;
            Vec3 candidate = target.add(lineDelta.scale(t));
            var lineOpen = findNearestOpen(level, candidate, 1, 1);
            if (lineOpen.isPresent()) {
                return lineOpen.get();
            }
        }

        return findNearestOpen(level, sourceAnchor, 2, 2).orElse(sourceAnchor);
    }

    private static java.util.Optional<Vec3> findNearestOpen(ClientLevel level, Vec3 center, int horizontalRadius, int verticalRadius) {
        int baseX = Mth.floor(center.x);
        int baseY = Mth.floor(center.y);
        int baseZ = Mth.floor(center.z);
        for (int r = 0; r <= horizontalRadius; r++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos testPos = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                        if (canRenderAt(level, testPos)) {
                            return java.util.Optional.of(new Vec3(testPos.getX() + 0.5D, testPos.getY() + 0.05D, testPos.getZ() + 0.5D));
                        }
                    }
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean canRenderAt(ClientLevel level, BlockPos pos) {
        return !level.getBlockState(pos).canOcclude() && !level.getBlockState(pos.above()).canOcclude();
    }

    @Nullable
    private static SeismogramMapService.MapSnapshot findHeldSeismogramSnapshot(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        SeismogramMapService.MapSnapshot mainSnapshot = SeismogramMapService.readSnapshot(mainHand);
        if (mainSnapshot != null) {
            return mainSnapshot;
        }
        ItemStack offhand = minecraft.player.getOffhandItem();
        return SeismogramMapService.readSnapshot(offhand);
    }

    private static List<SignalDistance> buildNearestSignals(SeismogramMapService.MapSnapshot snapshot, double playerX, double playerZ) {
        return snapshot.signals().stream()
            .map(signal -> toSignalDistance(snapshot, signal, playerX, playerZ))
            .sorted(Comparator.comparingInt(SignalDistance::horizontalDistance))
            .limit(SEISMOGRAM_SIDEBAR_MAX_SIGNALS)
            .toList();
    }

    private static SignalDistance toSignalDistance(SeismogramMapService.MapSnapshot snapshot, SeismogramMapService.MapSignal signal,
                                                   double playerX, double playerZ) {
        double dx = playerX - (signal.worldX() + 0.5D);
        double dz = playerZ - (signal.worldZ() + 0.5D);
        int horizontalDistance = Mth.floor(Math.sqrt(dx * dx + dz * dz));
        int approxDepth = Math.max(0, snapshot.centerY() - signal.approxY());
        return new SignalDistance(signal.type(), horizontalDistance, approxDepth);
    }

    private static void renderSeismogramSidebar(GuiGraphics graphics, Minecraft minecraft, List<SignalDistance> nearestSignals) {
        int rows = nearestSignals.size() + 1;
        int panelHeight = 8 + rows * SEISMOGRAM_SIDEBAR_ROW_HEIGHT;
        int left = minecraft.getWindow().getGuiScaledWidth() - SEISMOGRAM_SIDEBAR_WIDTH - 8;
        int top = (minecraft.getWindow().getGuiScaledHeight() - panelHeight) / 2;
        graphics.fill(left, top, left + SEISMOGRAM_SIDEBAR_WIDTH, top + panelHeight, 0x7A1A1A1A);

        graphics.drawString(minecraft.font, Component.translatable("item.creategeoresonance.seismogram.sidebar.title"),
            left + 4, top + 3, 0xF7EED7, false);
        int y = top + 3 + SEISMOGRAM_SIDEBAR_ROW_HEIGHT;
        for (SignalDistance signalDistance : nearestSignals) {
            Component line = Component.translatable(
                "item.creategeoresonance.seismogram.sidebar.line",
                Component.translatable(signalTypeKey(signalDistance.type())),
                signalDistance.horizontalDistance(),
                signalDistance.approxDepth()
            );
            graphics.drawString(minecraft.font, line, left + 4, y, signalTypeColor(signalDistance.type()), false);
            y += SEISMOGRAM_SIDEBAR_ROW_HEIGHT;
        }
    }

    private static int signalTypeColor(SeismicAnomalyType type) {
        return switch (type) {
            case CAVE -> 0xCFCFCF;
            case WATER -> 0x7FC6FF;
            case LAVA -> 0xFFB061;
            case SOLID -> 0xA19485;
        };
    }

    private static String signalTypeKey(SeismicAnomalyType type) {
        return switch (type) {
            case CAVE -> "item.creategeoresonance.seismogram.signal.cave";
            case WATER -> "item.creategeoresonance.seismogram.signal.water";
            case LAVA -> "item.creategeoresonance.seismogram.signal.lava";
            case SOLID -> "item.creategeoresonance.seismogram.signal.solid";
        };
    }

    private record ScheduledEcho(BlockPos origin, int scannerEntityId, int maxDepth, boolean lowPressure, SeismicAnomaly anomaly,
                                 long executeTick) {
    }

    private record ScheduledSound(long executeTick, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
    }

    private record ActiveEffect(SeismicWaveEffect effect, long expiresAtTick) {
    }

    private record ActiveShake(long startsAtTick, long endsAtTick, float intensity, float phaseSeed) {
    }

    private record SignalDistance(SeismicAnomalyType type, int horizontalDistance, int approxDepth) {
    }
}
