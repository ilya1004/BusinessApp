package oll.business.repository;

import oll.business.model.Role;
import oll.business.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByDepartmentId(Long departmentId);
    List<User> findByRole(Role role);
}