package oll.business.dto;

public class KpiPair {
    private Double baseline;
    private Double scenario;

    public KpiPair() {}
    public KpiPair(Double baseline, Double scenario) {
        this.baseline = baseline;
        this.scenario = scenario;
    }

    public Double getBaseline() { return baseline; }
    public void setBaseline(Double baseline) { this.baseline = baseline; }
    public Double getScenario() { return scenario; }
    public void setScenario(Double scenario) { this.scenario = scenario; }
}
