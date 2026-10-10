package net.goldskinmc.creategeoresonance.seismic;

import net.goldskinmc.creategeoresonance.CreateGeoResonanceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class SeismicMineralTags {
    public static final TagKey<Block> CRIMSITE = deposit("crimsite");
    public static final TagKey<Block> ASURINE = deposit("asurine");
    public static final TagKey<Block> OCHRUM = deposit("ochrum");
    public static final TagKey<Block> VERIDIUM = deposit("veridium");

    private SeismicMineralTags() {
    }

    private static TagKey<Block> deposit(String mineral) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(
            CreateGeoResonanceMod.MODID, "deposits/" + mineral));
    }
}
