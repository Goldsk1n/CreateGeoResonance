package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.goldskinmc.creategeoresonance.Config;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SeismicStationMountedRuntime {
    private SeismicStationMountedRuntime() {
    }

    public static boolean tryStartScan(ServerPlayer player, AbstractContraptionEntity contraptionEntity,
                                       BlockPos controllerLocalPos, SeismicStationData stationData) {
        if (!(contraptionEntity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!isParkedTrain(contraptionEntity)) {
            player.displayClientMessage(
                Component.translatable("block.creategeoresonance.seismic_station.mounted_requires_parked_train")
                    .withStyle(ChatFormatting.RED),
                true
            );
            return true;
        }
        if (stationData.scanRunning() || stationData.awaitingScanResult()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.busy")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!hasRequiredSpeed(contraptionEntity, controllerLocalPos)) {
            player.displayClientMessage(Component.translatable(
                    "block.creategeoresonance.seismic_station.no_rotation",
                    Config.STATION_MIN_SPEED.get())
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_PAPER_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_paper")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_INK_INPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.no_ink")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.output_full")
                .withStyle(ChatFormatting.RED), true);
            return true;
        }

        BlockPos scanOrigin = globalBlockPos(contraptionEntity, controllerLocalPos);
        stationData.inventory().extractItem(SeismicStationBlockEntity.SLOT_PAPER_INPUT, 1, false);
        stationData.inventory().extractItem(SeismicStationBlockEntity.SLOT_INK_INPUT, 1, false);
        stationData.setScanRunning(true);
        stationData.setAwaitingScanResult(true);
        stationData.setMapReady(false);
        stationData.setStrikeTimer(0);
        stationData.setRevealIndex(0);
        stationData.setCooldownTicks(0);
        stationData.setScanOrigin(scanOrigin);
        stationData.mapEntries().clear();
        stationData.exactClusters().clear();
        stationData.queuedAnomalies().clear();
        stationData.setCurrentScanDepth(Math.max(1, scanOrigin.getY() - serverLevel.getMinBuildHeight()));

        SeismicScanQueue.enqueue(new SeismicScanQueue.SeismicScanRequest(
            serverLevel,
            scanOrigin,
            contraptionEntity.getId(),
            Config.STATION_RADIUS.get(),
            stationData.currentScanDepth(),
            Config.STATION_NOISE.get().floatValue(),
            serverLevel.getGameTime(),
            false,
            buildDetectableTypes(stationData.inventory()),
            SeismicStationControllerLogic.hasModuleInstalled(stationData.inventory(), SeismicModuleType.BELOW_ZERO),
            (request, result) -> acceptScanResult(contraptionEntity.getId(), controllerLocalPos, request.level(), result)
        ));

        persistData(contraptionEntity, controllerLocalPos, stationData, true);
        player.displayClientMessage(Component.translatable("block.creategeoresonance.seismic_station.starting")
            .withStyle(ChatFormatting.GOLD), true);
        playFeedbackSound(serverLevel, scanOrigin, SoundEvents.LEVER_CLICK, 0.6F, 0.6F);
        spawnFeedbackParticles(serverLevel, scanOrigin, 8, 0.25D, 0.01D);
        return true;
    }

    public static void tick(MovementContext context) {
        if (context.world.isClientSide || context.contraption == null || context.contraption.entity == null) {
            return;
        }

        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        SeismicStationData stationData = new SeismicStationData();
        stationData.readFromTag(context.blockEntityData == null ? new CompoundTag() : context.blockEntityData);

        if (!stationData.scanRunning() || stationData.awaitingScanResult()) {
            return;
        }
        if (!isParkedTrain(contraptionEntity)) {
            return;
        }
        if (!hasRequiredSpeed(contraptionEntity, context.localPos)) {
            return;
        }
        if (stationData.strikeTimer() > 0) {
            stationData.setStrikeTimer(stationData.strikeTimer() - 1);
            persistData(contraptionEntity, context.localPos, stationData, false);
            return;
        }

        if (contraptionEntity.level() instanceof ServerLevel serverLevel) {
            replayStrike(serverLevel, contraptionEntity, context.localPos, stationData);
        }
    }

    public static boolean isParkedTrain(AbstractContraptionEntity contraptionEntity) {
        if (!(contraptionEntity instanceof CarriageContraptionEntity carriageContraptionEntity)) {
            return false;
        }
        if (carriageContraptionEntity.getCarriage() == null || carriageContraptionEntity.getCarriage().train == null) {
            return false;
        }

        var train = carriageContraptionEntity.getCarriage().train;
        return train.getCurrentStation() != null && Mth.equal((float) train.speed, 0.0F);
    }

    private static void acceptScanResult(int contraptionEntityId, BlockPos controllerLocalPos, ServerLevel level,
                                         SeismicScanQueue.SeismicScanResult result) {
        if (!(level.getEntity(contraptionEntityId) instanceof AbstractContraptionEntity contraptionEntity)) {
            return;
        }

        StructureTemplate.StructureBlockInfo controllerInfo = contraptionEntity.getContraption().getBlocks().get(controllerLocalPos);
        if (controllerInfo == null) {
            return;
        }

        SeismicStationData stationData = new SeismicStationData();
        stationData.readFromTag(controllerInfo.nbt() == null ? new CompoundTag() : controllerInfo.nbt());
        if (!stationData.scanRunning() || !stationData.awaitingScanResult()) {
            return;
        }

        List<SeismicAnomaly> prioritized = prioritizeByModuleOrder(stationData.inventory(), result.anomalies());
        prioritized.sort(Comparator.comparingInt(SeismicAnomaly::depth));
        stationData.queuedAnomalies().clear();
        stationData.queuedAnomalies().addAll(prioritized);
        stationData.exactClusters().clear();
        for (SeismicScanQueue.ExactCluster cluster : result.exactClusters()) {
            stationData.exactClusters().add(new SeismogramMapService.ExactCluster(cluster.type(), List.copyOf(cluster.blocks())));
        }
        stationData.setRevealIndex(0);
        stationData.setStrikeTimer(calculateStrikeIntervalTicks());
        stationData.setAwaitingScanResult(false);
        persistData(contraptionEntity, controllerLocalPos, stationData, true);
    }

    private static void replayStrike(ServerLevel serverLevel, AbstractContraptionEntity contraptionEntity,
                                     BlockPos controllerLocalPos, SeismicStationData stationData) {
        BlockPos scanOrigin = stationData.scanOrigin();
        if (scanOrigin == null) {
            stationData.setScanRunning(false);
            stationData.setAwaitingScanResult(false);
            persistData(contraptionEntity, controllerLocalPos, stationData, true);
            return;
        }

        if (stationData.queuedAnomalies().isEmpty()) {
            if (stationData.revealIndex() == 0) {
                GeoResonancePackets.sendSeismicImpact(serverLevel, scanOrigin, contraptionEntity.getId(), false);
            }
            finishReplayScan(serverLevel, contraptionEntity, controllerLocalPos, stationData);
            return;
        }

        if (stationData.revealIndex() >= stationData.queuedAnomalies().size()) {
            finishReplayScan(serverLevel, contraptionEntity, controllerLocalPos, stationData);
            return;
        }

        GeoResonancePackets.sendSeismicImpact(serverLevel, scanOrigin, contraptionEntity.getId(), false);
        SeismicAnomaly anomaly = stationData.queuedAnomalies().get(stationData.revealIndex());
        stationData.setRevealIndex(stationData.revealIndex() + 1);
        GeoResonancePackets.sendSeismicResult(
            serverLevel,
            scanOrigin,
            contraptionEntity.getId(),
            false,
            stationData.currentScanDepth(),
            List.of(anomaly)
        );

        if (stationData.revealIndex() >= stationData.queuedAnomalies().size()) {
            finishReplayScan(serverLevel, contraptionEntity, controllerLocalPos, stationData);
            return;
        }

        stationData.setStrikeTimer(calculateStrikeIntervalTicks());
        persistData(contraptionEntity, controllerLocalPos, stationData, true);
    }

    private static void finishReplayScan(ServerLevel serverLevel, AbstractContraptionEntity contraptionEntity,
                                         BlockPos controllerLocalPos, SeismicStationData stationData) {
        generateMapEntries(serverLevel, stationData);
        stationData.setScanRunning(false);
        stationData.setAwaitingScanResult(false);
        stationData.setCooldownTicks(0);
        stationData.setStrikeTimer(0);
        playFeedbackSound(serverLevel, globalBlockPos(contraptionEntity, controllerLocalPos), SoundEvents.LEVER_CLICK, 0.6F, 0.5F);
        stationData.queuedAnomalies().clear();
        persistData(contraptionEntity, controllerLocalPos, stationData, true);
    }

    private static void generateMapEntries(ServerLevel serverLevel, SeismicStationData stationData) {
        BlockPos scanOrigin = stationData.scanOrigin();
        if (scanOrigin == null) {
            return;
        }

        stationData.mapEntries().clear();
        for (SeismicAnomaly anomaly : stationData.queuedAnomalies()) {
            int spread = 5 + Mth.floor(anomaly.depth() / 20.0F);
            int sampledOffset = serverLevel.random.nextInt(spread * 2 + 1) - spread;
            int sampledY = scanOrigin.getY() - anomaly.depth() + sampledOffset;
            sampledY = Math.max(-64, sampledY);
            sampledY = Math.max(serverLevel.getMinBuildHeight(), sampledY);
            stationData.mapEntries().add(new SeismicStationBlockEntity.MapEntry(
                anomaly.type(),
                anomaly.offsetX(),
                anomaly.offsetZ(),
                sampledY
            ));
        }
        writeSeismogramOutput(serverLevel, stationData);
        stationData.setMapReady(true);
    }

    private static void writeSeismogramOutput(ServerLevel serverLevel, SeismicStationData stationData) {
        if (!stationData.inventory().getStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT).isEmpty()) {
            return;
        }
        BlockPos scanOrigin = stationData.scanOrigin();
        if (scanOrigin == null) {
            return;
        }
        ItemStack seismogram = SeismogramMapService.createMap(serverLevel, scanOrigin, stationData.mapEntries(), stationData.exactClusters());
        stationData.inventory().setStackInSlot(SeismicStationBlockEntity.SLOT_SEISMOGRAM_OUTPUT, seismogram);
        playFeedbackSound(serverLevel, scanOrigin, SoundEvents.BELL_BLOCK, 0.7F, 1.65F);
        spawnOutputParticles(serverLevel, scanOrigin);
        notifyNearbyPlayers(serverLevel, scanOrigin, "block.creategeoresonance.seismic_station.output_ready", ChatFormatting.AQUA);
    }

    private static void notifyNearbyPlayers(ServerLevel serverLevel, BlockPos pos, String messageKey, ChatFormatting style) {
        Component message = Component.translatable(messageKey).withStyle(style);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (ServerPlayer nearbyPlayer : serverLevel.players()) {
            if (nearbyPlayer.distanceToSqr(x, y, z) <= 24.0D * 24.0D) {
                nearbyPlayer.displayClientMessage(message, true);
            }
        }
    }

    private static void playFeedbackSound(ServerLevel serverLevel, BlockPos pos, net.minecraft.sounds.SoundEvent sound,
                                          float volume, float pitch) {
        serverLevel.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private static void spawnFeedbackParticles(ServerLevel serverLevel, BlockPos pos, int count, double spread, double speed) {
        serverLevel.sendParticles(ParticleTypes.CLOUD,
            pos.getX() + 0.5D,
            pos.getY() + 1.05D,
            pos.getZ() + 0.5D,
            count,
            spread,
            0.12D,
            spread,
            speed);
    }

    private static void spawnOutputParticles(ServerLevel serverLevel, BlockPos pos) {
        serverLevel.sendParticles(ParticleTypes.GLOW,
            pos.getX() + 0.5D,
            pos.getY() + 1.05D,
            pos.getZ() + 0.5D,
            12,
            0.25D,
            0.12D,
            0.25D,
            0.01D);
    }

    public static void persistData(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos,
                                   SeismicStationData stationData) {
        persistData(contraptionEntity, controllerLocalPos, stationData, true);
    }

    public static void persistData(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos,
                                   SeismicStationData stationData, boolean syncClients) {
        MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor =
            contraptionEntity.getContraption().getActorAt(controllerLocalPos);
        StructureTemplate.StructureBlockInfo controllerInfo = contraptionEntity.getContraption().getBlocks().get(controllerLocalPos);
        if (controllerInfo == null) {
            return;
        }

        CompoundTag updatedTag = new CompoundTag();
        stationData.writeToTag(updatedTag);
        StructureTemplate.StructureBlockInfo updatedInfo = new StructureTemplate.StructureBlockInfo(
            controllerLocalPos,
            controllerInfo.state(),
            updatedTag
        );

        if (actor != null) {
            actor.setLeft(updatedInfo);
            actor.getRight().blockEntityData = updatedTag;
        }
        contraptionEntity.setBlock(controllerLocalPos, updatedInfo);
        if (syncClients) {
            GeoResonancePackets.sendMountedStationState(contraptionEntity, controllerLocalPos, updatedTag);
        }
    }

    private static BlockPos globalBlockPos(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        return BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F));
    }

    private static boolean hasRequiredSpeed(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos) {
        return Math.abs(getOperationalSpeed(contraptionEntity, controllerLocalPos)) >= Config.STATION_MIN_SPEED.get();
    }

    private static float getOperationalSpeed(AbstractContraptionEntity contraptionEntity, BlockPos controllerLocalPos) {
        StructureTemplate.StructureBlockInfo controllerInfo = contraptionEntity.getContraption().getBlocks().get(controllerLocalPos);
        if (controllerInfo == null) {
            return 0.0F;
        }

        float fallbackSpeed = readMountedSpeed(controllerInfo.nbt());
        BlockState controllerState = controllerInfo.state();
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return fallbackSpeed;
        }

        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos inputLocalPos = controllerLocalPos.above();
        StructureTemplate.StructureBlockInfo inputInfo = contraptionEntity.getContraption().getBlocks().get(inputLocalPos);
        float inputSpeed = readMountedSpeed(inputInfo == null ? null : inputInfo.nbt(), fallbackSpeed);
        BlockPos sourceLocalPos = inputLocalPos.relative(facing.getOpposite());
        StructureTemplate.StructureBlockInfo sourceInfo = contraptionEntity.getContraption().getBlocks().get(sourceLocalPos);
        return readMountedSpeed(sourceInfo == null ? null : sourceInfo.nbt(), inputSpeed);
    }

    private static float readMountedSpeed(@Nullable CompoundTag tag) {
        return readMountedSpeed(tag, 0.0F);
    }

    private static float readMountedSpeed(@Nullable CompoundTag tag, float fallback) {
        if (tag == null || !tag.contains("Speed")) {
            return fallback;
        }
        return tag.getFloat("Speed");
    }

    private static Set<SeismicAnomalyType> buildDetectableTypes(net.minecraftforge.items.ItemStackHandler inventory) {
        EnumSet<SeismicAnomalyType> detectable = EnumSet.noneOf(SeismicAnomalyType.class);
        if (!SeismicStationControllerLogic.hasModuleInstalled(inventory, SeismicModuleType.NOISE_CANCELLATION)) {
            detectable.add(SeismicAnomalyType.CAVE);
            detectable.add(SeismicAnomalyType.WATER);
            detectable.add(SeismicAnomalyType.LAVA);
        }
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            SeismicAnomalyType detects = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(slot));
            if (detects != null) {
                detectable.add(detects);
            }
        }
        return detectable;
    }

    private static List<SeismicAnomaly> prioritizeByModuleOrder(net.minecraftforge.items.ItemStackHandler inventory,
                                                                List<SeismicAnomaly> anomalies) {
        if (anomalies.isEmpty()) {
            return anomalies;
        }

        Map<SeismicAnomalyType, Integer> modulePriority = buildModulePriorityMap(inventory);
        List<SeismicAnomaly> ordered = new ArrayList<>(anomalies);
        ordered.sort((left, right) -> comparePriority(left, right, modulePriority));

        int mergeDistance = Config.ECHO_MERGE_DISTANCE.get();
        List<SeismicAnomaly> selected = new ArrayList<>();
        for (SeismicAnomaly candidate : ordered) {
            int overlapIndex = findOverlappingAnomaly(selected, candidate, mergeDistance);
            if (overlapIndex < 0) {
                selected.add(candidate);
                continue;
            }

            SeismicAnomaly current = selected.get(overlapIndex);
            if (comparePriority(candidate, current, modulePriority) < 0) {
                selected.set(overlapIndex, candidate);
            }
        }
        return selected;
    }

    private static Map<SeismicAnomalyType, Integer> buildModulePriorityMap(net.minecraftforge.items.ItemStackHandler inventory) {
        Map<SeismicAnomalyType, Integer> priority = new HashMap<>();
        for (int slot = SeismicStationBlockEntity.SLOT_MODULE_START; slot <= SeismicStationBlockEntity.SLOT_MODULE_END; slot++) {
            SeismicAnomalyType type = SeismicModuleItem.getDetectsType(inventory.getStackInSlot(slot));
            if (type != null) {
                priority.put(type, slot);
            }
        }
        return priority;
    }

    private static int comparePriority(SeismicAnomaly left, SeismicAnomaly right,
                                       Map<SeismicAnomalyType, Integer> modulePriority) {
        int typeCompare = Integer.compare(typePriorityScore(left.type(), modulePriority), typePriorityScore(right.type(), modulePriority));
        if (typeCompare != 0) {
            return -typeCompare;
        }
        return -Double.compare(anomalyStrength(left), anomalyStrength(right));
    }

    private static int findOverlappingAnomaly(List<SeismicAnomaly> selected, SeismicAnomaly candidate, int mergeDistance) {
        for (int i = 0; i < selected.size(); i++) {
            SeismicAnomaly existing = selected.get(i);
            int effectiveMergeDistance = Math.max(mergeDistance, (existing.radius() + candidate.radius()) / 2);
            int dx = existing.offsetX() - candidate.offsetX();
            int dz = existing.offsetZ() - candidate.offsetZ();
            if (dx * dx + dz * dz <= effectiveMergeDistance * effectiveMergeDistance) {
                return i;
            }
        }
        return -1;
    }

    private static int typePriorityScore(SeismicAnomalyType type, Map<SeismicAnomalyType, Integer> modulePriority) {
        Integer slotPriority = modulePriority.get(type);
        if (slotPriority != null) {
            return 1000 + slotPriority;
        }

        return switch (type) {
            case LAVA -> 30;
            case WATER -> 20;
            case CAVE -> 10;
            default -> 0;
        };
    }

    private static double anomalyStrength(SeismicAnomaly anomaly) {
        return anomaly.confidence() * (1.0D + anomaly.radius() * 0.2D);
    }

    public static int calculateStrikeIntervalTicks() {
        int intervalMultiplier = Math.max(1, Config.STATION_STRIKE_INTERVAL_MULTIPLIER.get());
        int baseInterval = Config.STATION_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier;
        int minInterval = Math.min(baseInterval, Config.STATION_MIN_STRIKE_INTERVAL_TICKS.get() * intervalMultiplier);
        return Math.max(1, minInterval);
    }
}
