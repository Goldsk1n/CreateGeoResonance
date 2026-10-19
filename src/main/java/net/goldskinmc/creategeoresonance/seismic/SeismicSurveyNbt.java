package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class SeismicSurveyNbt {
    private static final String BLOCKS = "Blocks";

    private SeismicSurveyNbt() {
    }

    public static void writeBlocks(CompoundTag cluster, List<BlockPos> blocks) {
        long[] packed = new long[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            packed[i] = blocks.get(i).asLong();
        }
        cluster.putLongArray(BLOCKS, packed);
    }

    public static List<BlockPos> readBlocks(CompoundTag cluster, String xKey, String yKey, String zKey) {
        if (cluster.contains(BLOCKS, Tag.TAG_LONG_ARRAY)) {
            long[] packed = cluster.getLongArray(BLOCKS);
            List<BlockPos> blocks = new ArrayList<>(packed.length);
            for (long position : packed) {
                blocks.add(BlockPos.of(position));
            }
            return List.copyOf(blocks);
        }

        // Existing saves used one compound per block, with different coordinate keys in projectors.
        ListTag legacy = cluster.getList(BLOCKS, Tag.TAG_COMPOUND);
        List<BlockPos> blocks = new ArrayList<>(legacy.size());
        for (Tag raw : legacy) {
            CompoundTag block = (CompoundTag) raw;
            blocks.add(new BlockPos(block.getInt(xKey), block.getInt(yKey), block.getInt(zKey)));
        }
        return List.copyOf(blocks);
    }

    public static void compactLegacyClusters(ListTag clusters) {
        for (Tag raw : clusters) {
            CompoundTag cluster = (CompoundTag) raw;
            if (cluster.contains(BLOCKS, Tag.TAG_LIST)) {
                writeBlocks(cluster, readBlocks(cluster, "X", "Y", "Z"));
            }
        }
    }
}
