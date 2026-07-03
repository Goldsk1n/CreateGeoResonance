package net.goldskinmc.creategeoresonance.network;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.platform.CatnipServices;
import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicImpactPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CMountedStationStatePacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CMountedProjectorStatePacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicResultPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismogramMarkerPacket;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomaly;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class GeoResonancePackets {
    private static final String PROTOCOL_VERSION = "1";
    private static final double BROADCAST_RADIUS = 48.0D;
    private static final CatnipPacketRegistry REGISTRY = new CatnipPacketRegistry(CreateGeoResonanceMod.MODID, PROTOCOL_VERSION);

    public static final CatnipPacketRegistry.PacketType<S2CSeismicImpactPacket> SEISMIC_IMPACT = register(
        "seismic_impact",
        S2CSeismicImpactPacket.class,
        S2CSeismicImpactPacket.STREAM_CODEC
    );
    public static final CatnipPacketRegistry.PacketType<S2CSeismicResultPacket> SEISMIC_RESULT = register(
        "seismic_result",
        S2CSeismicResultPacket.class,
        S2CSeismicResultPacket.STREAM_CODEC
    );
    public static final CatnipPacketRegistry.PacketType<S2CSeismogramMarkerPacket> SEISMOGRAM_MARKER = register(
        "seismogram_marker",
        S2CSeismogramMarkerPacket.class,
        S2CSeismogramMarkerPacket.STREAM_CODEC
    );
    public static final CatnipPacketRegistry.PacketType<S2CMountedProjectorStatePacket> MOUNTED_PROJECTOR_STATE = register(
        "mounted_projector_state",
        S2CMountedProjectorStatePacket.class,
        S2CMountedProjectorStatePacket.STREAM_CODEC
    );
    public static final CatnipPacketRegistry.PacketType<S2CMountedStationStatePacket> MOUNTED_STATION_STATE = register(
        "mounted_station_state",
        S2CMountedStationStatePacket.class,
        S2CMountedStationStatePacket.STREAM_CODEC
    );

    private GeoResonancePackets() {
    }

    public static void register() {
        REGISTRY.registerAllPackets();
    }

    public static void sendSeismicImpact(ServerLevel level, BlockPos origin, int scannerEntityId, boolean lowPressure) {
        S2CSeismicImpactPacket packet = new S2CSeismicImpactPacket(origin, scannerEntityId, lowPressure);
        sendToNearby(level, origin, packet);
    }

    public static void sendSeismicResult(ServerLevel level, BlockPos origin, int scannerEntityId, boolean lowPressure, int maxDepth,
                                         List<SeismicAnomaly> anomalies) {
        S2CSeismicResultPacket packet = new S2CSeismicResultPacket(origin, scannerEntityId, lowPressure, maxDepth, anomalies);
        sendToNearby(level, origin, packet);
    }

    public static void sendSeismogramMarker(ServerPlayer player, int mapId, byte markerX, byte markerZ, byte markerRot) {
        CatnipServices.NETWORK.sendToClient(player, new S2CSeismogramMarkerPacket(mapId, markerX, markerZ, markerRot));
    }

    public static void sendMountedProjectorState(net.minecraft.world.entity.Entity contraptionEntity,
                                                 BlockPos localPos, CompoundTag blockEntityData) {
        CatnipServices.NETWORK.sendToClientsTrackingEntity(contraptionEntity,
            new S2CMountedProjectorStatePacket(contraptionEntity.getId(), localPos, blockEntityData.copy()));
    }

    public static void sendMountedStationState(net.minecraft.world.entity.Entity contraptionEntity,
                                               BlockPos localPos, CompoundTag blockEntityData) {
        CatnipServices.NETWORK.sendToClientsTrackingEntity(contraptionEntity,
            new S2CMountedStationStatePacket(contraptionEntity.getId(), localPos, blockEntityData.copy()));
    }

    private static void sendToNearby(ServerLevel level, BlockPos origin, CustomPacketPayload packet) {
        CatnipServices.NETWORK.sendToClientsAround(level, origin, BROADCAST_RADIUS, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        CatnipServices.NETWORK.sendToServer(packet);
    }

    private static <T extends BasePacketPayload> CatnipPacketRegistry.PacketType<T> register(String id, Class<T> clazz,
                                                                                              StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        CatnipPacketRegistry.PacketType<T> packetType = new CatnipPacketRegistry.PacketType<>(
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, id)),
            clazz,
            codec
        );
        REGISTRY.registerPacket(packetType);
        return packetType;
    }
}
