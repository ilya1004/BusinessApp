package oll.business.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
    @Index(name = "idx_tasks_status_dates", columnList = "status, startedAt, completedAt")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ProcessInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private TaskDefinition taskDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    // in minutes
    @Column(nullable = false)
    private Integer plannedDuration;

    // in minutes
    @Column
    private Integer actualDuration;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime dueDate;

    public Task() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessInstance getInstance() { return instance; }
    public void setInstance(ProcessInstance instance) { this.instance = instance; }
    public TaskDefinition getTaskDefinition() { return taskDefinition; }
    public void setTaskDefinition(TaskDefinition taskDefinition) { this.taskDefinition = taskDefinition; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getPlannedDuration() { return plannedDuration; }
    public void setPlannedDuration(Integer plannedDuration) { this.plannedDuration = plannedDuration; }
    public Integer getActualDuration() { return actualDuration; }
    public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public enum TaskStatus {
        PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED
    }
}