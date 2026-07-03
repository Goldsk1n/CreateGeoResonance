package net.goldskinmc.creategeoresonance.network.packet;

import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CSeismicImpactPacket(BlockPos origin, int scannerEntityId, boolean lowPressure) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSeismicImpactPacket> STREAM_CODEC =
        StreamCodec.of(S2CSeismicImpactPacket::encode, S2CSeismicImpactPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, S2CSeismicImpactPacket packet) {
        buffer.writeBlockPos(packet.origin);
        buffer.writeVarInt(packet.scannerEntityId);
        buffer.writeBoolean(packet.lowPressure);
    }

    private static S2CSeismicImpactPacket decode(RegistryFriendlyByteBuf buffer) {
        return new S2CSeismicImpactPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public void handle(LocalPlayer player) {
        net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleImpact(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.SEISMIC_IMPACT.type();
            }
        };
    }
}
