package oll.business.dto;

public class TaskPrediction {
    private String taskName;
    private String elementId;
    private Double baseDuration;
    private Double scenarioDuration;
    private Double baseCost;
    private Double scenarioCost;
    private Double resourceLoadPercent;

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }
    public Double getBaseDuration() { return baseDuration; }
    public void setBaseDuration(Double baseDuration) { this.baseDuration = baseDuration; }
    public Double getScenarioDuration() { return scenarioDuration; }
    public void setScenarioDuration(Double scenarioDuration) { this.scenarioDuration = scenarioDuration; }
    public Double getBaseCost() { return baseCost; }
    public void setBaseCost(Double baseCost) { this.baseCost = baseCost; }
    public Double getScenarioCost() { return scenarioCost; }
    public void setScenarioCost(Double scenarioCost) { this.scenarioCost = scenarioCost; }
    public Double getResourceLoadPercent() { return resourceLoadPercent; }
    public void setResourceLoadPercent(Double resourceLoadPercent) { this.resourceLoadPercent = resourceLoadPercent; }
}
