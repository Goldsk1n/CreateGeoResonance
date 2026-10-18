package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeismicProjectorRenderBoundsTest {
    private static final BlockPos PROJECTOR = new BlockPos(0, 90, 0);
    private static final BlockPos DEPOSIT = new BlockPos(10, -50, 0);
    private static final int VIEW_DISTANCE = 128;

    @Test
    void cameraNearDeepDepositRemainsInRangeDespiteDistanceToProjector() {
        SeismicProjectorData data = surveyAt(DEPOSIT);
        Vec3 camera = Vec3.atCenterOf(DEPOSIT);

        assertTrue(Vec3.atCenterOf(PROJECTOR).distanceToSqr(camera) > VIEW_DISTANCE * VIEW_DISTANCE);
        assertTrue(data.isWithinProjectionRenderDistance(PROJECTOR, true, camera, VIEW_DISTANCE));
        assertFalse(data.isWithinProjectionRenderDistance(PROJECTOR, false, camera, VIEW_DISTANCE));
    }

    @Test
    void boundingBoxIncludesDepositsAndTheBeamBackToTheProjector() {
        AABB bounds = surveyAt(DEPOSIT).getRenderBoundingBox(PROJECTOR, true);

        assertTrue(bounds.contains(new Vec3(0.5D, 90.05D, 0.5D)));
        assertTrue(bounds.contains(Vec3.atCenterOf(DEPOSIT)));
        assertTrue(bounds.contains(new Vec3(5.5D, 20.0D, 0.5D)));
    }

    @Test
    void distanceBeyondProjectionBoundsRemainsFiniteAndThreeDimensional() {
        SeismicProjectorData data = surveyAt(DEPOSIT);

        assertTrue(data.isWithinProjectionRenderDistance(PROJECTOR, true,
            new Vec3(138.5D, -49.5D, 0.5D), VIEW_DISTANCE));
        assertFalse(data.isWithinProjectionRenderDistance(PROJECTOR, true,
            new Vec3(139.0D, -49.5D, 0.5D), VIEW_DISTANCE));
        assertFalse(data.isWithinProjectionRenderDistance(PROJECTOR, true,
            new Vec3(111.0D, -150.0D, 0.5D), VIEW_DISTANCE));
    }

    @Test
    void inactiveBoundsStayLocalWithoutDiscardingTheActiveCache() {
        SeismicProjectorData data = surveyAt(DEPOSIT);
        AABB active = data.getRenderBoundingBox(PROJECTOR, true);

        assertEquals(new AABB(PROJECTOR).inflate(1.0D), data.getRenderBoundingBox(PROJECTOR, false));
        assertSame(active, data.getRenderBoundingBox(PROJECTOR, true));
    }

    @Test
    void repeatedChecksReuseBoundsAndNewSurveyDataInvalidatesThem() {
        SeismicProjectorData data = surveyAt(DEPOSIT);
        AABB original = data.getRenderBoundingBox(PROJECTOR, true);
        assertSame(original, data.getRenderBoundingBox(PROJECTOR, true));

        data.readFromTag(surveyTag(new BlockPos(20, -60, 0)));
        AABB replacement = data.getRenderBoundingBox(PROJECTOR, true);

        assertNotSame(original, replacement);
        assertEquals(-60.0D, replacement.minY);
        assertEquals(21.0D, replacement.maxX);
    }

    @Test
    void changingProjectorPositionRecomputesBeamBoundsWithoutMovingTheDeposit() {
        SeismicProjectorData data = surveyAt(DEPOSIT);
        AABB original = data.getRenderBoundingBox(PROJECTOR, true);
        AABB moved = data.getRenderBoundingBox(new BlockPos(-20, 100, 0), true);

        assertNotSame(original, moved);
        assertEquals(-20.0D, moved.minX);
        assertEquals(101.0D, moved.maxY);
        assertTrue(moved.contains(Vec3.atCenterOf(DEPOSIT)));
    }

    @Test
    void clearingSurveyDataRemovesTheOldUndergroundExtent() {
        SeismicProjectorData data = surveyAt(DEPOSIT);
        AABB original = data.getRenderBoundingBox(PROJECTOR, true);

        data.readFromTag(new CompoundTag());
        AABB cleared = data.getRenderBoundingBox(PROJECTOR, true);

        assertNotSame(original, cleared);
        assertEquals(new AABB(PROJECTOR).inflate(1.0D), cleared);
        assertFalse(data.isWithinProjectionRenderDistance(PROJECTOR, true,
            Vec3.atCenterOf(DEPOSIT), VIEW_DISTANCE));
    }

    private static SeismicProjectorData surveyAt(BlockPos deposit) {
        SeismicProjectorData data = new SeismicProjectorData();
        data.readFromTag(surveyTag(deposit));
        return data;
    }

    private static CompoundTag surveyTag(BlockPos deposit) {
        ListTag nodes = new ListTag();
        for (int stationX : new int[] {0, 8}) {
            CompoundTag node = new CompoundTag();
            node.putInt("CenterX", stationX);
            node.putInt("CenterY", 90);
            node.putInt("CenterZ", 0);
            node.putInt("StationX", stationX);
            node.putInt("StationY", 90);
            node.putInt("StationZ", 0);
            node.putString("StationDimension", "minecraft:overworld");

            CompoundTag block = new CompoundTag();
            block.putInt("BlockX", deposit.getX());
            block.putInt("BlockY", deposit.getY());
            block.putInt("BlockZ", deposit.getZ());
            ListTag blocks = new ListTag();
            blocks.add(block);
            CompoundTag cluster = new CompoundTag();
            cluster.putString("ClusterType", "DIAMOND");
            cluster.put("Blocks", blocks);
            ListTag clusters = new ListTag();
            clusters.add(cluster);
            node.put("ExactClusters", clusters);
            nodes.add(node);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Nodes", nodes);
        return tag;
    }
}
