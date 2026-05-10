package oll.business.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProcessModelRequest {
    private String name;
    private String bpmnXml;
    private Long authorId;
    private List<TaskDefinitionRequest> taskDefinitions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public List<TaskDefinitionRequest> getTaskDefinitions() { return taskDefinitions; }
    public void setTaskDefinitions(List<TaskDefinitionRequest> taskDefinitions) { this.taskDefinitions = taskDefinitions; }

    public static class TaskDefinitionRequest {
        private Long id;
        private String bpmnElementId;
        private String name;
        private Integer defaultDuration;
        private BigDecimal expectedCost;
        private BigDecimal kpiWeight;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBpmnElementId() { return bpmnElementId; }
        public void setBpmnElementId(String bpmnElementId) { this.bpmnElementId = bpmnElementId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDefaultDuration() { return defaultDuration; }
        public void setDefaultDuration(Integer defaultDuration) { this.defaultDuration = defaultDuration; }
        public BigDecimal getExpectedCost() { return expectedCost; }
        public void setExpectedCost(BigDecimal expectedCost) { this.expectedCost = expectedCost; }
        public BigDecimal getKpiWeight() { return kpiWeight; }
        public void setKpiWeight(BigDecimal kpiWeight) { this.kpiWeight = kpiWeight; }
    }
}
