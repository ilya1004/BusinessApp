package oll.business.controller;

import oll.business.model.Role;
import oll.business.model.User;
import oll.business.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
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