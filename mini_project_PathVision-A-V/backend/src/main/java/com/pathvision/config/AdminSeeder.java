package com.pathvision.config;

import com.pathvision.entity.Role;
import com.pathvision.entity.User;
import com.pathvision.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    public CommandLineRunner commandLineRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // Hardcode Admin User
            String adminEmail = "admin@pathvision.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                    .fullName("Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123")) // Hardcoded password
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
                userRepository.save(admin);
                System.out.println("Admin user created: " + adminEmail);
            }
        };
    }
}
