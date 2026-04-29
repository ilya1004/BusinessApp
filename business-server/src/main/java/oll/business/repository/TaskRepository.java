package oll.business.repository;

import oll.business.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByInstanceId(Long instanceId);
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByStatus(Task.TaskStatus status);
}