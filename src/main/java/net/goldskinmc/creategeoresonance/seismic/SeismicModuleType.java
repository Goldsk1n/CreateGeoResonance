package net.goldskinmc.creategeoresonance.seismic;

import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public enum SeismicModuleType {
    // Retained for saved items and enum ordinal compatibility.
    BELOW_ZERO("below_zero_module", null),
    NOISE_CANCELLATION("noise_cancellation_module", null),
    AMETHYST("amethyst_module", SeismicAnomalyType.AMETHYST),
    CHEST("chest_module", SeismicAnomalyType.CHEST),
    SPAWNER("spawner_module", SeismicAnomalyType.SPAWNER),
    COAL("coal_resonance_module", SeismicAnomalyType.COAL),
    IRON("iron_resonance_module", SeismicAnomalyType.IRON),
    COPPER("copper_resonance_module", SeismicAnomalyType.COPPER),
    GOLD("gold_resonance_module", SeismicAnomalyType.GOLD),
    REDSTONE("redstone_resonance_module", SeismicAnomalyType.REDSTONE),
    LAPIS("lapis_resonance_module", SeismicAnomalyType.LAPIS),
    EMERALD("emerald_resonance_module", SeismicAnomalyType.EMERALD),
    DIAMOND("diamond_resonance_module", SeismicAnomalyType.DIAMOND),
    ZINC("zinc_resonance_module", SeismicAnomalyType.ZINC),
    CRIMSITE("crimsite_resonance_module", SeismicAnomalyType.CRIMSITE),
    ASURINE("asurine_resonance_module", SeismicAnomalyType.ASURINE),
    OCHRUM("ochrum_resonance_module", SeismicAnomalyType.OCHRUM),
    VERIDIUM("veridium_resonance_module", SeismicAnomalyType.VERIDIUM),
    THORIUM("thorium_resonance_module", SeismicAnomalyType.THORIUM),
    MAGNETITE("magnetite_resonance_module", SeismicAnomalyType.MAGNETITE),
    LEAD("lead_resonance_module", SeismicAnomalyType.LEAD),
    NICKEL("nickel_resonance_module", SeismicAnomalyType.NICKEL),
    LITHIUM("lithium_resonance_module", SeismicAnomalyType.LITHIUM),
    BAUXITE("bauxite_resonance_module", SeismicAnomalyType.BAUXITE),
    GALENA("galena_resonance_module", SeismicAnomalyType.GALENA),
    LIGNITE("lignite_resonance_module", SeismicAnomalyType.LIGNITE),
    SULFUR("sulfur_resonance_module", SeismicAnomalyType.SULFUR),
    CRUDE_OIL("crude_oil_resonance_module", SeismicAnomalyType.CRUDE_OIL),
    WOLFRAMITE("wolframite_resonance_module", SeismicAnomalyType.WOLFRAMITE),
    URANIUM("uranium_resonance_module", SeismicAnomalyType.URANIUM),
    AUTUNITE("autunite_resonance_module", SeismicAnomalyType.AUTUNITE),
    NITRATE("nitrate_resonance_module", SeismicAnomalyType.NITRATE),
    TIN("tin_resonance_module", SeismicAnomalyType.TIN),
    SILVER("silver_resonance_module", SeismicAnomalyType.SILVER),
    PLATINUM("platinum_resonance_module", SeismicAnomalyType.PLATINUM),
    OSMIUM("osmium_resonance_module", SeismicAnomalyType.OSMIUM),
    QUARTZ("quartz_resonance_module", SeismicAnomalyType.QUARTZ),
    COBALT("cobalt_resonance_module", SeismicAnomalyType.COBALT),
    ANCIENT_DEBRIS("ancient_debris_resonance_module", SeismicAnomalyType.ANCIENT_DEBRIS),
    FLUORITE("fluorite_resonance_module", SeismicAnomalyType.FLUORITE),
    CERTUS_QUARTZ("certus_quartz_resonance_module", SeismicAnomalyType.CERTUS_QUARTZ),
    INFERIUM("inferium_resonance_module", SeismicAnomalyType.INFERIUM),
    PROSPERITY("prosperity_resonance_module", SeismicAnomalyType.PROSPERITY),
    SOULIUM("soulium_resonance_module", SeismicAnomalyType.SOULIUM),
    ARCANE_CRYSTAL("arcane_crystal_resonance_module", SeismicAnomalyType.ARCANE_CRYSTAL),
    RUNE("rune_resonance_module", SeismicAnomalyType.RUNE),
    STELLARITE("stellarite_resonance_module", SeismicAnomalyType.STELLARITE),
    URANINITE("uraninite_resonance_module", SeismicAnomalyType.URANINITE),
    DRACONIUM("draconium_resonance_module", SeismicAnomalyType.DRACONIUM),
    BLACK_QUARTZ("black_quartz_resonance_module", SeismicAnomalyType.BLACK_QUARTZ);

    private final String itemName;
    @Nullable
    private final SeismicAnomalyType detectedType;

    SeismicModuleType(String itemName, @Nullable SeismicAnomalyType detectedType) {
        this.itemName = itemName;
        this.detectedType = detectedType;
    }

    public boolean isAvailable() {
        return switch (this) {
            case BELOW_ZERO, NOISE_CANCELLATION -> false;
            case THORIUM -> hasAddonItems("create_new_age", "thorium", "radioactive_thorium")
                || hasAddonItems("createnuclear", "raw_thorium", "thorium_dust");
            case MAGNETITE -> ModList.get().isLoaded("create_new_age");
            case LEAD -> hasAddonItems("tfmg", "raw_lead", "lead_ingot")
                || hasAddonItems("createnuclear", "raw_lead", "lead_ingot")
                || hasAddonItems("mekanism", "raw_lead", "ingot_lead")
                || hasAddonItems("oreganized", "raw_lead", "lead_ingot")
                || hasAddonItems("immersiveengineering", "raw_lead", "ingot_lead");
            case NICKEL -> hasAddonItems("tfmg", "raw_nickel", "nickel_ingot")
                || hasAddonItems("immersiveengineering", "raw_nickel", "ingot_nickel");
            case LITHIUM, GALENA, LIGNITE -> ModList.get().isLoaded("tfmg");
            case SULFUR -> hasAddonItems("tfmg", "sulfur", "sulfur_dust");
            case CRUDE_OIL -> hasAddonItems("tfmg", "crude_oil_bucket")
                || hasAddonItems("pneumaticcraft", "oil_bucket");
            case BAUXITE -> hasAddonItems("tfmg", "bauxite", "aluminum_ingot")
                || hasAddonItems("immersiveengineering", "raw_aluminum", "ingot_aluminum");
            case WOLFRAMITE -> ModList.get().isLoaded("createmetallurgy")
                || SeismicRockStoneDeposits.hasCraftingMaterial("wolframite");
            case URANIUM -> hasAddonItems("createnuclear", "raw_uranium", "uranium_powder")
                || hasAddonItems("mekanism", "raw_uranium", "ingot_uranium")
                || hasAddonItems("immersiveengineering", "raw_uranium", "ingot_uranium");
            case AUTUNITE -> hasAddonItems("createnuclear", "autunite", "raw_uranium");
            case NITRATE -> hasAddonItems("createnuclear", "nitrate", "nitrogen_concentrate");
            case TIN -> hasAddonItems("mekanism", "raw_tin", "ingot_tin");
            case SILVER -> hasAddonItems("oreganized", "raw_silver", "silver_ingot")
                || hasAddonItems("immersiveengineering", "raw_silver", "ingot_silver");
            case PLATINUM -> hasAddonItems("createpropulsion", "raw_platinum", "platinum_ingot")
                || SeismicRockStoneDeposits.hasCraftingMaterial("platinum");
            case OSMIUM -> hasAddonItems("mekanism", "raw_osmium", "ingot_osmium")
                || SeismicRockStoneDeposits.hasCraftingMaterial("osmium");
            case COBALT -> SeismicRockStoneDeposits.hasCraftingMaterial("cobalt");
            case FLUORITE -> hasAddonItems("mekanism", "fluorite_gem", "block_fluorite");
            case CERTUS_QUARTZ -> hasAddonItems("ae2", "certus_quartz_crystal", "quartz_block");
            case INFERIUM -> hasAddonItems("mysticalagriculture", "inferium_essence", "inferium_block");
            case PROSPERITY -> hasAddonItems("mysticalagriculture", "prosperity_shard", "prosperity_block");
            case SOULIUM -> hasAddonItems("mysticalagriculture", "soulium_dust", "soulium_ingot");
            case ARCANE_CRYSTAL -> hasAddonItems("forbidden_arcanus", "arcane_crystal", "arcane_crystal_block");
            case RUNE -> hasAddonItems("forbidden_arcanus", "rune", "rune_block");
            case STELLARITE -> hasAddonItems("forbidden_arcanus", "stellarite_piece", "arcane_crystal");
            case URANINITE -> hasAddonItems("powah", "uraninite_raw", "uraninite");
            case DRACONIUM -> hasAddonItems("draconicevolution", "draconium_dust", "draconium_ingot");
            case BLACK_QUARTZ -> hasAddonItems("actuallyadditions", "black_quartz", "black_quartz_block");
            default -> true;
        };
    }

    private static boolean hasAddonItems(String modId, String... paths) {
        if (!ModList.get().isLoaded(modId)) {
            return false;
        }
        for (String path : paths) {
            if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(modId, path))) {
                return false;
            }
        }
        return true;
    }

    public String itemName() {
        return itemName;
    }

    public boolean isRetired() {
        return this == BELOW_ZERO || this == NOISE_CANCELLATION;
    }

    @Nullable
    public SeismicAnomalyType detectedType() {
        return detectedType;
    }
}
