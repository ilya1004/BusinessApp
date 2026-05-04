package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KpiInstanceData(
    Long instanceId,
    String modelId,
    String status,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Integer plannedDuration,
    Integer actualDuration,
    Double deviationPercent,
    List<KpiInstanceTaskData> tasks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KpiInstanceTaskData(
        Long taskId,
        String elementId,
        String taskName,
        String status,
        Integer plannedDuration,
        Integer actualDuration,
        Double deviationPercent,
        BigDecimal kpiWeight
    ) {}
}
