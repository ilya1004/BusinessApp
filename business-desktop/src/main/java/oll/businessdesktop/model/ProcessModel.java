package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessModel(
    Long id,
    String name,
    @JsonProperty("bpmnXml") String bpmnXml,
    Integer version,
    Long authorId,
    List<TaskDefinition> taskDefinitions
) {
    public ProcessModel(Long id, String name, String bpmnXml, Integer version) {
        this(id, name, bpmnXml, version, null, null);
    }
}
