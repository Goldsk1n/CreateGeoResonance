package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SeismicStationBlock extends HorizontalKineticBlock implements IBE<SeismicStationBlockEntity> {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public SeismicStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        GeoResonanceMenus.SEISMIC_STATION.open(
            serverPlayer,
            Component.translatable("block.creategeoresonance.seismic_station"),
            (windowId, inventory, p) -> new SeismicStationMenu(
                GeoResonanceMenus.SEISMIC_STATION.get(),
                windowId,
                inventory,
                pos
            ),
            buffer -> buffer.writeBlockPos(pos)
        );
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, SeismicStationBlockEntity::dropInventory);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<SeismicStationBlockEntity> getBlockEntityClass() {
        return SeismicStationBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeismicStationBlockEntity> getBlockEntityType() {
        return GeoResonanceBlockEntityTypes.SEISMIC_STATION.get();
    }
}
