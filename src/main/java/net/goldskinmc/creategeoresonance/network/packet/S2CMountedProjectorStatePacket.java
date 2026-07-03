package net.goldskinmc.creategeoresonance.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.goldskinmc.creategeoresonance.network.GeoResonancePackets;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

public record S2CMountedProjectorStatePacket(int entityId, BlockPos localPos,
                                             CompoundTag blockEntityData) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMountedProjectorStatePacket> STREAM_CODEC =
        StreamCodec.of(S2CMountedProjectorStatePacket::encode, S2CMountedProjectorStatePacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, S2CMountedProjectorStatePacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeNbt(packet.blockEntityData);
    }

    private static S2CMountedProjectorStatePacket decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMountedProjectorStatePacket(
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
        movementContext.blockEntityData = blockEntityData.copy();
        if (actor.getLeft().state().getBlock() instanceof SeismicProjectorBlock) {
            actor.setLeft(new StructureTemplate.StructureBlockInfo(
                actor.getLeft().pos(),
                actor.getLeft().state(),
                movementContext.blockEntityData.copy()
            ));
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return new PacketTypeProvider() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
                return (CustomPacketPayload.Type<T>) GeoResonancePackets.MOUNTED_PROJECTOR_STATE.type();
            }
        };
    }
}
