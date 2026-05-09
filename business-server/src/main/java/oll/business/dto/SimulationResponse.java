package oll.business.dto;

import java.util.List;

public class SimulationResponse {
    private Long modelId;
    private String modelName;
    private KpiPair cycleTime;
    private KpiPair totalCost;
    private KpiPair resourceLoad;
    private List<TaskPrediction> taskPredictions;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public KpiPair getCycleTime() { return cycleTime; }
    public void setCycleTime(KpiPair cycleTime) { this.cycleTime = cycleTime; }
    public KpiPair getTotalCost() { return totalCost; }
    public void setTotalCost(KpiPair totalCost) { this.totalCost = totalCost; }
    public KpiPair getResourceLoad() { return resourceLoad; }
    public void setResourceLoad(KpiPair resourceLoad) { this.resourceLoad = resourceLoad; }
    public List<TaskPrediction> getTaskPredictions() { return taskPredictions; }
    public void setTaskPredictions(List<TaskPrediction> taskPredictions) { this.taskPredictions = taskPredictions; }
}
