package oll.business.repository;

import oll.business.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Long> {
    List<TaskDefinition> findByModelId(Long modelId);
    Optional<TaskDefinition> findByModelIdAndBpmnElementId(Long modelId, String bpmnElementId);
    void deleteByModelId(Long modelId);
}