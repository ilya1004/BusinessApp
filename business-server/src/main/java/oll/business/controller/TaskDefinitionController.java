package oll.business.controller;

import oll.business.model.TaskDefinition;
import oll.business.service.TaskDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-definitions")
public class TaskDefinitionController {

    private final TaskDefinitionService taskDefinitionService;

    public TaskDefinitionController(TaskDefinitionService taskDefinitionService) {
        this.taskDefinitionService = taskDefinitionService;
    }

    @GetMapping
    public List<TaskDefinition> findAll() {
        return taskDefinitionService.findAll();
    }

    @GetMapping("/{id}")
    public TaskDefinition findById(@PathVariable Long id) {
        return taskDefinitionService.findById(id);
    }

    @GetMapping("/model/{modelId}")
    public List<TaskDefinition> findByModelId(@PathVariable Long modelId) {
        return taskDefinitionService.findByModelId(modelId);
    }

    @PostMapping
    public TaskDefinition create(@RequestBody TaskDefinitionService.TaskDefinitionRequest request) {
        return taskDefinitionService.create(request);
    }

    @PutMapping("/{id}")
    public TaskDefinition update(@PathVariable Long id,
                                 @RequestBody TaskDefinitionService.TaskDefinitionRequest request) {
        return taskDefinitionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskDefinitionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}