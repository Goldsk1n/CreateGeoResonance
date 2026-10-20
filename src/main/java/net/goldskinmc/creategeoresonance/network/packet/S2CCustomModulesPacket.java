package net.goldskinmc.creategeoresonance.network.packet;

import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2CCustomModulesPacket(CompoundTag data) {
    public static void encode(S2CCustomModulesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static S2CCustomModulesPacket decode(FriendlyByteBuf buffer) {
        return new S2CCustomModulesPacket(java.util.Objects.requireNonNull(buffer.readNbt(), "Missing module definitions"));
    }

    public static void handle(S2CCustomModulesPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> SeismicCustomModules.readDefinitions(packet.data));
        context.setPacketHandled(true);
    }
}
