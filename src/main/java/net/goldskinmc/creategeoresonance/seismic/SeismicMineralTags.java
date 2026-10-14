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
    public static final TagKey<Block> THORIUM = deposit("thorium");
    public static final TagKey<Block> MAGNETITE = deposit("magnetite");
    public static final TagKey<Block> LEAD = deposit("lead");
    public static final TagKey<Block> NICKEL = deposit("nickel");
    public static final TagKey<Block> LITHIUM = deposit("lithium");
    public static final TagKey<Block> BAUXITE = deposit("bauxite");
    public static final TagKey<Block> GALENA = deposit("galena");
    public static final TagKey<Block> LIGNITE = deposit("lignite");
    public static final TagKey<Block> SULFUR = deposit("sulfur");
    public static final TagKey<Block> CRUDE_OIL = deposit("crude_oil");
    public static final TagKey<Block> WOLFRAMITE = deposit("wolframite");
    public static final TagKey<Block> URANIUM = deposit("uranium");
    public static final TagKey<Block> AUTUNITE = deposit("autunite");
    public static final TagKey<Block> NITRATE = deposit("nitrate");
    public static final TagKey<Block> TIN = deposit("tin");
    public static final TagKey<Block> OSMIUM = deposit("osmium");
    public static final TagKey<Block> FLUORITE = deposit("fluorite");
    public static final TagKey<Block> CERTUS_QUARTZ = deposit("certus_quartz");
    public static final TagKey<Block> INFERIUM = deposit("inferium");
    public static final TagKey<Block> PROSPERITY = deposit("prosperity");
    public static final TagKey<Block> SOULIUM = deposit("soulium");
    public static final TagKey<Block> ARCANE_CRYSTAL = deposit("arcane_crystal");
    public static final TagKey<Block> RUNE = deposit("rune");
    public static final TagKey<Block> STELLARITE = deposit("stellarite");
    public static final TagKey<Block> URANINITE = deposit("uraninite");
    public static final TagKey<Block> DRACONIUM = deposit("draconium");
    public static final TagKey<Block> XPETRIFIED = deposit("xpetrified");
    public static final TagKey<Block> SILVER = deposit("silver");
    public static final TagKey<Block> APATITE = deposit("apatite");
    public static final TagKey<Block> CINNABAR = deposit("cinnabar");
    public static final TagKey<Block> NITER = deposit("niter");
    public static final TagKey<Block> COBALT = deposit("cobalt");

    private SeismicMineralTags() {
    }

    private static TagKey<Block> deposit(String mineral) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(
            CreateGeoResonanceMod.MODID, "deposits/" + mineral));
    }
}
