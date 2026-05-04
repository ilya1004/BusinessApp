package oll.business.dto;

public class CreateProcessInstanceRequest {
    private Long processModelId;
    private String instanceName;

    public Long getProcessModelId() { return processModelId; }
    public void setProcessModelId(Long processModelId) { this.processModelId = processModelId; }
    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
}
