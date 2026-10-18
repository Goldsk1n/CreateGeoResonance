package net.goldskinmc.creategeoresonance.seismic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeismicProjectorVeinSelectorTest {
    @Test
    void abundantNearbyCopperDoesNotHideOtherResourcesAtTheDefaultCap() {
        List<Vein> veins = fourResourceVeins();
        List<Vein> original = List.copyOf(veins);

        List<Vein> selected = select(veins, 10);

        assertEquals(List.of("c0", "a0", "g0", "r0", "c1", "a1", "g1", "r1", "c2", "a2"),
            selected.stream().map(Vein::id).toList());
        assertEquals(3, count(selected, SeismicAnomalyType.COPPER));
        assertEquals(3, count(selected, SeismicAnomalyType.AMETHYST));
        assertEquals(2, count(selected, SeismicAnomalyType.GOLD));
        assertEquals(2, count(selected, SeismicAnomalyType.REDSTONE));
        assertEquals(original, veins);
    }

    @Test
    void smallCapsRepresentTheNearestTypesBeforeRepeatingAnyType() {
        assertEquals(List.of("c0", "a0", "g0", "r0"),
            select(fourResourceVeins(), 4).stream().map(Vein::id).toList());
        assertEquals(List.of("c0", "a0", "g0"),
            select(fourResourceVeins(), 3).stream().map(Vein::id).toList());
    }

    @Test
    void exhaustedTypesYieldTheirUnusedSlots() {
        List<Vein> veins = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            veins.add(new Vein("c" + i, SeismicAnomalyType.COPPER, i + 1, i + 1));
        }
        veins.add(new Vein("g0", SeismicAnomalyType.GOLD, 100, 100));

        List<Vein> selected = select(veins, 6);

        assertEquals(List.of("c0", "g0", "c1", "c2", "c3", "c4"),
            selected.stream().map(Vein::id).toList());
    }

    @Test
    void oreAndDepositVariantsShareOneMineralTurn() {
        List<Vein> veins = List.of(
            new Vein("c0", SeismicAnomalyType.COPPER, 1, 1),
            new Vein("cd0", SeismicAnomalyType.COPPER_DEPOSIT, 2, 2),
            new Vein("c1", SeismicAnomalyType.COPPER, 3, 3),
            new Vein("g0", SeismicAnomalyType.GOLD, 100, 100),
            new Vein("gd0", SeismicAnomalyType.GOLD_DEPOSIT, 101, 101));

        assertEquals(List.of("c0", "g0", "cd0", "gd0"),
            select(veins, 4).stream().map(Vein::id).toList());
    }

    @Test
    void equalDistanceTypeOrderIsStableEvenWhenInputOrderChanges() {
        List<Vein> veins = new ArrayList<>(List.of(
            new Vein("r0", SeismicAnomalyType.REDSTONE, 1, 1),
            new Vein("g0", SeismicAnomalyType.GOLD, 1, 1),
            new Vein("c0", SeismicAnomalyType.COPPER, 1, 1),
            new Vein("a0", SeismicAnomalyType.AMETHYST, 1, 1)));
        List<Vein> selected = select(veins, 2);
        Collections.reverse(veins);

        assertEquals(List.of("a0", "c0"), selected.stream().map(Vein::id).toList());
        assertEquals(selected, select(veins, 2));
    }

    @Test
    void partiallyMinedCentersDoNotChangeSelectionButRemovingAVeinFreesItsSlot() {
        List<Vein> veins = fourResourceVeins();
        List<String> before = select(veins, 10).stream().map(Vein::id).toList();
        veins.set(0, new Vein("c0", SeismicAnomalyType.COPPER, 1, 10000));

        assertEquals(before, select(veins, 10).stream().map(Vein::id).toList());
        veins.remove(0);
        List<Vein> after = select(veins, 10);

        assertEquals(10, after.size());
        assertTrue(after.stream().noneMatch(vein -> vein.id().equals("c0")));
        assertTrue(after.stream().anyMatch(vein -> vein.id().equals("c3")));
    }

    @Test
    void nearestVeinsAreChosenWithinEachTypeEvenWithUnsortedInput() {
        List<Vein> veins = fourResourceVeins();
        Collections.reverse(veins);

        assertEquals(List.of("c0", "a0", "g0", "r0", "c1", "a1"),
            select(veins, 6).stream().map(Vein::id).toList());
    }

    @Test
    void zeroLimitHidesEverythingAndAnUnrestrictiveLimitKeepsAllEligibleVeins() {
        List<Vein> veins = fourResourceVeins();

        assertTrue(select(veins, 0).isEmpty());
        assertTrue(select(veins, -1).isEmpty());
        assertTrue(select(List.of(), 10).isEmpty());
        assertSame(veins, select(veins, veins.size()));
        assertSame(veins, select(veins, 256));
    }

    private static List<Vein> select(List<Vein> veins, int maxVisible) {
        return SeismicProjectorVeinSelector.select(veins, maxVisible, Vein::type, Vein::surveyedDistance);
    }

    private static long count(List<Vein> veins, SeismicAnomalyType type) {
        return veins.stream().filter(vein -> vein.type().mineralType() == type).count();
    }

    private static List<Vein> fourResourceVeins() {
        List<Vein> veins = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            veins.add(new Vein("c" + i, SeismicAnomalyType.COPPER, i + 1, i + 1));
        }
        for (int i = 0; i < 4; i++) {
            veins.add(new Vein("a" + i, SeismicAnomalyType.AMETHYST, 100 + i, 100 + i));
            veins.add(new Vein("g" + i, SeismicAnomalyType.GOLD, 200 + i, 200 + i));
            veins.add(new Vein("r" + i, SeismicAnomalyType.REDSTONE, 300 + i, 300 + i));
        }
        return veins;
    }

    private record Vein(String id, SeismicAnomalyType type, double surveyedDistance, double liveDistance) {
    }
}
