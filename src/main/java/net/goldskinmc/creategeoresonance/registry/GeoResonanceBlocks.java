package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicProjectorBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBoundingBlock;
import net.goldskinmc.creategeoresonance.seismic.SeismicStationBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

public final class GeoResonanceBlocks {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final BlockEntry<SeismicStationBlock> SEISMIC_STATION = REGISTRATE
        .block("seismic_station", SeismicStationBlock::new)
        .initialProperties(() -> Blocks.ANDESITE)
        .properties(properties -> properties
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion())
        .item()
        .build()
        .register();

    public static final BlockEntry<SeismicStationBoundingBlock> SEISMIC_STATION_BOUNDING = REGISTRATE
        .block("seismic_station_bounding", SeismicStationBoundingBlock::new)
        .initialProperties(() -> Blocks.ANDESITE)
        .properties(properties -> properties
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion())
        .register();

    public static final BlockEntry<PlacedSeismicHammerBlock> PLACED_SEISMIC_HAMMER = REGISTRATE
        .block("placed_seismic_hammer", PlacedSeismicHammerBlock::new)
        .initialProperties(() -> Blocks.ANDESITE)
        .properties(properties -> properties
            .strength(1.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion())
        .register();

    public static final BlockEntry<SeismicProjectorBlock> SEISMIC_PROJECTOR = REGISTRATE
        .block("seismic_projector", SeismicProjectorBlock::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(properties -> properties
            .strength(3.5F)
            .sound(SoundType.STONE)
            .lightLevel(state -> state.getValue(SeismicProjectorBlock.ACTIVE) ? 15 : 0)
            .requiresCorrectToolForDrops())
        .item()
        .build()
        .register();

    private GeoResonanceBlocks() {
    }

    public static void register() {
    }
}
