package net.goldskinmc.creategeoresonance.seismic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeismicScanQueueTest {
    @Test
    void balancesAbundantTargetsInsteadOfLettingTheLastModuleMonopolizeTheCap() {
        List<SeismicAnomaly> anomalies = abundantCopperAndDiamondSignals();

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 8, Map.of(SeismicAnomalyType.COPPER, 3, SeismicAnomalyType.DIAMOND, 4));

        assertEquals(List.of(SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER,
            SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER,
            SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER,
            SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER),
            selected.stream().map(SeismicAnomaly::type).toList());
        assertEquals(1.0F, selected.get(0).confidence());
        assertEquals(1.0F, selected.get(1).confidence());
    }

    @Test
    void lastInstalledModuleGetsTheExtraSlotForAnOddCap() {
        List<SeismicAnomaly> diamondLast = SeismicScanQueue.prioritizeAndLimitAnomalies(
            abundantCopperAndDiamondSignals(), 3, 5,
            Map.of(SeismicAnomalyType.COPPER, 3, SeismicAnomalyType.DIAMOND, 4));
        List<SeismicAnomaly> copperLast = SeismicScanQueue.prioritizeAndLimitAnomalies(
            abundantCopperAndDiamondSignals(), 3, 5,
            Map.of(SeismicAnomalyType.COPPER, 4, SeismicAnomalyType.DIAMOND, 3));

        assertEquals(List.of(SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER,
            SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER, SeismicAnomalyType.DIAMOND),
            diamondLast.stream().map(SeismicAnomaly::type).toList());
        assertEquals(List.of(SeismicAnomalyType.COPPER, SeismicAnomalyType.DIAMOND,
            SeismicAnomalyType.COPPER, SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER),
            copperLast.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void exhaustedTypesYieldSlotsBeforeAmbientSignalsAreAdded() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.DIAMOND, 0, 0, 10, 1, 0.1F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 10, 0, 10, 1, 0.3F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 20, 0, 10, 1, 0.2F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 30, 0, 10, 1, 0.1F),
            new SeismicAnomaly(SeismicAnomalyType.WATER, 40, 0, 10, 1, 1.0F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 5, Map.of(SeismicAnomalyType.COPPER, 3, SeismicAnomalyType.DIAMOND, 4));

        assertEquals(List.of(SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER,
            SeismicAnomalyType.COPPER, SeismicAnomalyType.COPPER, SeismicAnomalyType.WATER),
            selected.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void differentModuleTargetsSurviveAtTheSameHorizontalPosition() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 0, 0, 10, 1, 0.9F),
            new SeismicAnomaly(SeismicAnomalyType.DIAMOND, 0, 0, 30, 1, 0.1F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 8, Map.of(SeismicAnomalyType.COPPER, 3, SeismicAnomalyType.DIAMOND, 4));

        assertEquals(List.of(SeismicAnomalyType.DIAMOND, SeismicAnomalyType.COPPER),
            selected.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void oreAndDepositVariantsShareOneTurnPerMineral() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 0, 0, 10, 1, 0.9F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER_DEPOSIT, 10, 0, 10, 1, 0.8F),
            new SeismicAnomaly(SeismicAnomalyType.DIAMOND, 20, 0, 10, 1, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.DIAMOND, 30, 0, 10, 1, 0.9F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 3, Map.of(SeismicAnomalyType.COPPER, 4, SeismicAnomalyType.DIAMOND, 3));

        assertEquals(List.of(SeismicAnomalyType.COPPER, SeismicAnomalyType.DIAMOND,
            SeismicAnomalyType.COPPER_DEPOSIT), selected.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void overlappingOreAndDepositVariantsStillMergeWithinTheirMineral() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 0, 0, 10, 1, 0.9F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER_DEPOSIT, 1, 0, 10, 1, 0.8F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 8, Map.of(SeismicAnomalyType.COPPER, 3));

        assertEquals(List.of(SeismicAnomalyType.COPPER),
            selected.stream().map(SeismicAnomaly::type).toList());
    }

    private static List<SeismicAnomaly> abundantCopperAndDiamondSignals() {
        List<SeismicAnomaly> anomalies = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            anomalies.add(new SeismicAnomaly(SeismicAnomalyType.COPPER, i * 10, 0, 10, 1, 1.0F - i * 0.05F));
            anomalies.add(new SeismicAnomaly(SeismicAnomalyType.DIAMOND, i * 10, 20, 10, 1, 1.0F - i * 0.05F));
        }
        return anomalies;
    }

    @Test
    void moduleTargetWinsAgainstOverlappingAmbientSignals() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 0, 0, 10, 1, 0.1F),
            new SeismicAnomaly(SeismicAnomalyType.WATER, 1, 0, 10, 2, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.LAVA, 2, 0, 10, 2, 1.0F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 8, Map.of(SeismicAnomalyType.COPPER, 3));

        assertEquals(List.of(SeismicAnomalyType.COPPER),
            selected.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void echoLimitKeepsModuleTargetsBeforeAmbientSignals() {
        List<SeismicAnomaly> anomalies = new ArrayList<>(List.of(
            new SeismicAnomaly(SeismicAnomalyType.WATER, 20, 0, 10, 1, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.LAVA, 30, 0, 10, 1, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.CAVE, 40, 0, 10, 1, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 0, 0, 10, 1, 0.2F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER, 10, 0, 10, 1, 0.1F)
        ));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(
            anomalies, 3, 3, Map.of(SeismicAnomalyType.COPPER, 3));

        assertEquals(List.of(SeismicAnomalyType.COPPER, SeismicAnomalyType.COPPER, SeismicAnomalyType.LAVA),
            selected.stream().map(SeismicAnomaly::type).toList());
    }

    @Test
    void laterModuleWinsForNormalOresAndDepositVariants() {
        List<SeismicAnomaly> anomalies = List.of(
            new SeismicAnomaly(SeismicAnomalyType.IRON, 0, 0, 10, 1, 1.0F),
            new SeismicAnomaly(SeismicAnomalyType.COPPER_DEPOSIT, 1, 0, 10, 1, 0.1F)
        );

        List<SeismicAnomaly> copperLast = SeismicScanQueue.prioritizeAndLimitAnomalies(
            new ArrayList<>(anomalies), 3, 1,
            Map.of(SeismicAnomalyType.IRON, 3, SeismicAnomalyType.COPPER, 4));
        List<SeismicAnomaly> ironLast = SeismicScanQueue.prioritizeAndLimitAnomalies(
            new ArrayList<>(anomalies), 3, 1,
            Map.of(SeismicAnomalyType.IRON, 4, SeismicAnomalyType.COPPER, 3));

        assertEquals(SeismicAnomalyType.COPPER_DEPOSIT, copperLast.get(0).type());
        assertEquals(SeismicAnomalyType.IRON, ironLast.get(0).type());
    }

    @Test
    void prioritizesLavaOverOtherTypesInsideMergeDistance() {
        List<SeismicAnomaly> anomalies = new ArrayList<>();
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.CAVE, 0, 0, 8, 2, 0.9F));
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.WATER, 1, 0, 9, 2, 0.8F));
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.LAVA, 2, 1, 10, 1, 0.2F));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(anomalies, 5, 8);

        assertEquals(1, selected.size(), "Overlapping echoes should collapse to one result");
        assertEquals(SeismicAnomalyType.LAVA, selected.get(0).type(), "Lava should win priority tie-breaks");
    }

    @Test
    void keepsSeparatedAnomaliesAndLimitsCount() {
        List<SeismicAnomaly> anomalies = new ArrayList<>();
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.CAVE, 0, 0, 5, 1, 0.8F));
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.WATER, 10, 0, 7, 1, 0.5F));
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.LAVA, -10, 0, 9, 1, 0.4F));
        anomalies.add(new SeismicAnomaly(SeismicAnomalyType.CAVE, 0, 12, 4, 1, 1.0F));

        List<SeismicAnomaly> selected = SeismicScanQueue.prioritizeAndLimitAnomalies(anomalies, 3, 3);

        assertEquals(3, selected.size(), "Echo output should respect max echo limit");
        assertEquals(SeismicAnomalyType.LAVA, selected.get(0).type(), "Highest priority type should be sorted first");
        assertEquals(SeismicAnomalyType.WATER, selected.get(1).type(), "Water should be second priority");
        assertEquals(SeismicAnomalyType.CAVE, selected.get(2).type(), "Remaining cave should follow");
    }
}
