package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KpiModelData(
    Long modelId,
    String modelName,
    Double avgDuration,
    Double delayRate,
    Double rating,
    Integer totalInstances,
    Integer completedInstances,
    List<KpiTaskData> tasks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KpiTaskData(
        String elementId,
        String name,
        BigDecimal kpiWeight
    ) {}
}
