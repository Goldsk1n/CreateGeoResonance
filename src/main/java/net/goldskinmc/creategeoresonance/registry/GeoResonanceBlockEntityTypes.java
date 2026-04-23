package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.client.render.PlacedSeismicHammerRenderer;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;

public final class GeoResonanceBlockEntityTypes {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final BlockEntityEntry<SeismicStationBlockEntity> SEISMIC_STATION = REGISTRATE
        .blockEntity("seismic_station", SeismicStationBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.SEISMIC_STATION)
        .register();

    public static final BlockEntityEntry<PlacedSeismicHammerBlockEntity> PLACED_SEISMIC_HAMMER = REGISTRATE
        .blockEntity("placed_seismic_hammer", PlacedSeismicHammerBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.PLACED_SEISMIC_HAMMER)
        .renderer(() -> PlacedSeismicHammerRenderer::new)
        .register();

    private GeoResonanceBlockEntityTypes() {
    }

    public static void register() {
    }
}
