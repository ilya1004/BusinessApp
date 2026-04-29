package oll.business.repository;

import oll.business.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Long> {
    List<TaskDefinition> findByModelId(Long modelId);
}