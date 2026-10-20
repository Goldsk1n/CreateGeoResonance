package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.resources.ResourceLocation;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stable saved identities; custom minerals must never share one generic signal type. */
public final class SeismicAnomalyType implements Comparable<SeismicAnomalyType> {
    public enum Kind {
        CAVE,
        WATER,
        LAVA,
        COAL,
        IRON,
        COPPER,
        GOLD,
        REDSTONE,
        LAPIS,
        EMERALD,
        DIAMOND,
        ZINC,
        AMETHYST,
        CHEST,
        SPAWNER,
        SOLID,
        CRIMSITE,
        ASURINE,
        OCHRUM,
        VERIDIUM,
        THORIUM,
        MAGNETITE,
        LEAD,
        NICKEL,
        LITHIUM,
        BAUXITE,
        GALENA,
        LIGNITE,
        SULFUR,
        CRUDE_OIL,
        WOLFRAMITE,
        URANIUM,
        AUTUNITE,
        NITRATE,
        TIN,
        SILVER,
        PLATINUM,
        OSMIUM,
        QUARTZ,
        COBALT,
        COAL_DEPOSIT,
        IRON_DEPOSIT,
        COPPER_DEPOSIT,
        GOLD_DEPOSIT,
        REDSTONE_DEPOSIT,
        LAPIS_DEPOSIT,
        ZINC_DEPOSIT,
        LEAD_DEPOSIT,
        NICKEL_DEPOSIT,
        URANIUM_DEPOSIT,
        THORIUM_DEPOSIT,
        WOLFRAMITE_DEPOSIT,
        TIN_DEPOSIT,
        SILVER_DEPOSIT,
        PLATINUM_DEPOSIT,
        OSMIUM_DEPOSIT,
        QUARTZ_DEPOSIT,
        COBALT_DEPOSIT,
        ANCIENT_DEBRIS,
        FLUORITE,
        APATITE,
        CINNABAR,
        NITER,
        CERTUS_QUARTZ,
        INFERIUM,
        PROSPERITY,
        SOULIUM,
        ARCANE_CRYSTAL,
        RUNE,
        STELLARITE,
        XPETRIFIED,
        URANINITE,
        DRACONIUM,
        CUSTOM
    }

