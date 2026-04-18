package net.goldskinmc.creategeoresonance.network.packet;

import net.goldskinmc.creategeoresonance.seismic.SeismicAnomaly;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CSeismicResultPacket(BlockPos origin, int scannerEntityId, boolean lowPressure, int maxDepth,
                                     List<SeismicAnomaly> anomalies) {
    public static void encode(S2CSeismicResultPacket packet, FriendlyByteBuf buffer) {
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

    public static S2CSeismicResultPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(S2CSeismicResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.goldskinmc.creategeoresonance.client.GeoResonanceClient.handleResult(packet)));
        context.setPacketHandled(true);
    }
}
