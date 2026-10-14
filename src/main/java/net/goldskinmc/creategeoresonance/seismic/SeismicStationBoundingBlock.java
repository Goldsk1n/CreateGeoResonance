package net.goldskinmc.creategeoresonance.seismic;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.AllBlocks;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlockEntityTypes;
import net.goldskinmc.creategeoresonance.registry.GeoResonanceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SeismicStationBoundingBlock extends HorizontalKineticBlock implements IBE<SeismicStationBoundingBlockEntity>, IWrenchable {
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
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SeismicStationShapes.bounding(state.getValue(PART), state.getValue(FACING));
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (state.getValue(PART) != BoundingPart.UPPER_RIGHT) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        return face == facing.getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos controllerPos = getControllerPos(state, pos);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()) {
            return controllerState.getDestroyProgress(player, level, controllerPos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos controllerPos = getControllerPos(state, pos);
        BlockState controllerState = level.getBlockState(controllerPos);

        if (WrenchInteractionHelper.isWrench(stack)) {
            if (player.isShiftKeyDown()) {
                return WrenchInteractionHelper.pickupBlock(level, controllerPos, controllerState, player, stack).consumesAction()
                    ? ItemInteractionResult.CONSUME
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (state.getValue(PART) == BoundingPart.UPPER_LEFT && player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(controllerPos) instanceof SeismicStationBlockEntity station) {
                station.tryExtractModule(player, hand);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        SeismicModuleType moduleType = SeismicModuleItem.getModuleType(stack);
        if (moduleType != null) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(controllerPos) instanceof SeismicStationBlockEntity station) {
                station.tryInsertModule(player, hand);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (state.getValue(PART) == BoundingPart.UPPER_RIGHT) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(controllerPos) instanceof SeismicStationBlockEntity station) {
                station.tryStartScan(serverPlayer);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (state.getValue(PART) == BoundingPart.LOWER_LEFT) {
            boolean isIoInteraction = stack.is(Items.PAPER) || stack.is(Items.INK_SAC) || stack.isEmpty();
            if (isIoInteraction) {
                if (level.isClientSide) {
                    return ItemInteractionResult.SUCCESS;
                }
                if (level.getBlockEntity(controllerPos) instanceof SeismicStationBlockEntity station) {
                    if (stack.is(Items.PAPER)) {
                        station.tryInsertPaper(player, hand);
                    } else if (stack.is(Items.INK_SAC)) {
                        station.tryInsertInk(player, hand);
                    } else {
                        station.tryTakeOutputWithBareHand(player, hand);
                    }
                    return ItemInteractionResult.CONSUME;
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }

        if (state.getValue(PART) == BoundingPart.UPPER_LEFT) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(controllerState.getBlock() instanceof SeismicStationBlock stationBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return stationBlock.useItemOn(stack, controllerState, level, controllerPos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (!level.isClientSide && !isMoving) {
            BlockPos controllerPos = getControllerPos(state, pos);
            BlockState controllerState = level.getBlockState(controllerPos);
            if (controllerState.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()) {
                // Bounding parts are proxies. Non-player removals should tear down the controller without forcing drops.
                level.destroyBlock(controllerPos, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos controllerPos = getControllerPos(state, pos);
            BlockState controllerState = level.getBlockState(controllerPos);
            if (controllerState.getBlock() == GeoResonanceBlocks.SEISMIC_STATION.get()) {
                // Match controller breaking semantics: one drop in survival, no drop in creative.
                level.destroyBlock(controllerPos, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos controllerPos = getControllerPos(state, context.getClickedPos());
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.getBlock() != GeoResonanceBlocks.SEISMIC_STATION.get()
            || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return WrenchInteractionHelper.pickupBlock(level, controllerPos, controllerState, context.getPlayer(), context.getItemInHand());
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        super.spawnDestroyParticles(level, player, pos, AllBlocks.RAILWAY_CASING.getDefaultState());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
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
