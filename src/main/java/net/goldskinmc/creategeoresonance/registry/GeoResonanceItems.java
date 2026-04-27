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

    public static final ItemEntry<SeismicModuleItem> IRON_RESONANCE_MODULE = REGISTRATE
        .item("iron_resonance_module", properties -> new SeismicModuleItem(properties, SeismicAnomalyType.IRON))
        .properties(properties -> properties.stacksTo(1))
        .register();

    public static final ItemEntry<SeismicModuleItem> COPPER_RESONANCE_MODULE = REGISTRATE
        .item("copper_resonance_module", properties -> new SeismicModuleItem(properties, SeismicAnomalyType.COPPER))
        .properties(properties -> properties.stacksTo(1))
        .register();

    private GeoResonanceItems() {
    }

    public static void register() {
    }
}