    public static final SeismicAnomalyType CAVE = new SeismicAnomalyType(Kind.CAVE, null);
    public static final SeismicAnomalyType WATER = new SeismicAnomalyType(Kind.WATER, null);
    public static final SeismicAnomalyType LAVA = new SeismicAnomalyType(Kind.LAVA, null);
    public static final SeismicAnomalyType COAL = new SeismicAnomalyType(Kind.COAL, null);
    public static final SeismicAnomalyType IRON = new SeismicAnomalyType(Kind.IRON, null);
    public static final SeismicAnomalyType COPPER = new SeismicAnomalyType(Kind.COPPER, null);
    public static final SeismicAnomalyType GOLD = new SeismicAnomalyType(Kind.GOLD, null);
    public static final SeismicAnomalyType REDSTONE = new SeismicAnomalyType(Kind.REDSTONE, null);
    public static final SeismicAnomalyType LAPIS = new SeismicAnomalyType(Kind.LAPIS, null);
    public static final SeismicAnomalyType EMERALD = new SeismicAnomalyType(Kind.EMERALD, null);
    public static final SeismicAnomalyType DIAMOND = new SeismicAnomalyType(Kind.DIAMOND, null);
    public static final SeismicAnomalyType ZINC = new SeismicAnomalyType(Kind.ZINC, null);
    public static final SeismicAnomalyType AMETHYST = new SeismicAnomalyType(Kind.AMETHYST, null);
    public static final SeismicAnomalyType CHEST = new SeismicAnomalyType(Kind.CHEST, null);
    public static final SeismicAnomalyType SPAWNER = new SeismicAnomalyType(Kind.SPAWNER, null);
    public static final SeismicAnomalyType SOLID = new SeismicAnomalyType(Kind.SOLID, null);
    public static final SeismicAnomalyType CRIMSITE = new SeismicAnomalyType(Kind.CRIMSITE, null);
    public static final SeismicAnomalyType ASURINE = new SeismicAnomalyType(Kind.ASURINE, null);
    public static final SeismicAnomalyType OCHRUM = new SeismicAnomalyType(Kind.OCHRUM, null);
    public static final SeismicAnomalyType VERIDIUM = new SeismicAnomalyType(Kind.VERIDIUM, null);
    public static final SeismicAnomalyType THORIUM = new SeismicAnomalyType(Kind.THORIUM, null);
    public static final SeismicAnomalyType MAGNETITE = new SeismicAnomalyType(Kind.MAGNETITE, null);
    public static final SeismicAnomalyType LEAD = new SeismicAnomalyType(Kind.LEAD, null);
    public static final SeismicAnomalyType NICKEL = new SeismicAnomalyType(Kind.NICKEL, null);
    public static final SeismicAnomalyType LITHIUM = new SeismicAnomalyType(Kind.LITHIUM, null);
    public static final SeismicAnomalyType BAUXITE = new SeismicAnomalyType(Kind.BAUXITE, null);
    public static final SeismicAnomalyType GALENA = new SeismicAnomalyType(Kind.GALENA, null);
    public static final SeismicAnomalyType LIGNITE = new SeismicAnomalyType(Kind.LIGNITE, null);
    public static final SeismicAnomalyType SULFUR = new SeismicAnomalyType(Kind.SULFUR, null);
    public static final SeismicAnomalyType CRUDE_OIL = new SeismicAnomalyType(Kind.CRUDE_OIL, null);
    public static final SeismicAnomalyType WOLFRAMITE = new SeismicAnomalyType(Kind.WOLFRAMITE, null);
    public static final SeismicAnomalyType URANIUM = new SeismicAnomalyType(Kind.URANIUM, null);
    public static final SeismicAnomalyType AUTUNITE = new SeismicAnomalyType(Kind.AUTUNITE, null);
    public static final SeismicAnomalyType NITRATE = new SeismicAnomalyType(Kind.NITRATE, null);
    public static final SeismicAnomalyType TIN = new SeismicAnomalyType(Kind.TIN, null);
    public static final SeismicAnomalyType SILVER = new SeismicAnomalyType(Kind.SILVER, null);
    public static final SeismicAnomalyType PLATINUM = new SeismicAnomalyType(Kind.PLATINUM, null);
    public static final SeismicAnomalyType OSMIUM = new SeismicAnomalyType(Kind.OSMIUM, null);
    public static final SeismicAnomalyType QUARTZ = new SeismicAnomalyType(Kind.QUARTZ, null);
    public static final SeismicAnomalyType COBALT = new SeismicAnomalyType(Kind.COBALT, null);
    public static final SeismicAnomalyType COAL_DEPOSIT = new SeismicAnomalyType(Kind.COAL_DEPOSIT, COAL);
    public static final SeismicAnomalyType IRON_DEPOSIT = new SeismicAnomalyType(Kind.IRON_DEPOSIT, IRON);
    public static final SeismicAnomalyType COPPER_DEPOSIT = new SeismicAnomalyType(Kind.COPPER_DEPOSIT, COPPER);
    public static final SeismicAnomalyType GOLD_DEPOSIT = new SeismicAnomalyType(Kind.GOLD_DEPOSIT, GOLD);
    public static final SeismicAnomalyType REDSTONE_DEPOSIT = new SeismicAnomalyType(Kind.REDSTONE_DEPOSIT, REDSTONE);
    public static final SeismicAnomalyType LAPIS_DEPOSIT = new SeismicAnomalyType(Kind.LAPIS_DEPOSIT, LAPIS);
    public static final SeismicAnomalyType ZINC_DEPOSIT = new SeismicAnomalyType(Kind.ZINC_DEPOSIT, ZINC);
    public static final SeismicAnomalyType LEAD_DEPOSIT = new SeismicAnomalyType(Kind.LEAD_DEPOSIT, LEAD);
    public static final SeismicAnomalyType NICKEL_DEPOSIT = new SeismicAnomalyType(Kind.NICKEL_DEPOSIT, NICKEL);
    public static final SeismicAnomalyType URANIUM_DEPOSIT = new SeismicAnomalyType(Kind.URANIUM_DEPOSIT, URANIUM);
    public static final SeismicAnomalyType THORIUM_DEPOSIT = new SeismicAnomalyType(Kind.THORIUM_DEPOSIT, THORIUM);
    public static final SeismicAnomalyType WOLFRAMITE_DEPOSIT = new SeismicAnomalyType(Kind.WOLFRAMITE_DEPOSIT, WOLFRAMITE);
    public static final SeismicAnomalyType TIN_DEPOSIT = new SeismicAnomalyType(Kind.TIN_DEPOSIT, TIN);
    public static final SeismicAnomalyType SILVER_DEPOSIT = new SeismicAnomalyType(Kind.SILVER_DEPOSIT, SILVER);
    public static final SeismicAnomalyType PLATINUM_DEPOSIT = new SeismicAnomalyType(Kind.PLATINUM_DEPOSIT, PLATINUM);
    public static final SeismicAnomalyType OSMIUM_DEPOSIT = new SeismicAnomalyType(Kind.OSMIUM_DEPOSIT, OSMIUM);
    public static final SeismicAnomalyType QUARTZ_DEPOSIT = new SeismicAnomalyType(Kind.QUARTZ_DEPOSIT, QUARTZ);
    public static final SeismicAnomalyType COBALT_DEPOSIT = new SeismicAnomalyType(Kind.COBALT_DEPOSIT, COBALT);
    public static final SeismicAnomalyType ANCIENT_DEBRIS = new SeismicAnomalyType(Kind.ANCIENT_DEBRIS, null);
    public static final SeismicAnomalyType FLUORITE = new SeismicAnomalyType(Kind.FLUORITE, null);
    public static final SeismicAnomalyType APATITE = new SeismicAnomalyType(Kind.APATITE, null);
    public static final SeismicAnomalyType CINNABAR = new SeismicAnomalyType(Kind.CINNABAR, null);
    public static final SeismicAnomalyType NITER = new SeismicAnomalyType(Kind.NITER, null);
    public static final SeismicAnomalyType CERTUS_QUARTZ = new SeismicAnomalyType(Kind.CERTUS_QUARTZ, null);
    public static final SeismicAnomalyType INFERIUM = new SeismicAnomalyType(Kind.INFERIUM, null);
    public static final SeismicAnomalyType PROSPERITY = new SeismicAnomalyType(Kind.PROSPERITY, null);
    public static final SeismicAnomalyType SOULIUM = new SeismicAnomalyType(Kind.SOULIUM, null);
    public static final SeismicAnomalyType ARCANE_CRYSTAL = new SeismicAnomalyType(Kind.ARCANE_CRYSTAL, null);
    public static final SeismicAnomalyType RUNE = new SeismicAnomalyType(Kind.RUNE, null);
    public static final SeismicAnomalyType STELLARITE = new SeismicAnomalyType(Kind.STELLARITE, null);
    public static final SeismicAnomalyType XPETRIFIED = new SeismicAnomalyType(Kind.XPETRIFIED, null);
    public static final SeismicAnomalyType URANINITE = new SeismicAnomalyType(Kind.URANINITE, null);
    public static final SeismicAnomalyType DRACONIUM = new SeismicAnomalyType(Kind.DRACONIUM, null);

