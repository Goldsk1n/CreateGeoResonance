package net.goldskinmc.creategeoresonance.seismic;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class SeismicStationShapes {
    private static final VoxelShape SIDES = Shapes.or(
        Block.box(0, 0, 0, 2, 16, 16),
        Block.box(14, 0, 0, 16, 16, 16)
    );
    private static final VoxelShape TOP = Block.box(0, 14, 0, 16, 16, 16);

    // Hollow collision shapes also keep recessed faces from sampling light inside adjacent blocks.
    // Reserve the moving parts' swept volumes rather than changing collision during animations.
    private static final VoxelShaper LOWER_RIGHT = VoxelShaper.forHorizontal(Shapes.or(
        SIDES,
        Block.box(2, 0, 2, 14, 16, 14)
    ), Direction.NORTH);
    private static final VoxelShaper LOWER_LEFT = VoxelShaper.forHorizontal(Shapes.or(
        SIDES,
        Block.box(2, 0, 0, 14, 4, 16),
        Block.box(2, 8, 5, 14, 14, 11),
        Block.box(7, 13, 7, 9, 16, 9)
    ), Direction.NORTH);
    private static final VoxelShaper UPPER_RIGHT = VoxelShaper.forHorizontal(Shapes.or(
        SIDES, TOP,
        Block.box(2, 4, 1, 14, 14, 15),
        Block.box(5, 0, 5, 11, 4, 11)
    ), Direction.NORTH);
    private static final VoxelShaper UPPER_LEFT = VoxelShaper.forHorizontal(Shapes.or(
        SIDES, TOP,
        Block.box(2, 7, 1, 14, 14, 2.01),
        Block.box(4, 0, 4, 12, 7, 12),
        Block.box(7, 6, 7, 9, 14, 9)
    ), Direction.NORTH);

    private SeismicStationShapes() {
    }

    static VoxelShape controller(Direction facing) {
        return LOWER_RIGHT.get(facing);
    }

    static VoxelShape bounding(SeismicStationBoundingBlock.BoundingPart part, Direction facing) {
        return switch (part) {
            case LOWER_LEFT -> LOWER_LEFT.get(facing);
            case UPPER_RIGHT -> UPPER_RIGHT.get(facing);
            case UPPER_LEFT -> UPPER_LEFT.get(facing);
        };
    }
}
