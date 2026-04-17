package net.goldskinmc.creategeoresonance.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSeismicImpactPacket(BlockPos origin, int scannerEntityId, boolean lowPressure) {
    public static void encode(S2CSeismicImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.origin);
        buffer.writeVarInt(packet.scannerEntityId);
        buffer.writeBoolean(packet.lowPressure);
    }

    public static S2CSeismicImpactPacket decode(FriendlyByteBuf buffer) {
        return new S2CSeismicImpactPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(S2CSeismicImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleImpact(packet)));
        context.setPacketHandled(true);
    }
}
