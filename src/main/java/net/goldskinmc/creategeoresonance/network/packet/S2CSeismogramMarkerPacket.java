package net.goldskinmc.creategeoresonance.network.packet;

import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CSeismogramMarkerPacket(int mapId, byte markerX, byte markerZ, byte markerRot) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSeismogramMarkerPacket> STREAM_CODEC =
        StreamCodec.of(S2CSeismogramMarkerPacket::encode, S2CSeismogramMarkerPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, S2CSeismogramMarkerPacket packet) {
        buffer.writeVarInt(packet.mapId);
        buffer.writeByte(packet.markerX);
        buffer.writeByte(packet.markerZ);
        buffer.writeByte(packet.markerRot);
    }

    private static S2CSeismogramMarkerPacket decode(RegistryFriendlyByteBuf buffer) {
        return new S2CSeismogramMarkerPacket(buffer.readVarInt(), buffer.readByte(), buffer.readByte(), buffer.readByte());
    }

    @Override
    public void handle(LocalPlayer player) {
        net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleSeismogramMarker(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.SEISMOGRAM_MARKER.type();
            }
        };
    }
}
