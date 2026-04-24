package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SeismicStationBoundingBlock extends HorizontalKineticBlock implements IBE<SeismicStationBoundingBlockEntity> {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BoundingPart> PART = EnumProperty.create("part", BoundingPart.class);

    public SeismicStationBoundingBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(PART, BoundingPart.LOWER_LEFT));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (state.getValue(PART) != BoundingPart.UPPER_RIGHT) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        return face == facing || face == facing.getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos controllerPos = getControllerPos(state, pos);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.getBlock() != GeoResonanceBlocks.SEISMIC_STATION.get()) {
            return false;
        }
        return controllerState.getValue(HorizontalDirectionalBlock.FACING) == state.getValue(FACING);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        if (!state.canSurvive(level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos controllerPos = getControllerPos(state, pos);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof SeismicStationBlock stationBlock)) {
            return InteractionResult.PASS;
        }
        return stationBlock.use(controllerState, level, controllerPos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (!level.isClientSide) {
            BlockPos controllerPos = getControllerPos(state, pos);
            BlockState controllerState = level.getBlockState(controllerPos);
            if (controllerState.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()) {
                level.destroyBlock(controllerPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(GeoResonanceBlocks.SEISMIC_STATION.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(PART));
    }

    @Override
    public Class<SeismicStationBoundingBlockEntity> getBlockEntityClass() {
        return SeismicStationBoundingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeismicStationBoundingBlockEntity> getBlockEntityType() {
        return GeoResonanceBlockEntityTypes.SEISMIC_STATION_BOUNDING.get();
    }

    public static BlockPos getControllerPos(BlockState state, BlockPos partPos) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(PART)) {
            case LOWER_LEFT -> partPos.relative(facing.getClockWise());
            case UPPER_RIGHT -> partPos.below();
            case UPPER_LEFT -> partPos.below().relative(facing.getClockWise());
        };
    }

    public enum BoundingPart implements StringRepresentable {
        LOWER_LEFT,
        UPPER_RIGHT,
        UPPER_LEFT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
