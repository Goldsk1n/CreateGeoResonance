package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleType;

public final class GeoResonanceItems {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final ItemEntry<SeismicHammerItem> SEISMIC_HAMMER = REGISTRATE
        .item("seismic_hammer", SeismicHammerItem::new)
        .properties(properties -> properties.stacksTo(1))
        .register();

    public static final ItemEntry<SeismicModuleItem> BELOW_ZERO_MODULE = module(SeismicModuleType.BELOW_ZERO);
    public static final ItemEntry<SeismicModuleItem> NOISE_CANCELLATION_MODULE = module(SeismicModuleType.NOISE_CANCELLATION);
    public static final ItemEntry<SeismicModuleItem> AMETHYST_MODULE = module(SeismicModuleType.AMETHYST);
    public static final ItemEntry<SeismicModuleItem> CHEST_MODULE = module(SeismicModuleType.CHEST);
    public static final ItemEntry<SeismicModuleItem> SPAWNER_MODULE = module(SeismicModuleType.SPAWNER);
    public static final ItemEntry<SeismicModuleItem> COAL_RESONANCE_MODULE = module(SeismicModuleType.COAL);
    public static final ItemEntry<SeismicModuleItem> IRON_RESONANCE_MODULE = module(SeismicModuleType.IRON);
    public static final ItemEntry<SeismicModuleItem> COPPER_RESONANCE_MODULE = module(SeismicModuleType.COPPER);
    public static final ItemEntry<SeismicModuleItem> GOLD_RESONANCE_MODULE = module(SeismicModuleType.GOLD);
    public static final ItemEntry<SeismicModuleItem> REDSTONE_RESONANCE_MODULE = module(SeismicModuleType.REDSTONE);
    public static final ItemEntry<SeismicModuleItem> LAPIS_RESONANCE_MODULE = module(SeismicModuleType.LAPIS);
    public static final ItemEntry<SeismicModuleItem> EMERALD_RESONANCE_MODULE = module(SeismicModuleType.EMERALD);
    public static final ItemEntry<SeismicModuleItem> DIAMOND_RESONANCE_MODULE = module(SeismicModuleType.DIAMOND);
    public static final ItemEntry<SeismicModuleItem> ZINC_RESONANCE_MODULE = module(SeismicModuleType.ZINC);
    public static final ItemEntry<SeismicModuleItem> CRIMSITE_RESONANCE_MODULE = module(SeismicModuleType.CRIMSITE);
    public static final ItemEntry<SeismicModuleItem> ASURINE_RESONANCE_MODULE = module(SeismicModuleType.ASURINE);
    public static final ItemEntry<SeismicModuleItem> OCHRUM_RESONANCE_MODULE = module(SeismicModuleType.OCHRUM);
    public static final ItemEntry<SeismicModuleItem> VERIDIUM_RESONANCE_MODULE = module(SeismicModuleType.VERIDIUM);
    public static final ItemEntry<SeismicModuleItem> THORIUM_RESONANCE_MODULE = module(SeismicModuleType.THORIUM);
    public static final ItemEntry<SeismicModuleItem> MAGNETITE_RESONANCE_MODULE = module(SeismicModuleType.MAGNETITE);
    public static final ItemEntry<SeismicModuleItem> LEAD_RESONANCE_MODULE = module(SeismicModuleType.LEAD);
    public static final ItemEntry<SeismicModuleItem> NICKEL_RESONANCE_MODULE = module(SeismicModuleType.NICKEL);
    public static final ItemEntry<SeismicModuleItem> LITHIUM_RESONANCE_MODULE = module(SeismicModuleType.LITHIUM);
    public static final ItemEntry<SeismicModuleItem> BAUXITE_RESONANCE_MODULE = module(SeismicModuleType.BAUXITE);
    public static final ItemEntry<SeismicModuleItem> GALENA_RESONANCE_MODULE = module(SeismicModuleType.GALENA);
    public static final ItemEntry<SeismicModuleItem> LIGNITE_RESONANCE_MODULE = module(SeismicModuleType.LIGNITE);
    public static final ItemEntry<SeismicModuleItem> SULFUR_RESONANCE_MODULE = module(SeismicModuleType.SULFUR);
    public static final ItemEntry<SeismicModuleItem> CRUDE_OIL_RESONANCE_MODULE = module(SeismicModuleType.CRUDE_OIL);
    public static final ItemEntry<SeismicModuleItem> WOLFRAMITE_RESONANCE_MODULE = module(SeismicModuleType.WOLFRAMITE);
    public static final ItemEntry<SeismicModuleItem> URANIUM_RESONANCE_MODULE = module(SeismicModuleType.URANIUM);
    public static final ItemEntry<SeismicModuleItem> AUTUNITE_RESONANCE_MODULE = module(SeismicModuleType.AUTUNITE);
    public static final ItemEntry<SeismicModuleItem> NITRATE_RESONANCE_MODULE = module(SeismicModuleType.NITRATE);
    public static final ItemEntry<SeismicModuleItem> TIN_RESONANCE_MODULE = module(SeismicModuleType.TIN);
    public static final ItemEntry<SeismicModuleItem> SILVER_RESONANCE_MODULE = module(SeismicModuleType.SILVER);
    public static final ItemEntry<SeismicModuleItem> PLATINUM_RESONANCE_MODULE = module(SeismicModuleType.PLATINUM);
    public static final ItemEntry<SeismicModuleItem> OSMIUM_RESONANCE_MODULE = module(SeismicModuleType.OSMIUM);
    public static final ItemEntry<SeismicModuleItem> QUARTZ_RESONANCE_MODULE = module(SeismicModuleType.QUARTZ);
    public static final ItemEntry<SeismicModuleItem> ANCIENT_DEBRIS_RESONANCE_MODULE = module(SeismicModuleType.ANCIENT_DEBRIS);
    public static final ItemEntry<SeismicModuleItem> FLUORITE_RESONANCE_MODULE = module(SeismicModuleType.FLUORITE);
    public static final ItemEntry<SeismicModuleItem> CERTUS_QUARTZ_RESONANCE_MODULE = module(SeismicModuleType.CERTUS_QUARTZ);
    public static final ItemEntry<SeismicModuleItem> INFERIUM_RESONANCE_MODULE = module(SeismicModuleType.INFERIUM);
    public static final ItemEntry<SeismicModuleItem> PROSPERITY_RESONANCE_MODULE = module(SeismicModuleType.PROSPERITY);
    public static final ItemEntry<SeismicModuleItem> SOULIUM_RESONANCE_MODULE = module(SeismicModuleType.SOULIUM);
    public static final ItemEntry<SeismicModuleItem> ARCANE_CRYSTAL_RESONANCE_MODULE = module(SeismicModuleType.ARCANE_CRYSTAL);
    public static final ItemEntry<SeismicModuleItem> RUNE_RESONANCE_MODULE = module(SeismicModuleType.RUNE);
    public static final ItemEntry<SeismicModuleItem> STELLARITE_RESONANCE_MODULE = module(SeismicModuleType.STELLARITE);
    public static final ItemEntry<SeismicModuleItem> URANINITE_RESONANCE_MODULE = module(SeismicModuleType.URANINITE);
    public static final ItemEntry<SeismicModuleItem> DRACONIUM_RESONANCE_MODULE = module(SeismicModuleType.DRACONIUM);
    public static final ItemEntry<SeismicModuleItem> BLACK_QUARTZ_RESONANCE_MODULE = module(SeismicModuleType.BLACK_QUARTZ);
    public static final ItemEntry<SeismicModuleItem> COBALT_RESONANCE_MODULE = module(SeismicModuleType.COBALT);

    private GeoResonanceItems() {
    }

    public static void register() {
    }

    private static ItemEntry<SeismicModuleItem> module(SeismicModuleType moduleType) {
        return REGISTRATE
            .item(moduleType.itemName(), properties -> new SeismicModuleItem(properties, moduleType))
            .properties(properties -> properties.stacksTo(1))
            .register();
    }
}
