package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return canAssembleAt(context.getLevel(), context.getClickedPos(), facing) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        BlockPos leftLower = getLeftPos(pos, facing);
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            && level.getBlockState(leftLower.below()).isFaceSturdy(level, leftLower.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        if (!state.canSurvive(level, currentPos) || !hasAllBoundingParts(level, currentPos, state.getValue(HORIZONTAL_FACING))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        Direction facing = state.getValue(HORIZONTAL_FACING);
        BlockPos left = getLeftPos(pos, facing);
        BlockState boundingBase = GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.getDefaultState()
            .setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlock(left, boundingBase.setValue(SeismicStationBoundingBlock.PART, SeismicStationBoundingBlock.BoundingPart.LOWER_LEFT), Block.UPDATE_ALL);
        level.setBlock(pos.above(), boundingBase.setValue(SeismicStationBoundingBlock.PART, SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT), Block.UPDATE_ALL);
        level.setBlock(left.above(), boundingBase.setValue(SeismicStationBoundingBlock.PART, SeismicStationBoundingBlock.BoundingPart.UPPER_LEFT), Block.UPDATE_ALL);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, SeismicStationBlockEntity::dropInventory);
            removeBoundingParts(level, pos, state.getValue(HORIZONTAL_FACING));
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return super.getDrops(state, params);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public Class<SeismicStationBlockEntity> getBlockEntityClass() {
        return SeismicStationBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeismicStationBlockEntity> getBlockEntityType() {
        return GeoResonanceBlockEntityTypes.SEISMIC_STATION.get();
    }

    public static BlockPos getLeftPos(BlockPos controllerPos, Direction facing) {
        return controllerPos.relative(facing.getCounterClockWise());
    }

    public static BlockPos getUpperRightPos(BlockPos controllerPos) {
        return controllerPos.above();
    }

    public static BlockPos getUpperLeftPos(BlockPos controllerPos, Direction facing) {
        return getLeftPos(controllerPos, facing).above();
    }

    private static void removeBoundingParts(Level level, BlockPos controllerPos, Direction facing) {
        removeBoundingPart(level, getLeftPos(controllerPos, facing));
        removeBoundingPart(level, getUpperRightPos(controllerPos));
        removeBoundingPart(level, getUpperLeftPos(controllerPos, facing));
    }

    private static void removeBoundingPart(Level level, BlockPos partPos) {
        BlockState state = level.getBlockState(partPos);
        if (state.getBlock() == GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get()) {
            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    private static boolean isExpectedBounding(LevelReader level, BlockPos pos, Direction facing, SeismicStationBoundingBlock.BoundingPart part) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != GeoResonanceBlocks.SEISMIC_STATION_BOUNDING.get()) {
            return false;
        }
        return state.getValue(HorizontalDirectionalBlock.FACING) == facing
            && state.getValue(SeismicStationBoundingBlock.PART) == part;
    }

    private static boolean hasAllBoundingParts(LevelReader level, BlockPos controllerPos, Direction facing) {
        return isExpectedBounding(level, getLeftPos(controllerPos, facing), facing, SeismicStationBoundingBlock.BoundingPart.LOWER_LEFT)
            && isExpectedBounding(level, getUpperRightPos(controllerPos), facing, SeismicStationBoundingBlock.BoundingPart.UPPER_RIGHT)
            && isExpectedBounding(level, getUpperLeftPos(controllerPos, facing), facing, SeismicStationBoundingBlock.BoundingPart.UPPER_LEFT);
    }

    private static boolean canAssembleAt(LevelReader level, BlockPos controllerPos, Direction facing) {
        BlockPos left = getLeftPos(controllerPos, facing);
        BlockPos upperRight = getUpperRightPos(controllerPos);
        BlockPos upperLeft = getUpperLeftPos(controllerPos, facing);
        return level.getBlockState(controllerPos).canBeReplaced()
            && level.getBlockState(left).canBeReplaced()
            && level.getBlockState(upperRight).canBeReplaced()
            && level.getBlockState(upperLeft).canBeReplaced()
            && level.getBlockState(controllerPos.below()).isFaceSturdy(level, controllerPos.below(), Direction.UP)
            && level.getBlockState(left.below()).isFaceSturdy(level, left.below(), Direction.UP);
    }
}
