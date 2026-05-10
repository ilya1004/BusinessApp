package oll.business.controller;

import oll.business.dto.ProcessModelRequest;
import oll.business.model.ProcessModel;
import oll.business.model.TaskDefinition;
import oll.business.model.User;
import oll.business.repository.ProcessModelRepository;
import oll.business.repository.TaskDefinitionRepository;
import oll.business.repository.UserRepository;
import oll.business.service.LogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/process-models")
public class ProcessModelController {

    private final ProcessModelRepository processModelRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    public ProcessModelController(ProcessModelRepository processModelRepository,
                                  TaskDefinitionRepository taskDefinitionRepository,
                                  UserRepository userRepository,
                                  LogService logService) {
        this.processModelRepository = processModelRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.userRepository = userRepository;
        this.logService = logService;
    }

    @GetMapping
    public List<ProcessModel> list() {
        return processModelRepository.findAll();
    }

    @GetMapping("/find-by-name")
    public ProcessModel findByName(@RequestParam String name) {
        ProcessModel model = processModelRepository.findByName(name).orElse(null);
        if (model != null) {
            model.getTaskDefinitions().size();
        }
        return model;
    }

    @GetMapping("/{id:\\d+}")
    public ProcessModel getById(@PathVariable Long id) {
        return processModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessModel not found: " + id));
    }

    @PostMapping
    @Transactional
    public ProcessModel create(@RequestBody ProcessModelRequest request) {
        logService.logInfo("Creating process model: " + request.getName(), "ProcessModelController", "create", request.getAuthorId(),
                "taskDefinitions=" + (request.getTaskDefinitions() != null ? request.getTaskDefinitions().size() : 0));

        ProcessModel model = new ProcessModel();
        model.setName(request.getName());
        model.setBpmnXml(request.getBpmnXml());
        model.setVersion(1);
        model.setCreatedAt(java.time.LocalDateTime.now());

        if (request.getAuthorId() != null) {
            User author = userRepository.findById(request.getAuthorId()).orElse(null);
            model.setAuthor(author);
        }

        ProcessModel saved = processModelRepository.save(model);
        syncTaskDefinitions(saved, request.getTaskDefinitions());
        ProcessModel result = processModelRepository.findById(saved.getId()).orElse(saved);
        logService.logInfo("Process model created: " + result.getName() + " (id=" + result.getId() + ")", "ProcessModelController", "create");
        return result;
    }

    @PutMapping("/{id:\\d+}")
    @Transactional
    public ProcessModel update(@PathVariable Long id, @RequestBody ProcessModelRequest request) {
        ProcessModel existing = processModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessModel not found: " + id));
        existing.setName(request.getName());
        existing.setBpmnXml(request.getBpmnXml());
        existing.setVersion(existing.getVersion() + 1);

        if (request.getAuthorId() != null && (existing.getAuthor() == null || !existing.getAuthor().getId().equals(request.getAuthorId()))) {
            User author = userRepository.findById(request.getAuthorId()).orElse(null);
            existing.setAuthor(author);
        }

        ProcessModel saved = processModelRepository.save(existing);
        syncTaskDefinitions(saved, request.getTaskDefinitions());
        logService.logInfo("Process model updated: " + saved.getName() + " (id=" + saved.getId() + ", version=" + saved.getVersion() + ")", "ProcessModelController", "update");
        return saved;
    }

    @DeleteMapping("/{id:\\d+}")
    @Transactional
    public void delete(@PathVariable Long id) {
        ProcessModel model = processModelRepository.findById(id).orElse(null);
        String name = model != null ? model.getName() : String.valueOf(id);
        taskDefinitionRepository.deleteByModelId(id);
        processModelRepository.deleteById(id);
        logService.logInfo("Process model deleted: " + name + " (id=" + id + ")", "ProcessModelController", "delete");
    }

    @Transactional
    void syncTaskDefinitions(ProcessModel model, List<ProcessModelRequest.TaskDefinitionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Map<String, ProcessModelRequest.TaskDefinitionRequest> requestMap = new HashMap<>();
        for (ProcessModelRequest.TaskDefinitionRequest req : requests) {
            if (req.getBpmnElementId() != null) {
                requestMap.put(req.getBpmnElementId(), req);
            }
        }

        List<TaskDefinition> existing = taskDefinitionRepository.findByModelId(model.getId());
        Map<String, TaskDefinition> existingMap = new HashMap<>();
        for (TaskDefinition td : existing) {
            existingMap.put(td.getBpmnElementId(), td);
        }

        for (Map.Entry<String, ProcessModelRequest.TaskDefinitionRequest> entry : requestMap.entrySet()) {
            String elementId = entry.getKey();
            ProcessModelRequest.TaskDefinitionRequest req = entry.getValue();
            TaskDefinition td = existingMap.get(elementId);

            if (td != null) {
                td.setName(req.getName());
                td.setDefaultDuration(req.getDefaultDuration() != null ? req.getDefaultDuration() : 0);
                td.setExpectedCost(req.getExpectedCost() != null ? req.getExpectedCost() : BigDecimal.ZERO);
                td.setKpiWeight(req.getKpiWeight());
                taskDefinitionRepository.save(td);
                logService.logInfo("Updated TaskDefinition id=" + td.getId() + " for element " + elementId, "ProcessModelController", "syncTaskDefinitions");
            } else {
                td = new TaskDefinition();
                td.setModel(model);
                td.setBpmnElementId(elementId);
                td.setName(req.getName());
                td.setDefaultDuration(req.getDefaultDuration() != null ? req.getDefaultDuration() : 0);
                td.setExpectedCost(req.getExpectedCost() != null ? req.getExpectedCost() : BigDecimal.ZERO);
                td.setKpiWeight(req.getKpiWeight());
                taskDefinitionRepository.save(td);
                logService.logInfo("Created TaskDefinition for element " + elementId, "ProcessModelController", "syncTaskDefinitions");
            }
        }

        for (TaskDefinition td : existing) {
            if (!requestMap.containsKey(td.getBpmnElementId())) {
                taskDefinitionRepository.delete(td);
                logService.logInfo("Deleted TaskDefinition for removed element " + td.getBpmnElementId(), "ProcessModelController", "syncTaskDefinitions");
            }
        }
    }
}
