package oll.business.controller;

import oll.business.dto.CreateProcessInstanceRequest;
import oll.business.model.ProcessInstance;
import oll.business.model.ProcessInstance.ProcessStatus;
import oll.business.model.Task;
import oll.business.model.TaskDefinition;
import oll.business.model.Task.TaskStatus;
import oll.business.repository.ProcessInstanceRepository;
import oll.business.repository.ProcessModelRepository;
import oll.business.repository.TaskDefinitionRepository;
import oll.business.repository.TaskRepository;
import oll.business.service.KpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/process-instances")
@CrossOrigin(origins = "*")
public class ProcessInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceController.class);

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final ProcessModelRepository processModelRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final KpiService kpiService;

    public ProcessInstanceController(ProcessInstanceRepository processInstanceRepository,
                                     TaskRepository taskRepository,
                                     ProcessModelRepository processModelRepository,
                                     TaskDefinitionRepository taskDefinitionRepository,
                                     KpiService kpiService) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.processModelRepository = processModelRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.kpiService = kpiService;
    }

    @GetMapping
    public List<ProcessInstance> list() {
        return processInstanceRepository.findAll();
    }

    @GetMapping("/{id}")
    public ProcessInstance getById(@PathVariable Long id) {
        return processInstanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessInstance not found: " + id));
    }

    @PostMapping
    @Transactional
    public ProcessInstance create(@RequestBody CreateProcessInstanceRequest request) {
        logger.info("Creating ProcessInstance for model id={}", request.getProcessModelId());

        var model = processModelRepository.findById(request.getProcessModelId())
                .orElseThrow(() -> new RuntimeException("ProcessModel not found: " + request.getProcessModelId()));

        ProcessInstance instance = new ProcessInstance();
        instance.setModel(model);
        instance.setStatus(ProcessStatus.RUNNING);
        instance.setStartedAt(LocalDateTime.now());
        instance.setCurrentState("Started");

        ProcessInstance saved = processInstanceRepository.save(instance);
        logger.info("Created ProcessInstance id={}", saved.getId());

        List<TaskDefinition> definitions = taskDefinitionRepository.findByModelId(model.getId());
        for (TaskDefinition def : definitions) {
            Task task = new Task();
            task.setInstance(saved);
            task.setTaskDefinition(def);
            task.setStatus(TaskStatus.PENDING);
            task.setPlannedDuration(def.getDefaultDuration());
            taskRepository.save(task);
        }

        logger.info("Created {} tasks for instance id={}", definitions.size(), saved.getId());

        return processInstanceRepository.findById(saved.getId()).orElse(saved);
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ProcessInstance updateStatus(@PathVariable Long id, @RequestParam String status) {
        ProcessInstance instance = processInstanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessInstance not found: " + id));
        instance.setStatus(ProcessStatus.valueOf(status.toUpperCase()));
        if (status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("cancelled")) {
            instance.setFinishedAt(LocalDateTime.now());
        }
        return processInstanceRepository.save(instance);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        taskRepository.findByInstanceId(id).forEach(taskRepository::delete);
        processInstanceRepository.deleteById(id);
    }

    @GetMapping("/{id}/tasks")
    public List<Task> getTasks(@PathVariable Long id) {
        ProcessInstance instance = processInstanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessInstance not found: " + id));
        return taskRepository.findByInstanceId(id);
    }

    @PutMapping("/tasks/{taskId}/status")
    @Transactional
    public Task updateTaskStatus(@PathVariable Long taskId, @RequestParam String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            throw new RuntimeException("Cannot change status of a completed task");
        }

        if (status.equalsIgnoreCase("completed")) {
            throw new RuntimeException("Use the complete endpoint to mark task as completed");
        }

        task.setStatus(Task.TaskStatus.valueOf(status.toUpperCase()));
        return taskRepository.save(task);
    }
}
