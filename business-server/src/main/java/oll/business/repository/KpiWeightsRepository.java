package oll.business.repository;

import oll.business.model.KpiWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KpiWeightsRepository extends JpaRepository<KpiWeights, Long> {
    Optional<KpiWeights> findByModelId(Long modelId);
    Optional<KpiWeights> findByModelIdIsNull();
    List<KpiWeights> findAllByModelIdIsNull();
}
