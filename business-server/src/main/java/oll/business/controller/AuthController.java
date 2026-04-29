package oll.business.controller;

import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
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

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().getValue());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().getValue());
        return new AuthResponse(token);
    }

    public record RegisterRequest(String username, String password, String firstName, String lastName, String role) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token) {}
}