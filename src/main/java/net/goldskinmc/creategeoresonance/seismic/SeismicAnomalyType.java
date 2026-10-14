package net.goldskinmc.creategeoresonance.seismic;

public enum SeismicAnomalyType {
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
    COAL_DEPOSIT(COAL),
    IRON_DEPOSIT(IRON),
    COPPER_DEPOSIT(COPPER),
    GOLD_DEPOSIT(GOLD),
    REDSTONE_DEPOSIT(REDSTONE),
    LAPIS_DEPOSIT(LAPIS),
    ZINC_DEPOSIT(ZINC),
    LEAD_DEPOSIT(LEAD),
    NICKEL_DEPOSIT(NICKEL),
    URANIUM_DEPOSIT(URANIUM),
    THORIUM_DEPOSIT(THORIUM),
    WOLFRAMITE_DEPOSIT(WOLFRAMITE),
    TIN_DEPOSIT(TIN),
    SILVER_DEPOSIT(SILVER),
    PLATINUM_DEPOSIT(PLATINUM),
    OSMIUM_DEPOSIT(OSMIUM),
    QUARTZ_DEPOSIT(QUARTZ),
    COBALT_DEPOSIT(COBALT),
    ANCIENT_DEBRIS,
    FLUORITE,
    CERTUS_QUARTZ,
    INFERIUM,
    PROSPERITY,
    SOULIUM,
    ARCANE_CRYSTAL,
    RUNE,
    STELLARITE,
    URANINITE,
    DRACONIUM,
    BLACK_QUARTZ;

    private final SeismicAnomalyType mineral;

    SeismicAnomalyType() {
        this.mineral = null;
    }

    SeismicAnomalyType(SeismicAnomalyType mineral) {
        this.mineral = mineral;
    }

    public boolean isRockStoneDeposit() {
        return mineral != null;
    }

    public SeismicAnomalyType mineralType() {
        return mineral != null ? mineral : this;
    }
}
