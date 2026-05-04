package oll.business.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class TaskDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @JsonIgnore
    private ProcessModel model;

    @Column(nullable = false, length = 200)
    private String bpmnElementId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer defaultDuration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedCost;

    @Column(precision = 3, scale = 2)
    private BigDecimal kpiWeight;

    public TaskDefinition() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessModel getModel() { return model; }
    public void setModel(ProcessModel model) { this.model = model; }
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