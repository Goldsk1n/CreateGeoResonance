package net.goldskinmc.creategeoresonance.seismic;

import org.jetbrains.annotations.Nullable;

public enum SeismicModuleType {
    BELOW_ZERO("below_zero_module", null),
    AMETHYST("amethyst_module", SeismicAnomalyType.AMETHYST),
    CHEST("chest_module", SeismicAnomalyType.CHEST),
    SPAWNER("spawner_module", SeismicAnomalyType.SPAWNER),
    COAL("coal_resonance_module", SeismicAnomalyType.COAL),
    IRON("iron_resonance_module", SeismicAnomalyType.IRON),
    COPPER("copper_resonance_module", SeismicAnomalyType.COPPER),
    GOLD("gold_resonance_module", SeismicAnomalyType.GOLD),
    REDSTONE("redstone_resonance_module", SeismicAnomalyType.REDSTONE),
    LAPIS("lapis_resonance_module", SeismicAnomalyType.LAPIS),
    EMERALD("emerald_resonance_module", SeismicAnomalyType.EMERALD),
    DIAMOND("diamond_resonance_module", SeismicAnomalyType.DIAMOND),
    ZINC("zinc_resonance_module", SeismicAnomalyType.ZINC);

    private final String itemName;
    @Nullable
    private final SeismicAnomalyType detectedType;

    SeismicModuleType(String itemName, @Nullable SeismicAnomalyType detectedType) {
        this.itemName = itemName;
        this.detectedType = detectedType;
    }

    public String itemName() {
        return itemName;
    }

    @Nullable
    public SeismicAnomalyType detectedType() {
        return detectedType;
    }
}
