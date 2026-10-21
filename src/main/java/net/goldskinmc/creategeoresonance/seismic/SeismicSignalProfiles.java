package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.MapColor;
import java.util.HashMap;
import java.util.Map;

/** Shared identity across echoes, holograms, map pixels and sidebar labels. */
public final class SeismicSignalProfiles {
    public static final float MAX_DEPTH_PITCH_SHIFT = 0.06F;
    private static final Map<SeismicAnomalyType, Profile> BUILT_INS = createProfiles();

    private SeismicSignalProfiles() {
    }

    private static Map<SeismicAnomalyType, Profile> createProfiles() {
        Map<SeismicAnomalyType, Profile> profiles = new HashMap<>();
        profiles.put(SeismicAnomalyType.WATER, new Profile(0x50AAFF, MapColor.WATER,
            SeismicSoundFamily.LIQUID, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.84F));
        profiles.put(SeismicAnomalyType.LAVA, new Profile(0xFF7828, MapColor.COLOR_ORANGE,
            SeismicSoundFamily.LIQUID, SoundEvents.LAVA_POP, 0.66F));
        profiles.put(SeismicAnomalyType.CAVE, new Profile(0xB4C8DC, MapColor.COLOR_LIGHT_GRAY,
            SeismicSoundFamily.STONE, SoundEvents.BELL_BLOCK, 1.12F));
        profiles.put(SeismicAnomalyType.COAL, new Profile(0x8A8A8A, MapColor.COLOR_GRAY,
            SeismicSoundFamily.STONE, SoundEvents.NOTE_BLOCK_BASS.value(), 0.62F));
        profiles.put(SeismicAnomalyType.IRON, new Profile(0xD7D7D7, MapColor.METAL,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.72F));
        profiles.put(SeismicAnomalyType.COPPER, new Profile(0xD3844B, MapColor.COLOR_BROWN,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 1.0F));
        profiles.put(SeismicAnomalyType.GOLD, new Profile(0xFFD73D, MapColor.COLOR_YELLOW,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.22F));
        profiles.put(SeismicAnomalyType.REDSTONE, new Profile(0xE53131, MapColor.COLOR_RED,
            SeismicSoundFamily.STONE, SoundEvents.REDSTONE_TORCH_BURNOUT, 0.95F));
        profiles.put(SeismicAnomalyType.LAPIS, new Profile(0x426CD0, MapColor.COLOR_BLUE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.06F));
        profiles.put(SeismicAnomalyType.EMERALD, new Profile(0x31D271, MapColor.COLOR_GREEN,
            SeismicSoundFamily.CRYSTAL, SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.28F));
        profiles.put(SeismicAnomalyType.DIAMOND, new Profile(0x57EEDC, MapColor.COLOR_LIGHT_BLUE,
            SeismicSoundFamily.CRYSTAL, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.34F));
        profiles.put(SeismicAnomalyType.ZINC, new Profile(0x9CBECD, MapColor.COLOR_GRAY,
            SeismicSoundFamily.METAL, SoundEvents.CHAIN_STEP, 0.9F));
        profiles.put(SeismicAnomalyType.CRIMSITE, new Profile(0xB85C70, MapColor.TERRACOTTA_RED,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 1.0F));
        profiles.put(SeismicAnomalyType.ASURINE, new Profile(0x669FD0, MapColor.COLOR_LIGHT_BLUE,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 1.1F));
        profiles.put(SeismicAnomalyType.OCHRUM, new Profile(0xD9AC55, MapColor.TERRACOTTA_YELLOW,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 0.95F));
        profiles.put(SeismicAnomalyType.VERIDIUM, new Profile(0x79B883, MapColor.TERRACOTTA_GREEN,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 1.05F));
        profiles.put(SeismicAnomalyType.THORIUM, new Profile(0xC6D86B, MapColor.COLOR_LIGHT_GREEN,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 1.16F));
        profiles.put(SeismicAnomalyType.MAGNETITE, new Profile(0x8FA4AD, MapColor.TERRACOTTA_CYAN,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.76F));
        profiles.put(SeismicAnomalyType.LEAD, new Profile(0x848CB8, MapColor.TERRACOTTA_BLUE,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.68F));
        profiles.put(SeismicAnomalyType.NICKEL, new Profile(0xAFC38D, MapColor.COLOR_LIGHT_GREEN,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 0.88F));
        profiles.put(SeismicAnomalyType.LITHIUM, new Profile(0xD896B6, MapColor.COLOR_PINK,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 1.24F));
        profiles.put(SeismicAnomalyType.BAUXITE, new Profile(0xEFA76F, MapColor.TERRACOTTA_ORANGE,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 0.92F));
        profiles.put(SeismicAnomalyType.GALENA, new Profile(0x9E7BB8, MapColor.TERRACOTTA_PURPLE,
            SeismicSoundFamily.STONE, SoundEvents.DEEPSLATE_HIT, 0.74F));
        profiles.put(SeismicAnomalyType.LIGNITE, new Profile(0xB18B55, MapColor.TERRACOTTA_BROWN,
            SeismicSoundFamily.STONE, SoundEvents.NOTE_BLOCK_BASS.value(), 0.58F));
        profiles.put(SeismicAnomalyType.SULFUR, new Profile(0xDFD85A, MapColor.COLOR_YELLOW,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 1.12F));
        profiles.put(SeismicAnomalyType.CRUDE_OIL, new Profile(0xA76C8E, MapColor.TERRACOTTA_MAGENTA,
            SeismicSoundFamily.LIQUID, SeismicSoundFamily.LIQUID.sound(), 0.60F));
        profiles.put(SeismicAnomalyType.WOLFRAMITE, new Profile(0x90A9BF, MapColor.COLOR_LIGHT_GRAY,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.64F));
        profiles.put(SeismicAnomalyType.URANIUM, new Profile(0x64CE67, MapColor.COLOR_GREEN,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 1.02F));
        profiles.put(SeismicAnomalyType.AUTUNITE, new Profile(0xB9E45B, MapColor.COLOR_LIGHT_GREEN,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.10F));
        profiles.put(SeismicAnomalyType.NITRATE, new Profile(0xE6D0A8, MapColor.SAND,
            SeismicSoundFamily.STONE, SeismicSoundFamily.STONE.sound(), 1.26F));
        profiles.put(SeismicAnomalyType.TIN, new Profile(0x86BFA8, MapColor.COLOR_CYAN,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 0.96F));
        profiles.put(SeismicAnomalyType.SILVER, new Profile(0xCADCEC, MapColor.COLOR_LIGHT_GRAY,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.20F));
        profiles.put(SeismicAnomalyType.PLATINUM, new Profile(0xC1BBE8, MapColor.COLOR_PURPLE,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.30F));
        profiles.put(SeismicAnomalyType.OSMIUM, new Profile(0x7592BE, MapColor.COLOR_BLUE,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.82F));
        profiles.put(SeismicAnomalyType.QUARTZ, new Profile(0xF2E7D6, MapColor.QUARTZ,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.36F));
        profiles.put(SeismicAnomalyType.ANCIENT_DEBRIS, new Profile(0xA97F6C, MapColor.TERRACOTTA_BROWN,
            SeismicSoundFamily.METAL, SoundEvents.NETHERITE_BLOCK_HIT, 0.70F));
        profiles.put(SeismicAnomalyType.FLUORITE, new Profile(0xAEB8EF, MapColor.COLOR_LIGHT_BLUE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.30F));
        profiles.put(SeismicAnomalyType.CERTUS_QUARTZ, new Profile(0x9BCDE8, MapColor.COLOR_LIGHT_BLUE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.42F));
        profiles.put(SeismicAnomalyType.INFERIUM, new Profile(0x83B74D, MapColor.COLOR_LIGHT_GREEN,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.05F));
        profiles.put(SeismicAnomalyType.PROSPERITY, new Profile(0xD9E6E9, MapColor.SNOW,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.58F));
        profiles.put(SeismicAnomalyType.SOULIUM, new Profile(0xB48B83, MapColor.TERRACOTTA_PINK,
            SeismicSoundFamily.STONE, SoundEvents.SOUL_SAND_HIT, 0.78F));
        profiles.put(SeismicAnomalyType.ARCANE_CRYSTAL, new Profile(0x6F94F1, MapColor.COLOR_BLUE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.22F));
        profiles.put(SeismicAnomalyType.RUNE, new Profile(0xB480D9, MapColor.COLOR_PURPLE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.06F));
        profiles.put(SeismicAnomalyType.STELLARITE, new Profile(0xF1C45D, MapColor.COLOR_YELLOW,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.66F));
        profiles.put(SeismicAnomalyType.URANINITE, new Profile(0x9DBE55, MapColor.TERRACOTTA_LIGHT_GREEN,
            SeismicSoundFamily.METAL, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), 0.96F));
        profiles.put(SeismicAnomalyType.DRACONIUM, new Profile(0xA57CE6, MapColor.COLOR_PURPLE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 0.78F));
        profiles.put(SeismicAnomalyType.BLACK_QUARTZ, new Profile(0x8B93A6, MapColor.COLOR_GRAY,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 0.88F));
        profiles.put(SeismicAnomalyType.COBALT, new Profile(0x655BE0, MapColor.TERRACOTTA_BLUE,
            SeismicSoundFamily.METAL, SeismicSoundFamily.METAL.sound(), 0.90F));
        profiles.put(SeismicAnomalyType.AMETHYST, new Profile(0xC08AE6, MapColor.COLOR_PURPLE,
            SeismicSoundFamily.CRYSTAL, SeismicSoundFamily.CRYSTAL.sound(), 1.18F));
        profiles.put(SeismicAnomalyType.CHEST, new Profile(0xAB7742, MapColor.WOOD,
            SeismicSoundFamily.SPECIAL, SoundEvents.CHEST_OPEN, 0.85F));
        profiles.put(SeismicAnomalyType.SPAWNER, new Profile(0x52C4B5, MapColor.COLOR_CYAN,
            SeismicSoundFamily.SPECIAL, SeismicSoundFamily.SPECIAL.sound(), 0.85F));
        profiles.put(SeismicAnomalyType.SOLID, new Profile(0xA0A0A0, MapColor.STONE,
            SeismicSoundFamily.STONE, SoundEvents.NOTE_BLOCK_BASS.value(), 0.56F));
        return Map.copyOf(profiles);
    }

    private static Profile builtin(SeismicAnomalyType type) {
        Profile profile = BUILT_INS.get(type.mineralType());
        if (profile == null) {
            throw new IllegalArgumentException("Missing built-in seismic signal profile: " + type);
        }
        return profile;
    }

    public static int color(SeismicAnomalyType type) {
        return type.isCustom() ? SeismicCustomModules.color(type) : builtin(type).color();
    }

    public static int sidebarColor(SeismicAnomalyType type) {
        int color = color(type);
        int red = lighten(color >> 16 & 255);
        int green = lighten(color >> 8 & 255);
        int blue = lighten(color & 255);
        return red << 16 | green << 8 | blue;
    }

    private static int lighten(int channel) {
        return Math.round(channel + (255 - channel) * 0.22F);
    }

    public static MapColor mapColor(SeismicAnomalyType type) {
        return type.isCustom() ? SeismicCustomModules.mapColor(type) : builtin(type).mapColor();
    }

    public static SeismicSoundFamily soundFamily(SeismicAnomalyType type) {
        if (!type.isCustom()) {
            return builtin(type).family();
        }
        SeismicCustomModuleDefinition definition = SeismicCustomModules.definition(type);
        return definition != null ? definition.soundFamily() : SeismicSoundFamily.CRYSTAL;
    }

    public static SoundEvent echoSound(SeismicAnomalyType type) {
        return type.isCustom() ? soundFamily(type).sound() : builtin(type).sound();
    }

    public static float basePitch(SeismicAnomalyType type) {
        if (!type.isCustom()) {
            return builtin(type).basePitch();
        }
        SeismicCustomModuleDefinition definition = SeismicCustomModules.definition(type);
        return definition != null ? definition.basePitch() : 1.0F;
    }

    public static float adjustedPitch(float basePitch, int depth, int maxDepth) {
        float depthRatio = maxDepth > 0 ? Mth.clamp(depth / (float) maxDepth, 0.0F, 1.0F) : 1.0F;
        return Mth.clamp(basePitch - depthRatio * MAX_DEPTH_PITCH_SHIFT, 0.5F, 2.0F);
    }

    private record Profile(int color, MapColor mapColor, SeismicSoundFamily family, SoundEvent sound, float basePitch) {
    }
}
