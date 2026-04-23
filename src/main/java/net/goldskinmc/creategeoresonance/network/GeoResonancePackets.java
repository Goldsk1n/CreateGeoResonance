package net.goldskinmc.creategeoresonance.network;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.goldskinmc.creategeoresonance.network.packet.C2SStartSeismicStationPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicImpactPacket;
import net.goldskinmc.creategeoresonance.network.packet.S2CSeismicResultPacket;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomaly;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class GeoResonancePackets {
    private static final String PROTOCOL_VERSION = "1";
    private static final double BROADCAST_RADIUS = 48.0D;
    private static int packetIndex = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(CreateGeoResonanceMod.MODID, "main"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private GeoResonancePackets() {
    }

    public static void register() {
        CHANNEL.messageBuilder(C2SStartSeismicStationPacket.class, packetIndex++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(C2SStartSeismicStationPacket::encode)
            .decoder(C2SStartSeismicStationPacket::decode)
            .consumerMainThread(C2SStartSeismicStationPacket::handle)
            .add();

        CHANNEL.messageBuilder(S2CSeismicImpactPacket.class, packetIndex++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(S2CSeismicImpactPacket::encode)
            .decoder(S2CSeismicImpactPacket::decode)
            .consumerMainThread(S2CSeismicImpactPacket::handle)
            .add();

        CHANNEL.messageBuilder(S2CSeismicResultPacket.class, packetIndex++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(S2CSeismicResultPacket::encode)
            .decoder(S2CSeismicResultPacket::decode)
            .consumerMainThread(S2CSeismicResultPacket::handle)
            .add();
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

    private static void sendToNearby(ServerLevel level, BlockPos origin, Object packet) {
        double maxDistanceSqr = BROADCAST_RADIUS * BROADCAST_RADIUS;
        double x = origin.getX() + 0.5D;
        double y = origin.getY() + 0.5D;
        double z = origin.getZ() + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= maxDistanceSqr) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
