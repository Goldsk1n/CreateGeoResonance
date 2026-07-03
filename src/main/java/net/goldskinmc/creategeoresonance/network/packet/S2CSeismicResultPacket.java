package net.goldskinmc.creategeoresonance.network.packet;

import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomaly;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record S2CSeismicResultPacket(BlockPos origin, int scannerEntityId, boolean lowPressure, int maxDepth,
                                     List<SeismicAnomaly> anomalies) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSeismicResultPacket> STREAM_CODEC =
        StreamCodec.of(S2CSeismicResultPacket::encode, S2CSeismicResultPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, S2CSeismicResultPacket packet) {
        buffer.writeBlockPos(packet.origin);
        buffer.writeVarInt(packet.scannerEntityId);
        buffer.writeBoolean(packet.lowPressure);
        buffer.writeVarInt(packet.maxDepth);
        buffer.writeVarInt(packet.anomalies.size());
        for (SeismicAnomaly anomaly : packet.anomalies) {
            buffer.writeEnum(anomaly.type());
            buffer.writeVarInt(anomaly.offsetX());
            buffer.writeVarInt(anomaly.offsetZ());
            buffer.writeVarInt(anomaly.depth());
            buffer.writeVarInt(anomaly.radius());
            buffer.writeFloat(anomaly.confidence());
        }
    }

    private static S2CSeismicResultPacket decode(RegistryFriendlyByteBuf buffer) {
        BlockPos origin = buffer.readBlockPos();
        int scannerEntityId = buffer.readVarInt();
        boolean lowPressure = buffer.readBoolean();
        int maxDepth = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<SeismicAnomaly> anomalies = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SeismicAnomalyType type = buffer.readEnum(SeismicAnomalyType.class);
            int offsetX = buffer.readVarInt();
            int offsetZ = buffer.readVarInt();
            int depth = buffer.readVarInt();
            int radius = buffer.readVarInt();
            float confidence = buffer.readFloat();
            anomalies.add(new SeismicAnomaly(type, offsetX, offsetZ, depth, radius, confidence));
        }
        return new S2CSeismicResultPacket(origin, scannerEntityId, lowPressure, maxDepth, anomalies);
    }

    @Override
    public void handle(LocalPlayer player) {
        net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleResult(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.SEISMIC_RESULT.type();
            }
        };
    }
}
