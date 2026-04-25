package net.goldskinmc.creategeoresonance.network.packet;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public record S2CSeismogramMarkerPacket(int mapId, byte markerX, byte markerZ, byte markerRot) {
    public static void encode(S2CSeismogramMarkerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mapId);
        buffer.writeByte(packet.markerX);
        buffer.writeByte(packet.markerZ);
        buffer.writeByte(packet.markerRot);
    }

    public static S2CSeismogramMarkerPacket decode(FriendlyByteBuf buffer) {
        return new S2CSeismogramMarkerPacket(buffer.readVarInt(), buffer.readByte(), buffer.readByte(), buffer.readByte());
    }

    public static void handle(S2CSeismogramMarkerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleSeismogramMarker(packet)));
        context.setPacketHandled(true);
    }
}