    private static final SeismicAnomalyType[] BUILT_INS = {
        CAVE,
        WATER,
        LAVA,
        COAL,
        IRON,
        COPPER,
        GOLD,
        REDSTONE,
        LAPIS,
        EMERALD,
        DIAMOND,
        ZINC,
        AMETHYST,
        CHEST,
        SPAWNER,
        SOLID,
        CRIMSITE,
        ASURINE,
        OCHRUM,
        VERIDIUM,
        THORIUM,
        MAGNETITE,
        LEAD,
        NICKEL,
        LITHIUM,
        BAUXITE,
        GALENA,
        LIGNITE,
        SULFUR,
        CRUDE_OIL,
        WOLFRAMITE,
        URANIUM,
        AUTUNITE,
        NITRATE,
        TIN,
        SILVER,
        PLATINUM,
        OSMIUM,
        QUARTZ,
        COBALT,
        COAL_DEPOSIT,
        IRON_DEPOSIT,
        COPPER_DEPOSIT,
        GOLD_DEPOSIT,
        REDSTONE_DEPOSIT,
        LAPIS_DEPOSIT,
        ZINC_DEPOSIT,
        LEAD_DEPOSIT,
        NICKEL_DEPOSIT,
        URANIUM_DEPOSIT,
        THORIUM_DEPOSIT,
        WOLFRAMITE_DEPOSIT,
        TIN_DEPOSIT,
        SILVER_DEPOSIT,
        PLATINUM_DEPOSIT,
        OSMIUM_DEPOSIT,
        QUARTZ_DEPOSIT,
        COBALT_DEPOSIT,
        ANCIENT_DEBRIS,
        FLUORITE,
        APATITE,
        CINNABAR,
        NITER,
        CERTUS_QUARTZ,
        INFERIUM,
        PROSPERITY,
        SOULIUM,
        ARCANE_CRYSTAL,
        RUNE,
        STELLARITE,
        XPETRIFIED,
        URANINITE,
        DRACONIUM,
    };
    private static final Map<ResourceLocation, SeismicAnomalyType> CUSTOM_TYPES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(BUILT_INS.length);
    private final Kind kind;
    private final String name;
    private final int ordinal;
    private final SeismicAnomalyType mineral;

    private SeismicAnomalyType(Kind kind, SeismicAnomalyType mineral) {
        this.kind = kind;
        this.name = kind.name();
        this.ordinal = kind.ordinal();
        this.mineral = mineral;
    }

    private SeismicAnomalyType(ResourceLocation id) {
        kind = Kind.CUSTOM;
        name = id.toString();
        ordinal = NEXT_ORDINAL.getAndIncrement();
        mineral = null;
    }

    public static SeismicAnomalyType custom(ResourceLocation id) {
        return CUSTOM_TYPES.computeIfAbsent(id, SeismicAnomalyType::new);
    }

    public static SeismicAnomalyType valueOf(String name) {
        if (name.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) {
                throw new IllegalArgumentException("Invalid custom seismic resource ID: " + name);
            }
            return custom(id);
        }
        for (SeismicAnomalyType type : BUILT_INS) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown seismic resource: " + name);
    }

    public static SeismicAnomalyType[] values() {
        return Arrays.copyOf(BUILT_INS, BUILT_INS.length);
    }

    public Kind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    /** Runtime-only key, never written to saves or packets. */
    public int ordinal() {
        return ordinal;
    }

    public boolean isCustom() {
        return kind == Kind.CUSTOM;
    }

    public boolean isRockStoneDeposit() {
        return mineral != null;
    }

    public SeismicAnomalyType mineralType() {
        return mineral != null ? mineral : this;
    }

    @Override
    public int compareTo(SeismicAnomalyType other) {
        if (!isCustom() || !other.isCustom()) {
            return Integer.compare(ordinal, other.ordinal);
        }
        return name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
