package oll.business.service;

import oll.business.model.ProcessInstance;
import oll.business.model.Task;
import oll.business.model.TaskDefinition;
import oll.business.model.User;
import oll.business.repository.TaskDefinitionRepository;
import oll.business.repository.TaskRepository;
import oll.business.repository.UserRepository;
import oll.business.repository.ProcessInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    public TaskService(TaskRepository taskRepository,
                    ProcessInstanceRepository processInstanceRepository,
                    TaskDefinitionRepository taskDefinitionRepository,
                    UserRepository userRepository,
                    LogService logService) {
        this.taskRepository = taskRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.userRepository = userRepository;
        this.logService = logService;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
    }

    public List<Task> findByInstanceId(Long instanceId) {
        return taskRepository.findByInstanceId(instanceId);
    }

    public List<Task> findByAssigneeId(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId);
    }

    public List<Task> findByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    @Transactional
    public Task create(TaskRequest request) {
        ProcessInstance instance = processInstanceRepository.findById(request.instanceId())
                .orElseThrow(() -> new RuntimeException("ProcessInstance not found: " + request.instanceId()));

        oll.business.model.TaskDefinition taskDef = taskDefinitionRepository.findById(request.taskDefinitionId())
                .orElseThrow(() -> new RuntimeException("TaskDefinition not found: " + request.taskDefinitionId()));

        Task task = new Task();
        task.setInstance(instance);
        task.setTaskDefinition(taskDef);
        task.setStatus(Task.TaskStatus.PENDING);
        task.setPlannedDuration(taskDef.getDefaultDuration());

        Task saved = taskRepository.save(task);
        logService.logInfo("Task created: id=" + saved.getId() + " for instance=" + request.instanceId(), "TaskService", "create");
        return saved;
    }

    @Transactional
    public Task update(Long id, TaskRequest request) {
        Task task = findById(id);

        if (request.instanceId() != null) {
            ProcessInstance instance = processInstanceRepository.findById(request.instanceId())
                    .orElseThrow(() -> new RuntimeException("ProcessInstance not found: " + request.instanceId()));
            task.setInstance(instance);
        }
        if (request.taskDefinitionId() != null) {
            oll.business.model.TaskDefinition taskDef = taskDefinitionRepository.findById(request.taskDefinitionId())
                    .orElseThrow(() -> new RuntimeException("TaskDefinition not found: " + request.taskDefinitionId()));
            task.setTaskDefinition(taskDef);
        }
        if (request.plannedDuration() != null) {
            task.setPlannedDuration(request.plannedDuration());
        }

        Task saved = taskRepository.save(task);
        logService.logInfo("Task updated: id=" + id, "TaskService", "update");
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Task task = findById(id);
        taskRepository.delete(task);
        logService.logInfo("Task deleted: id=" + id, "TaskService", "delete");
    }

    @Transactional
    public Task assign(Long id, Long assigneeId) {
        Task task = findById(id);

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new RuntimeException("User not found: " + assigneeId));

        task.setAssignee(assignee);

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " assigned to user id=" + assigneeId, "TaskService", "assign");
        return saved;
    }

    @Transactional
    public Task unassign(Long id) {
        Task task = findById(id);

        task.setAssignee(null);
        task.setStatus(Task.TaskStatus.PENDING);

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " unassigned", "TaskService", "unassign");
        return saved;
    }

    @Transactional
    public Task start(Long id) {
        Task task = findById(id);

        if (task.getStatus() != Task.TaskStatus.PENDING && task.getStatus() != Task.TaskStatus.ASSIGNED) {
            logService.logWarn("Cannot start task in status: " + task.getStatus(), "TaskService", "start");
            throw new RuntimeException("Cannot start task in status: " + task.getStatus());
        }

        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " started", "TaskService", "start");
        return saved;
    }

    @Transactional
    public Task complete(Long id) {
        Task task = findById(id);

        if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.CANCELLED) {
            logService.logWarn("Cannot complete task in status: " + task.getStatus(), "TaskService", "complete");
            throw new RuntimeException("Cannot complete task in status: " + task.getStatus());
        }

        LocalDateTime completedAt = LocalDateTime.now();
        task.setCompletedAt(completedAt);
        task.setStatus(Task.TaskStatus.COMPLETED);

        if (task.getStartedAt() != null) {
            long durationMinutes = Duration.between(task.getStartedAt(), completedAt).toMinutes();
            task.setActualDuration((int) durationMinutes);
        }

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " completed", "TaskService", "complete");
        return saved;
    }

    @Transactional
    public Task cancel(Long id) {
        Task task = findById(id);

        if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.CANCELLED) {
            logService.logWarn("Cannot cancel task in status: " + task.getStatus(), "TaskService", "cancel");
            throw new RuntimeException("Cannot cancel task in status: " + task.getStatus());
        }

        task.setStatus(Task.TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " cancelled", "TaskService", "cancel");
        return saved;
    }

    @Transactional
    public Task logTime(Long id, Integer actualDurationMinutes) {
        Task task = findById(id);

        if (actualDurationMinutes == null || actualDurationMinutes < 0) {
            throw new RuntimeException("Invalid duration: " + actualDurationMinutes);
        }

        task.setActualDuration(actualDurationMinutes);

        if (task.getStatus() == Task.TaskStatus.PENDING) {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            task.setStartedAt(LocalDateTime.now());
        }

        Task saved = taskRepository.save(task);
        logService.logInfo("Task id=" + id + " time logged: " + actualDurationMinutes + " min", "TaskService", "logTime");
        return saved;
    }

    public record TaskRequest(
            Long instanceId,
            Long taskDefinitionId,
            Integer plannedDuration
    ) {}
}