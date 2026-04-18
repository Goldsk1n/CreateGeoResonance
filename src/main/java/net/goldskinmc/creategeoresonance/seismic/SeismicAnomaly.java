package net.goldskinmc.creategeoresonance.seismic;

public record SeismicAnomaly(SeismicAnomalyType type, int offsetX, int offsetZ, int depth, int radius, float confidence) {
}
