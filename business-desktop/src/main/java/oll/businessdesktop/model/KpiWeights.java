package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KpiWeights(
    Long modelId,
    BigDecimal w1,
    BigDecimal w2,
    BigDecimal w3
) {}
