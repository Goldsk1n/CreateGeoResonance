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
