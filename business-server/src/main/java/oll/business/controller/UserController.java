package oll.business.controller;

import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/roles")
    public List<Role> findAllRoles() {
        return Arrays.asList(Role.values());
    }

    @GetMapping("/{id:\\d+}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/me")
    public AuthController.UserInfo me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new AuthController.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
        );
    }
    @GetMapping("/username/{username}")
    public User findByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }

    @GetMapping("/department/{departmentId}")
    public List<User> findByDepartmentId(@PathVariable Long departmentId) {
        return userService.findByDepartmentId(departmentId);
    }

    @GetMapping("/role/{role}")
    public List<User> findByRole(@PathVariable Role role) {
        return userService.findByRole(role);
    }

    @PostMapping
    public User create(@RequestBody UserService.UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody UserService.UserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/password")
    public User updatePassword(@PathVariable Long id, @RequestParam String newPassword) {
        return userService.updatePassword(id, newPassword);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}