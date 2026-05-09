package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Task(
    Long id,
    ProcessInstance instance,
    TaskDefinition taskDefinition,
    User assignee,
    String status,
    Integer plannedDuration,
    Integer actualDuration,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime dueDate
) {
    public String getTaskName() {
        return taskDefinition != null ? taskDefinition.name() : "Unknown";
    }

    public BigDecimal getExpectedCost() {
        return taskDefinition != null ? taskDefinition.expectedCost() : BigDecimal.ZERO;
    }

    public String getAssigneeName() {
        return assignee != null ? assignee.firstName() + " " + assignee.lastName() : "-";
    }
}
