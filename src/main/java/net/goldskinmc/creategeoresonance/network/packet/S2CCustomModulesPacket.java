package net.goldskinmc.creategeoresonance.network.packet;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.seismic.SeismicCustomModules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record S2CCustomModulesPacket(CompoundTag data) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCustomModulesPacket> STREAM_CODEC =
        StreamCodec.of((buffer, packet) -> buffer.writeNbt(packet.data),
            buffer -> new S2CCustomModulesPacket(java.util.Objects.requireNonNull(buffer.readNbt(), "Missing module definitions")));

    @Override
    public void handle(LocalPlayer player) {
        SeismicCustomModules.readDefinitions(data);
    }

    @Override
    public void handleInternal(net.minecraft.world.entity.player.Player player) {
        // Initial datapack sync can arrive before the local player exists.
        SeismicCustomModules.readDefinitions(data);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.CUSTOM_MODULES.type();
            }
        };
    }
}
