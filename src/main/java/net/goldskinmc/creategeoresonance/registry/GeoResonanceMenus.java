package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.MenuEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.client.screen.SeismicStationScreen;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationMenu;

public final class GeoResonanceMenus {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final MenuEntry<SeismicStationMenu> SEISMIC_STATION = REGISTRATE
        .menu("seismic_station", SeismicStationMenu::create, () -> SeismicStationScreen::new)
        .register();

    private GeoResonanceMenus() {
    }

    public static void register() {
    }
}
