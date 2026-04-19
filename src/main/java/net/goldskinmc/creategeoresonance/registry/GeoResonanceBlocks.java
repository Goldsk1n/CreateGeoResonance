package net.goldskinmc.creategeoresonance.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.goldskinmc.creategeoresonance.GeoResonanceRegistrate;
import net.goldskinmc.creategeoresonance.seismic.PlacedSeismicHammerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

public final class GeoResonanceBlocks {
    private static final Registrate REGISTRATE = GeoResonanceRegistrate.registrate();

    public static final BlockEntry<PlacedSeismicHammerBlock> PLACED_SEISMIC_HAMMER = REGISTRATE
        .block("placed_seismic_hammer", PlacedSeismicHammerBlock::new)
        .initialProperties(() -> Blocks.ANDESITE)
        .properties(properties -> properties
            .strength(1.5F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion())
        .register();

    private GeoResonanceBlocks() {
    }

    public static void register() {
    }
}
