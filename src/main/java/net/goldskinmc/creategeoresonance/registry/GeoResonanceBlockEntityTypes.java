package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.client.render.PlacedSeismicHammerRenderer;
import net.goldskinmc.creategeoresonance.client.render.SeismicProjectorRenderer;
import net.goldskinmc.creategeoresonance.client.render.SeismicStationRenderer;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlockEntity;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlockEntity;

public final class GeoResonanceBlockEntityTypes {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final BlockEntityEntry<SeismicStationBlockEntity> SEISMIC_STATION = REGISTRATE
        .blockEntity("seismic_station", SeismicStationBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.SEISMIC_STATION)
        .renderer(() -> SeismicStationRenderer::new)
        .register();

    public static final BlockEntityEntry<SeismicStationBoundingBlockEntity> SEISMIC_STATION_BOUNDING = REGISTRATE
        .blockEntity("seismic_station_bounding", SeismicStationBoundingBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.SEISMIC_STATION_BOUNDING)
        .register();

    public static final BlockEntityEntry<PlacedSeismicHammerBlockEntity> PLACED_SEISMIC_HAMMER = REGISTRATE
        .blockEntity("placed_seismic_hammer", PlacedSeismicHammerBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.PLACED_SEISMIC_HAMMER)
        .renderer(() -> PlacedSeismicHammerRenderer::new)
        .register();

    public static final BlockEntityEntry<SeismicProjectorBlockEntity> SEISMIC_PROJECTOR = REGISTRATE
        .blockEntity("seismic_projector", SeismicProjectorBlockEntity::new)
        .validBlocks(GeoResonanceBlocks.SEISMIC_PROJECTOR)
        .renderer(() -> SeismicProjectorRenderer::new)
        .register();

    private GeoResonanceBlockEntityTypes() {
    }

    public static void register() {
    }
}
