
package com.trustvault.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            createUser(userRepository, passwordEncoder,
                    "admin", "ChangeMe_Admin_2026!", "ADMIN");

            createUser(userRepository, passwordEncoder,
                    "owner1", "ChangeMe_Owner_2026!", "OWNER");

            createUser(userRepository, passwordEncoder,
                    "viewer1", "ChangeMe_Viewer_2026!", "VIEWER");
        };
    }

    private void createUser(
            UserRepository repository,
            PasswordEncoder encoder,
            String username,
            String rawPassword,
            String role) {

        if (!repository.existsByUsername(username)) {
            AppUser user = new AppUser(
                    username,
                    encoder.encode(rawPassword),
                    role);

            repository.save(user);
        }
    }
}
