package net.goldskinmc.creategeoresonance.network.packet;

import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SStartSeismicStationPacket(BlockPos stationPos) {
    public static void encode(C2SStartSeismicStationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.stationPos);
    }

    public static C2SStartSeismicStationPacket decode(FriendlyByteBuf buffer) {
        return new C2SStartSeismicStationPacket(buffer.readBlockPos());
    }

    public static void handle(C2SStartSeismicStationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.level().isLoaded(packet.stationPos)) {
                return;
            }
            if (player.distanceToSqr(packet.stationPos.getX() + 0.5D, packet.stationPos.getY() + 0.5D,
                packet.stationPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(packet.stationPos) instanceof SeismicStationBlockEntity station) {
                station.tryStartScan(player);
            }
        });
        context.setPacketHandled(true);
    }
}
