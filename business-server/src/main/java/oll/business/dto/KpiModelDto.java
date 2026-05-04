package oll.business.dto;

import java.math.BigDecimal;
import java.util.List;

public class KpiModelDto {
    private Long modelId;
    private String modelName;
    private Double avgDuration;
    private Double delayRate;
    private Double rating;
    private Integer totalInstances;
    private Integer completedInstances;
    private List<KpiTaskDto> tasks;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Double getAvgDuration() { return avgDuration; }
    public void setAvgDuration(Double avgDuration) { this.avgDuration = avgDuration; }
    public Double getDelayRate() { return delayRate; }
    public void setDelayRate(Double delayRate) { this.delayRate = delayRate; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalInstances() { return totalInstances; }
    public void setTotalInstances(Integer totalInstances) { this.totalInstances = totalInstances; }
    public Integer getCompletedInstances() { return completedInstances; }
    public void setCompletedInstances(Integer completedInstances) { this.completedInstances = completedInstances; }
    public List<KpiTaskDto> getTasks() { return tasks; }
    public void setTasks(List<KpiTaskDto> tasks) { this.tasks = tasks; }

    public static class KpiTaskDto {
        private String elementId;
        private String name;
        private BigDecimal kpiWeight;

        public String getElementId() { return elementId; }
        public void setElementId(String elementId) { this.elementId = elementId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getKpiWeight() { return kpiWeight; }
        public void setKpiWeight(BigDecimal kpiWeight) { this.kpiWeight = kpiWeight; }
    }
}
