package oll.business.service;

import oll.business.model.Department;
import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.DepartmentRepository;
import oll.business.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public UserService(UserRepository userRepository,
                   DepartmentRepository departmentRepository,
                   PasswordEncoder passwordEncoder,
                   LogService logService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public List<User> findByDepartmentId(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            logService.logWarn("User creation failed: username already exists", "UserService", "create");
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.departmentId()));
            user.setDepartment(department);
        }

        User saved = userRepository.save(user);
        logService.logInfo("User created: " + saved.getUsername() + " (id=" + saved.getId() + ", role=" + request.role() + ")", "UserService", "create", saved.getId(), null);
        return saved;
    }

    @Transactional
    public User update(Long id, UserRequest request) {
        User user = findById(id);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.departmentId()));
            user.setDepartment(department);
        }

        User saved = userRepository.save(user);
        logService.logInfo("User updated: id=" + id, "UserService", "update", id, null);
        return saved;
    }

    @Transactional
    public User updatePassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        logService.logInfo("Password changed for user id=" + id, "UserService", "updatePassword", id, null);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
        logService.logInfo("User deleted: " + user.getUsername() + " (id=" + id + ")", "UserService", "delete", id, null);
    }

    public record UserRequest(
            String username,
            String password,
            Role role,
            String firstName,
            String lastName,
            Long departmentId
    ) {}
}