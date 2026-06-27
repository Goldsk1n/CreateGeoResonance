package net.goldskinmc.creategeoresonance.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMountedVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.function.Supplier;

public record S2CMountedStationStatePacket(int entityId, BlockPos localPos, CompoundTag blockEntityData) {
    public static void encode(S2CMountedStationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeNbt(packet.blockEntityData);
    }

    public static S2CMountedStationStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CMountedStationStatePacket(
            buffer.readVarInt(),
            buffer.readBlockPos(),
            buffer.readNbt()
        );
    }

    public static void handle(S2CMountedStationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            if (!(minecraft.level.getEntity(packet.entityId) instanceof AbstractContraptionEntity contraptionEntity)) {
                return;
            }

            MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor =
                contraptionEntity.getContraption().getActorAt(packet.localPos);
            if (actor == null) {
                return;
            }

            MovementContext movementContext = actor.getRight();
            CompoundTag previousData = movementContext.blockEntityData == null ? new CompoundTag() : movementContext.blockEntityData.copy();
            movementContext.blockEntityData = packet.blockEntityData.copy();
            if (SeismicStationBlock.isStationPart(actor.getLeft().state())) {
                actor.setLeft(new StructureTemplate.StructureBlockInfo(
                    actor.getLeft().pos(),
                    actor.getLeft().state(),
                    movementContext.blockEntityData.copy()
                ));
                SeismicStationMountedVisualState.getOrCreate(movementContext)
                    .syncFromState(previousData, movementContext.blockEntityData, minecraft.level.getGameTime());
            }
        });
        context.setPacketHandled(true);
    }
}
