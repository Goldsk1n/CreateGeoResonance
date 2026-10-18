package net.goldskinmc.creategeoresonance.seismic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class SeismicProjectorVeinSelector {
    private SeismicProjectorVeinSelector() {
    }

    public static <T> List<T> select(List<T> veins, int maxVisible,
                                     Function<T, SeismicAnomalyType> typeOf, ToDoubleFunction<T> distanceSquared) {
        if (veins.isEmpty() || maxVisible <= 0) {
            return List.of();
        }
        if (veins.size() <= maxVisible) {
            return veins;
        }

        List<RankedVein<T>> ranked = new ArrayList<>(veins.size());
        for (T vein : veins) {
            ranked.add(new RankedVein<>(vein, typeOf.apply(vein).mineralType(), distanceSquared.applyAsDouble(vein)));
        }
        ranked.sort(Comparator.comparingDouble(RankedVein<T>::distanceSquared)
            .thenComparing(vein -> vein.mineral().name()));

        // Nearest representatives determine type order; variants share one resource's turns.
        Map<SeismicAnomalyType, Deque<T>> groups = new LinkedHashMap<>();
        for (RankedVein<T> vein : ranked) {
            groups.computeIfAbsent(vein.mineral(), ignored -> new ArrayDeque<>()).addLast(vein.vein());
        }

        List<T> selected = new ArrayList<>(maxVisible);
        while (!groups.isEmpty() && selected.size() < maxVisible) {
            var iterator = groups.values().iterator();
            while (iterator.hasNext() && selected.size() < maxVisible) {
                Deque<T> group = iterator.next();
                selected.add(group.removeFirst());
                if (group.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        return selected;
    }

    private record RankedVein<T>(T vein, SeismicAnomalyType mineral, double distanceSquared) {
    }
}
