package oll.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SimulationRequest {

    @NotNull(message = "modelId is required")
    private Long modelId;

    @NotNull(message = "durationMultiplier is required")
    @Min(value = 5, message = "durationMultiplier must be >= 0.5")
    @Max(value = 20, message = "durationMultiplier must be <= 2.0")
    private Integer durationMultiplierX10;

    @NotNull(message = "resourcesPerTask is required")
    @Min(value = 1)
    @Max(value = 10)
    private Integer resourcesPerTask;

    @NotNull(message = "parallelismFactor is required")
    @Min(value = 1)
    @Max(value = 10)
    private Integer parallelismFactor;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }

    public double getDurationMultiplier() {
        return durationMultiplierX10 / 10.0;
    }
    public void setDurationMultiplierX10(Integer durationMultiplierX10) {
        this.durationMultiplierX10 = durationMultiplierX10;
    }

    public Integer getResourcesPerTask() { return resourcesPerTask; }
    public void setResourcesPerTask(Integer resourcesPerTask) { this.resourcesPerTask = resourcesPerTask; }

    public Integer getParallelismFactor() { return parallelismFactor; }
    public void setParallelismFactor(Integer parallelismFactor) { this.parallelismFactor = parallelismFactor; }
}
