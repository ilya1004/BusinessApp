package oll.business.controller;

import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.JwtUtils;
import oll.business.service.LogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LogService logService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, LogService logService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.logService = logService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            logService.logWarn("Registration failed: username already exists", "AuthController", "register");
            throw new RuntimeException("Username already exists");
        }

        Role role = Role.valueOf(request.role().toUpperCase());

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                role,
                request.firstName(),
                request.lastName()
        );
        userRepository.save(user);
        logService.logInfo("User registered: " + request.username(), "AuthController", "register", user.getId(), "role=" + role);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().getValue());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    logService.logWarn("Login failed: user not found", "AuthController", "login");
                    return new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            logService.logWarn("Login failed: wrong password for " + request.username(), "AuthController", "login");
            throw new RuntimeException("Invalid credentials");
        }

        logService.logInfo("User logged in: " + request.username(), "AuthController", "login", user.getId(), null);
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().getValue());
        return new AuthResponse(token);
    }

    public record RegisterRequest(String username, String password, String firstName, String lastName, String role) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token) {}
    public record UserInfo(Long id, String username, Role role, String firstName, String lastName) {}
}