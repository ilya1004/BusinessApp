package oll.business.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
    @Index(name = "idx_instances_model_status_dates", columnList = "model_id, status, startedAt, finishedAt")
})
public class ProcessInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ProcessModel model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus status;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column(length = 100)
    private String currentState;

    @Column(length = 255)
    private String name;

    public ProcessInstance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessModel getModel() { return model; }
    public void setModel(ProcessModel model) { this.model = model; }
    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public enum ProcessStatus {
        PENDING, RUNNING, COMPLETED, CANCELLED, FAILED
    }
}