package oll.business.config;

import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.LogService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder, LogService logService) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setFirstName("Admin");
                admin.setLastName("User");
                userRepository.save(admin);
                logService.logInfo("Admin user created: admin / admin123", "SYSTEM", "init");
            }
        };
    }
}