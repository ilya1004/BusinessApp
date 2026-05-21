package oll.business.service;

import oll.business.model.*;
import oll.business.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProcessInstanceRepository processInstanceRepository;
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private UserRepository userRepository;
    @Mock private LogService logService;

    @Captor private ArgumentCaptor<Task> taskCaptor;

    private TaskService taskService;

    private ProcessInstance instance;
    private TaskDefinition taskDef;
    private User assignee;
    private Task pendingTask;
    private Task inProgressTask;
    private Task completedTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, processInstanceRepository,
                taskDefinitionRepository, userRepository, logService);

        assignee = new User("worker", "hash", Role.EXECUTOR, "Worker", "One");
        assignee.setId(50L);

        instance = new ProcessInstance();
        instance.setId(100L);
        instance.setStatus(ProcessInstance.ProcessStatus.RUNNING);

        taskDef = new TaskDefinition();
        taskDef.setId(200L);
        taskDef.setName("Review");
        taskDef.setDefaultDuration(120);
        taskDef.setExpectedCost(BigDecimal.valueOf(500));
        taskDef.setKpiWeight(BigDecimal.valueOf(1.0));

        pendingTask = new Task();
        pendingTask.setId(1L);
        pendingTask.setInstance(instance);
        pendingTask.setTaskDefinition(taskDef);
        pendingTask.setStatus(Task.TaskStatus.PENDING);
        pendingTask.setPlannedDuration(120);

        inProgressTask = new Task();
        inProgressTask.setId(2L);
        inProgressTask.setInstance(instance);
        inProgressTask.setTaskDefinition(taskDef);
        inProgressTask.setStatus(Task.TaskStatus.IN_PROGRESS);
        inProgressTask.setPlannedDuration(120);
        inProgressTask.setStartedAt(LocalDateTime.now().minusHours(2));

        completedTask = new Task();
        completedTask.setId(3L);
        completedTask.setInstance(instance);
        completedTask.setTaskDefinition(taskDef);
        completedTask.setStatus(Task.TaskStatus.COMPLETED);
        completedTask.setPlannedDuration(120);
        completedTask.setActualDuration(130);
        completedTask.setStartedAt(LocalDateTime.now().minusHours(3));
        completedTask.setCompletedAt(LocalDateTime.now());
    }

    @Test
    void findAll_shouldReturnAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(pendingTask, inProgressTask));

        List<Task> result = taskService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findById_shouldReturnTask_whenFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));

        Task result = taskService.findById(1L);

        assertEquals(Task.TaskStatus.PENDING, result.getStatus());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.findById(99L));
        assertEquals("Task not found: 99", ex.getMessage());
    }

    @Test
    void findByInstanceId_shouldReturnTasks() {
        when(taskRepository.findByInstanceId(100L)).thenReturn(List.of(pendingTask));

        List<Task> result = taskService.findByInstanceId(100L);

        assertEquals(1, result.size());
    }

    @Test
    void findByAssigneeId_shouldReturnTasks() {
        when(taskRepository.findByAssigneeId(50L)).thenReturn(List.of(inProgressTask));

        List<Task> result = taskService.findByAssigneeId(50L);

        assertEquals(1, result.size());
    }

    @Test
    void findByStatus_shouldReturnTasks() {
        when(taskRepository.findByStatus(Task.TaskStatus.PENDING)).thenReturn(List.of(pendingTask));

        List<Task> result = taskService.findByStatus(Task.TaskStatus.PENDING);

        assertEquals(1, result.size());
    }

    @Test
    void create_shouldBuildTaskWithPendingStatusAndPlannedDuration() {
        TaskService.TaskRequest request = new TaskService.TaskRequest(100L, 200L, null);

        when(processInstanceRepository.findById(100L)).thenReturn(Optional.of(instance));
        when(taskDefinitionRepository.findById(200L)).thenReturn(Optional.of(taskDef));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        Task result = taskService.create(request);

        assertEquals(Task.TaskStatus.PENDING, result.getStatus());
        assertEquals(120, result.getPlannedDuration());
        assertEquals(instance, result.getInstance());
        assertEquals(taskDef, result.getTaskDefinition());
        verify(logService).logInfo("Task created: id=99 for instance=100", "TaskService", "create");
    }

    @Test
    void create_shouldThrow_whenInstanceNotFound() {
        TaskService.TaskRequest request = new TaskService.TaskRequest(999L, 200L, null);

        when(processInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.create(request));
        assertEquals("ProcessInstance not found: 999", ex.getMessage());
    }

    @Test
    void create_shouldThrow_whenTaskDefinitionNotFound() {
        TaskService.TaskRequest request = new TaskService.TaskRequest(100L, 999L, null);

        when(processInstanceRepository.findById(100L)).thenReturn(Optional.of(instance));
        when(taskDefinitionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.create(request));
        assertEquals("TaskDefinition not found: 999", ex.getMessage());
    }

    @Test
    void update_shouldUpdateProvidedFields() {
        TaskDefinition otherDef = new TaskDefinition();
        otherDef.setId(201L);

        ProcessInstance otherInstance = new ProcessInstance();
        otherInstance.setId(101L);

        TaskService.TaskRequest request = new TaskService.TaskRequest(101L, 201L, 60);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(processInstanceRepository.findById(101L)).thenReturn(Optional.of(otherInstance));
        when(taskDefinitionRepository.findById(201L)).thenReturn(Optional.of(otherDef));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.update(1L, request);

        assertEquals(101L, result.getInstance().getId());
        assertEquals(201L, result.getTaskDefinition().getId());
        assertEquals(60, result.getPlannedDuration());
    }

    @Test
    void update_shouldIgnoreNullFields() {
        TaskService.TaskRequest request = new TaskService.TaskRequest(null, null, null);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.update(1L, request);

        assertEquals(100L, result.getInstance().getId());
        assertEquals(200L, result.getTaskDefinition().getId());
        assertEquals(120, result.getPlannedDuration());
    }

    @Test
    void delete_shouldRemoveTaskAndLog() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));

        taskService.delete(1L);

        verify(taskRepository).delete(pendingTask);
        verify(logService).logInfo("Task deleted: id=1", "TaskService", "delete");
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.delete(99L));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void assign_shouldSetAssignee() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(50L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.assign(1L, 50L);

        assertEquals(50L, result.getAssignee().getId());
        verify(logService).logInfo("Task id=1 assigned to user id=50", "TaskService", "assign");
    }

    @Test
    void assign_shouldThrow_whenUserNotFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.assign(1L, 99L));
        assertEquals("User not found: 99", ex.getMessage());
    }

    @Test
    void unassign_shouldClearAssigneeAndResetStatusToPending() {
        Task assigned = new Task();
        assigned.setId(5L);
        assigned.setInstance(instance);
        assigned.setTaskDefinition(taskDef);
        assigned.setStatus(Task.TaskStatus.ASSIGNED);
        assigned.setAssignee(assignee);
        assigned.setPlannedDuration(120);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(assigned));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.unassign(5L);

        assertNull(result.getAssignee());
        assertEquals(Task.TaskStatus.PENDING, result.getStatus());
        verify(logService).logInfo("Task id=5 unassigned", "TaskService", "unassign");
    }

    @Test
    void start_shouldSetInProgressAndStartedAt_whenPending() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.start(1L);

        assertEquals(Task.TaskStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(logService).logInfo("Task id=1 started", "TaskService", "start");
    }

    @Test
    void start_shouldSucceed_whenAssigned() {
        Task assigned = new Task();
        assigned.setId(4L);
        assigned.setStatus(Task.TaskStatus.ASSIGNED);
        assigned.setPlannedDuration(120);

        when(taskRepository.findById(4L)).thenReturn(Optional.of(assigned));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.start(4L);

        assertEquals(Task.TaskStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void start_shouldThrow_whenNotPendingOrAssigned() {
        when(taskRepository.findById(3L)).thenReturn(Optional.of(completedTask));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.start(3L));
        assertEquals("Cannot start task in status: COMPLETED", ex.getMessage());
        verify(logService).logWarn("Cannot start task in status: COMPLETED", "TaskService", "start");
    }

    @Test
    void complete_shouldSetCompletedAndCalculateActualDuration() {
        Task started = new Task();
        started.setId(6L);
        started.setStatus(Task.TaskStatus.IN_PROGRESS);
        started.setStartedAt(LocalDateTime.now().minusMinutes(45));
        started.setPlannedDuration(120);

        when(taskRepository.findById(6L)).thenReturn(Optional.of(started));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.complete(6L);

        assertEquals(Task.TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertNotNull(result.getActualDuration());
        assertTrue(result.getActualDuration() >= 44 && result.getActualDuration() <= 46);
        verify(logService).logInfo("Task id=6 completed", "TaskService", "complete");
    }

    @Test
    void complete_shouldNotCalculateDuration_whenNoStartedAt() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.complete(1L);

        assertEquals(Task.TaskStatus.COMPLETED, result.getStatus());
        assertNull(result.getActualDuration());
    }

    @Test
    void complete_shouldThrow_whenAlreadyCompleted() {
        when(taskRepository.findById(3L)).thenReturn(Optional.of(completedTask));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.complete(3L));
        assertEquals("Cannot complete task in status: COMPLETED", ex.getMessage());
    }

    @Test
    void complete_shouldThrow_whenCancelled() {
        Task cancelled = new Task();
        cancelled.setId(7L);
        cancelled.setStatus(Task.TaskStatus.CANCELLED);

        when(taskRepository.findById(7L)).thenReturn(Optional.of(cancelled));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.complete(7L));
        assertEquals("Cannot complete task in status: CANCELLED", ex.getMessage());
    }

    @Test
    void cancel_shouldSetCancelled() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.cancel(1L);

        assertEquals(Task.TaskStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(logService).logInfo("Task id=1 cancelled", "TaskService", "cancel");
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCompleted() {
        when(taskRepository.findById(3L)).thenReturn(Optional.of(completedTask));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.cancel(3L));
        assertEquals("Cannot cancel task in status: COMPLETED", ex.getMessage());
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCancelled() {
        Task cancelled = new Task();
        cancelled.setId(7L);
        cancelled.setStatus(Task.TaskStatus.CANCELLED);

        when(taskRepository.findById(7L)).thenReturn(Optional.of(cancelled));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.cancel(7L));
        assertEquals("Cannot cancel task in status: CANCELLED", ex.getMessage());
    }

    @Test
    void logTime_shouldSetActualDuration() {
        when(taskRepository.findById(2L)).thenReturn(Optional.of(inProgressTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.logTime(2L, 90);

        assertEquals(90, result.getActualDuration());
        verify(logService).logInfo("Task id=2 time logged: 90 min", "TaskService", "logTime");
    }

    @Test
    void logTime_shouldTransitionFromPendingToInProgress() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.logTime(1L, 60);

        assertEquals(60, result.getActualDuration());
        assertEquals(Task.TaskStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedAt());
    }

    @Test
    void logTime_shouldNotChangeStatus_whenAlreadyInProgress() {
        when(taskRepository.findById(2L)).thenReturn(Optional.of(inProgressTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.logTime(2L, 75);

        assertEquals(Task.TaskStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void logTime_shouldThrow_whenDurationNull() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.logTime(1L, null));
        assertEquals("Invalid duration: null", ex.getMessage());
    }

    @Test
    void logTime_shouldThrow_whenDurationNegative() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.logTime(1L, -5));
        assertEquals("Invalid duration: -5", ex.getMessage());
    }
}
