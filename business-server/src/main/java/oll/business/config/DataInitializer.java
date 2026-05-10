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
    public CommandLineRunner initDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, LogService logService) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setFirstName("Admin");
                admin.setLastName("User");
                userRepository.save(admin);
                logService.logInfo("Default admin created: admin / admin123", "SYSTEM", "init");
            }
            if (!userRepository.existsByUsername("analyst")) {
                User analyst = new User();
                analyst.setUsername("analyst");
                analyst.setPasswordHash(passwordEncoder.encode("analyst123"));
                analyst.setRole(Role.ANALYST);
                analyst.setFirstName("Analyst");
                analyst.setLastName("User");
                userRepository.save(analyst);
                logService.logInfo("Default analyst created: analyst / analyst123", "SYSTEM", "init");
            }
            if (!userRepository.existsByUsername("manager")) {
                User manager = new User();
                manager.setUsername("manager");
                manager.setPasswordHash(passwordEncoder.encode("manager123"));
                manager.setRole(Role.MANAGER);
                manager.setFirstName("Manager");
                manager.setLastName("User");
                userRepository.save(manager);
                logService.logInfo("Default manager created: manager / manager123", "SYSTEM", "init");
            }
            if (!userRepository.existsByUsername("user")) {
                User executor = new User();
                executor.setUsername("user");
                executor.setPasswordHash(passwordEncoder.encode("user123"));
                executor.setRole(Role.EXECUTOR);
                executor.setFirstName("Executor");
                executor.setLastName("User");
                userRepository.save(executor);
                logService.logInfo("Default executor created: user / user123", "SYSTEM", "init");
            }
        };
    }
}