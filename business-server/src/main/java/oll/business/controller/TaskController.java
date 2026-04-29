package oll.business.controller;

import oll.business.model.Task;
import oll.business.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public Task findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @GetMapping("/instance/{instanceId}")
    public List<Task> findByInstanceId(@PathVariable Long instanceId) {
        return taskService.findByInstanceId(instanceId);
    }

    @GetMapping("/assignee/{assigneeId}")
    public List<Task> findByAssigneeId(@PathVariable Long assigneeId) {
        return taskService.findByAssigneeId(assigneeId);
    }

    @GetMapping("/status/{status}")
    public List<Task> findByStatus(@PathVariable Task.TaskStatus status) {
        return taskService.findByStatus(status);
    }

    @PostMapping
    public Task create(@RequestBody TaskService.TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody TaskService.TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public Task assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        return taskService.assign(id, assigneeId);
    }

    @PostMapping("/{id}/start")
    public Task start(@PathVariable Long id) {
        return taskService.start(id);
    }

    @PostMapping("/{id}/complete")
    public Task complete(@PathVariable Long id) {
        return taskService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public Task cancel(@PathVariable Long id) {
        return taskService.cancel(id);
    }
}