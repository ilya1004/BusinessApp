package oll.business.service;

import oll.business.model.Department;
import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.DepartmentRepository;
import oll.business.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                   DepartmentRepository departmentRepository,
                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
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

        return userRepository.save(user);
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

        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
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