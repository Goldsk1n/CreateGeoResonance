package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.seismic.SeismicAnomalyType;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;
import net.goldskinmc.creategeoresonance.seismic.SeismicModuleItem;

public final class GeoResonanceItems {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final ItemEntry<SeismicHammerItem> SEISMIC_HAMMER = REGISTRATE
        .item("seismic_hammer", SeismicHammerItem::new)
        .properties(properties -> properties.stacksTo(1))
        .register();

    public static final ItemEntry<SeismicModuleItem> COAL_RESONANCE_MODULE = module("coal_resonance_module", SeismicAnomalyType.COAL);
    public static final ItemEntry<SeismicModuleItem> IRON_RESONANCE_MODULE = module("iron_resonance_module", SeismicAnomalyType.IRON);
    public static final ItemEntry<SeismicModuleItem> COPPER_RESONANCE_MODULE = module("copper_resonance_module", SeismicAnomalyType.COPPER);
    public static final ItemEntry<SeismicModuleItem> GOLD_RESONANCE_MODULE = module("gold_resonance_module", SeismicAnomalyType.GOLD);
    public static final ItemEntry<SeismicModuleItem> REDSTONE_RESONANCE_MODULE = module("redstone_resonance_module", SeismicAnomalyType.REDSTONE);
    public static final ItemEntry<SeismicModuleItem> LAPIS_RESONANCE_MODULE = module("lapis_resonance_module", SeismicAnomalyType.LAPIS);
    public static final ItemEntry<SeismicModuleItem> EMERALD_RESONANCE_MODULE = module("emerald_resonance_module", SeismicAnomalyType.EMERALD);
    public static final ItemEntry<SeismicModuleItem> DIAMOND_RESONANCE_MODULE = module("diamond_resonance_module", SeismicAnomalyType.DIAMOND);

    private GeoResonanceItems() {
    }

    public static void register() {
    }

    private static ItemEntry<SeismicModuleItem> module(String name, SeismicAnomalyType type) {
        return REGISTRATE
            .item(name, properties -> new SeismicModuleItem(properties, type))
            .properties(properties -> properties.stacksTo(1))
            .register();
    }
}
