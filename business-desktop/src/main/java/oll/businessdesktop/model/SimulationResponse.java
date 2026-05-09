package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulationResponse(
    Long modelId,
    String modelName,
    KpiPair cycleTime,
    KpiPair totalCost,
    KpiPair resourceLoad,
    List<TaskPrediction> taskPredictions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KpiPair(Double baseline, Double scenario) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaskPrediction(
        String taskName,
        String elementId,
        Double baseDuration,
        Double scenarioDuration,
        Double baseCost,
        Double scenarioCost,
        Double resourceLoadPercent
    ) {}
}
