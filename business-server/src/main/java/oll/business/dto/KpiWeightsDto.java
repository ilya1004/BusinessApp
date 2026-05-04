package oll.business.dto;

import java.math.BigDecimal;

public class KpiWeightsDto {
    private Long modelId;
    private BigDecimal w1;
    private BigDecimal w2;
    private BigDecimal w3;

    public KpiWeightsDto() {}

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public BigDecimal getW1() { return w1; }
    public void setW1(BigDecimal w1) { this.w1 = w1; }
    public BigDecimal getW2() { return w2; }
    public void setW2(BigDecimal w2) { this.w2 = w2; }
    public BigDecimal getW3() { return w3; }
    public void setW3(BigDecimal w3) { this.w3 = w3; }

    public BigDecimal getSum() {
        BigDecimal s = BigDecimal.ZERO;
        if (w1 != null) s = s.add(w1);
        if (w2 != null) s = s.add(w2);
        if (w3 != null) s = s.add(w3);
        return s;
    }
}
