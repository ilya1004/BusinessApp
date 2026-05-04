package oll.business.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kpi_weights")
public class KpiWeights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private ProcessModel model;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal w1;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal w2;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal w3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public KpiWeights() {}

    public static KpiWeights defaultWeights() {
        KpiWeights w = new KpiWeights();
        w.w1 = new BigDecimal("0.34");
        w.w2 = new BigDecimal("0.33");
        w.w3 = new BigDecimal("0.33");
        w.updatedAt = LocalDateTime.now();
        return w;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessModel getModel() { return model; }
    public void setModel(ProcessModel model) { this.model = model; }
    public BigDecimal getW1() { return w1; }
    public void setW1(BigDecimal w1) { this.w1 = w1; }
    public BigDecimal getW2() { return w2; }
    public void setW2(BigDecimal w2) { this.w2 = w2; }
    public BigDecimal getW3() { return w3; }
    public void setW3(BigDecimal w3) { this.w3 = w3; }
    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
