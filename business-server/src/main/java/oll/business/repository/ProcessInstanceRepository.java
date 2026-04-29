package oll.business.repository;

import oll.business.model.ProcessInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, Long> {
    List<ProcessInstance> findByModelId(Long modelId);
    List<ProcessInstance> findByStatus(ProcessInstance.ProcessStatus status);
}