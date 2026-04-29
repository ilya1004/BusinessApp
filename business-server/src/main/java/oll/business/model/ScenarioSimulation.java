package oll.business.model;

import jakarta.persistence.*;

@Entity
public class ScenarioSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ProcessModel model;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String parametersJson;

    @Column(columnDefinition = "TEXT")
    private String predictedKpi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User createdBy;

    public ScenarioSimulation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessModel getModel() { return model; }
    public void setModel(ProcessModel model) { this.model = model; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public String getPredictedKpi() { return predictedKpi; }
    public void setPredictedKpi(String predictedKpi) { this.predictedKpi = predictedKpi; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}