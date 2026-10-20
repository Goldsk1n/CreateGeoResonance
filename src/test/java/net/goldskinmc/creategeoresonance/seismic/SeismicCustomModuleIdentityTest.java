package net.goldskinmc.creategeoresonance.seismic;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeismicCustomModuleIdentityTest {
    @Test
    void builtInNamesAndRuntimeOrdinalsRemainCompatible() {
        SeismicAnomalyType[] types = SeismicAnomalyType.values();
        assertEquals(SeismicAnomalyType.Kind.CUSTOM.ordinal(), types.length);
        for (int index = 0; index < types.length; index++) {
            assertEquals(index, types[index].ordinal());
            assertSame(types[index], SeismicAnomalyType.valueOf(types[index].name()));
            assertFalse(types[index].isCustom());
        }
        assertSame(SeismicAnomalyType.COPPER, SeismicAnomalyType.COPPER_DEPOSIT.mineralType());
        assertThrows(IllegalArgumentException.class, () -> SeismicAnomalyType.valueOf("NOT_A_REAL_BUILTIN"));
    }

    @Test
    void customNamesAreStableAndNeverCollapseIntoOneGenericType() {
        SeismicAnomalyType ruby = SeismicAnomalyType.custom(ResourceLocation.fromNamespaceAndPath("pack", "ruby"));
        SeismicAnomalyType sapphire = SeismicAnomalyType.custom(ResourceLocation.fromNamespaceAndPath("pack", "sapphire"));
        assertSame(ruby, SeismicAnomalyType.valueOf("pack:ruby"));
        assertSame(sapphire, SeismicAnomalyType.valueOf("pack:sapphire"));
        assertNotSame(ruby, sapphire);
        assertNotEquals(ruby.ordinal(), sapphire.ordinal());
        assertSame(ruby, ruby.mineralType());
        assertTrue(ruby.isCustom());
        assertFalse(ruby.isRockStoneDeposit());
    }

    @Test
    void roundRobinTreatsCustomMineralsAndBuiltInsIndependently() {
        SeismicAnomalyType first = SeismicAnomalyType.valueOf("pack:round_robin_a");
        SeismicAnomalyType second = SeismicAnomalyType.valueOf("pack:round_robin_b");
        List<Vein> veins = List.of(new Vein(first, 1), new Vein(first, 2),
            new Vein(first, 3), new Vein(second, 100), new Vein(SeismicAnomalyType.COPPER, 200));
        List<Vein> selected = SeismicProjectorVeinSelector.select(veins, 3, Vein::type, Vein::distance);
        assertEquals(List.of(first, second, SeismicAnomalyType.COPPER), selected.stream().map(Vein::type).toList());
    }

    private record Vein(SeismicAnomalyType type, double distance) {
    }
}
