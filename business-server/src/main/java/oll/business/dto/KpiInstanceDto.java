package oll.business.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class KpiInstanceDto {
    private Long instanceId;
    private String modelId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer plannedDuration;
    private Integer actualDuration;
    private Double deviationPercent;
    private List<KpiInstanceTaskDto> tasks;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getPlannedDuration() { return plannedDuration; }
    public void setPlannedDuration(Integer plannedDuration) { this.plannedDuration = plannedDuration; }
    public Integer getActualDuration() { return actualDuration; }
    public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }
    public Double getDeviationPercent() { return deviationPercent; }
    public void setDeviationPercent(Double deviationPercent) { this.deviationPercent = deviationPercent; }
    public List<KpiInstanceTaskDto> getTasks() { return tasks; }
    public void setTasks(List<KpiInstanceTaskDto> tasks) { this.tasks = tasks; }

    public static class KpiInstanceTaskDto {
        private Long taskId;
        private String elementId;
        private String taskName;
        private String status;
        private Integer plannedDuration;
        private Integer actualDuration;
        private Double deviationPercent;
        private BigDecimal kpiWeight;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getElementId() { return elementId; }
        public void setElementId(String elementId) { this.elementId = elementId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getPlannedDuration() { return plannedDuration; }
        public void setPlannedDuration(Integer plannedDuration) { this.plannedDuration = plannedDuration; }
        public Integer getActualDuration() { return actualDuration; }
        public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }
        public Double getDeviationPercent() { return deviationPercent; }
        public void setDeviationPercent(Double deviationPercent) { this.deviationPercent = deviationPercent; }
        public BigDecimal getKpiWeight() { return kpiWeight; }
        public void setKpiWeight(BigDecimal kpiWeight) { this.kpiWeight = kpiWeight; }
    }
}
