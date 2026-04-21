package net.goldskinmc.creategeoresonance.seismic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeismicScanQueueTest {
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
