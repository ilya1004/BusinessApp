package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskDefinition(
    Long id,
    @JsonProperty("bpmnElementId") String bpmnElementId,
    String name,
    Integer defaultDuration,
    BigDecimal expectedCost,
    BigDecimal kpiWeight
) {
    public TaskDefinition(String bpmnElementId, String name) {
        this(null, bpmnElementId, name, 60, BigDecimal.ONE, BigDecimal.ONE);
    }

    public BigDecimal getKpiWeight() {
        return kpiWeight != null ? kpiWeight : BigDecimal.ONE;
    }
}
