package net.goldskinmc.creategeoresonance.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMountedVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

public record S2CMountedStationStatePacket(int entityId, BlockPos localPos,
                                           CompoundTag blockEntityData) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMountedStationStatePacket> STREAM_CODEC =
        StreamCodec.of(S2CMountedStationStatePacket::encode, S2CMountedStationStatePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, S2CMountedStationStatePacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeNbt(packet.blockEntityData);
    }

    private static S2CMountedStationStatePacket decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMountedStationStatePacket(
            buffer.readVarInt(),
            buffer.readBlockPos(),
            buffer.readNbt()
        );
    }

    @Override
    public void handle(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (!(minecraft.level.getEntity(entityId) instanceof AbstractContraptionEntity contraptionEntity)) {
            return;
        }

        MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor =
            contraptionEntity.getContraption().getActorAt(localPos);
        if (actor == null) {
            return;
        }

        MovementContext movementContext = actor.getRight();
        CompoundTag previousData = movementContext.blockEntityData == null ? new CompoundTag() : movementContext.blockEntityData.copy();
        movementContext.blockEntityData = blockEntityData.copy();
        if (SeismicStationBlock.isStationPart(actor.getLeft().state())) {
            actor.setLeft(new StructureTemplate.StructureBlockInfo(
                actor.getLeft().pos(),
                actor.getLeft().state(),
                movementContext.blockEntityData.copy()
            ));
            SeismicStationMountedVisualState.getOrCreate(movementContext)
                .syncFromState(minecraft.level.registryAccess(), previousData, movementContext.blockEntityData, minecraft.level.getGameTime());
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.MOUNTED_STATION_STATE.type();
            }
        };
    }
}
