package oll.business.repository;

import oll.business.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByInstanceId(Long instanceId);

    @Query("SELECT t FROM Task t JOIN FETCH t.taskDefinition td JOIN FETCH t.instance i JOIN FETCH i.model WHERE t.assignee.id = :assigneeId")
    List<Task> findByAssigneeIdWithDetails(@Param("assigneeId") Long assigneeId);

    @Query("SELECT t FROM Task t JOIN FETCH t.taskDefinition JOIN FETCH t.instance JOIN FETCH t.instance.model WHERE t.assignee.id = :assigneeId")
    List<Task> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    List<Task> findByStatus(Task.TaskStatus status);
}