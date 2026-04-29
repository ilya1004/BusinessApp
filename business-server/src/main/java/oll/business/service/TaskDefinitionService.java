package oll.business.service;

import oll.business.model.ProcessModel;
import oll.business.model.TaskDefinition;
import oll.business.repository.ProcessModelRepository;
import oll.business.repository.TaskDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TaskDefinitionService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final ProcessModelRepository processModelRepository;

    public TaskDefinitionService(TaskDefinitionRepository taskDefinitionRepository,
                                 ProcessModelRepository processModelRepository) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.processModelRepository = processModelRepository;
    }

    public List<TaskDefinition> findAll() {
        return taskDefinitionRepository.findAll();
    }

    public TaskDefinition findById(Long id) {
        return taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TaskDefinition not found: " + id));
    }

    public List<TaskDefinition> findByModelId(Long modelId) {
        return taskDefinitionRepository.findByModelId(modelId);
    }

    @Transactional
    public TaskDefinition create(TaskDefinitionRequest request) {
        ProcessModel model = processModelRepository.findById(request.modelId())
                .orElseThrow(() -> new RuntimeException("ProcessModel not found: " + request.modelId()));

        TaskDefinition task = new TaskDefinition();
        task.setModel(model);
        task.setName(request.name());
        task.setDefaultDuration(request.defaultDuration());
        task.setExpectedCost(request.expectedCost());

        return taskDefinitionRepository.save(task);
    }

    @Transactional
    public TaskDefinition update(Long id, TaskDefinitionRequest request) {
        TaskDefinition task = findById(id);

        if (request.modelId() != null) {
            ProcessModel model = processModelRepository.findById(request.modelId())
                    .orElseThrow(() -> new RuntimeException("ProcessModel not found: " + request.modelId()));
            task.setModel(model);
        }
        if (request.name() != null) {
            task.setName(request.name());
        }
        if (request.defaultDuration() != null) {
            task.setDefaultDuration(request.defaultDuration());
        }
        if (request.expectedCost() != null) {
            task.setExpectedCost(request.expectedCost());
        }

        return taskDefinitionRepository.save(task);
    }

    @Transactional
    public void delete(Long id) {
        TaskDefinition task = findById(id);
        taskDefinitionRepository.delete(task);
    }

    public record TaskDefinitionRequest(
            Long modelId,
            String name,
            Integer defaultDuration,
            BigDecimal expectedCost
    ) {}
}