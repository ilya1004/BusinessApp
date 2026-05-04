package oll.business.repository;

import oll.business.model.ProcessModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProcessModelRepository extends JpaRepository<ProcessModel, Long> {
    List<ProcessModel> findByAuthorId(Long authorId);
    Optional<ProcessModel> findByName(String name);
}