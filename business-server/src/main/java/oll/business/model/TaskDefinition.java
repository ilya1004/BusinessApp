package oll.business.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class TaskDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ProcessModel model;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer defaultDuration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedCost;

    public TaskDefinition() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessModel getModel() { return model; }
    public void setModel(ProcessModel model) { this.model = model; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDefaultDuration() { return defaultDuration; }
    public void setDefaultDuration(Integer defaultDuration) { this.defaultDuration = defaultDuration; }
    public BigDecimal getExpectedCost() { return expectedCost; }
    public void setExpectedCost(BigDecimal expectedCost) { this.expectedCost = expectedCost; }
}