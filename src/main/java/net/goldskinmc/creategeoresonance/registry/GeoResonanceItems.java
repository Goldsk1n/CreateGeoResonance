package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.seismic.SeismicHammerItem;

public final class GeoResonanceItems {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final ItemEntry<SeismicHammerItem> SEISMIC_HAMMER = REGISTRATE
        .item("seismic_hammer", SeismicHammerItem::new)
        .properties(properties -> properties.stacksTo(1))
        .model((context, provider) -> provider.withExistingParent(context.getName(), provider.mcLoc("item/handheld"))
            .texture("layer0", provider.modLoc("item/" + context.getName())))
        .register();

    private GeoResonanceItems() {
    }

    public static void register() {
    }
}
