package org.example.backend.config;

import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final String adminFullName;

    public DefaultAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.username:admin}") String adminUsername,
            @Value("${app.default-admin.password:Admin@123}") String adminPassword,
            @Value("${app.default-admin.email:admin@orca.local}") String adminEmail,
            @Value("${app.default-admin.full-name:Administrator}") String adminFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.adminFullName = adminFullName;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername(adminUsername)
                .ifPresentOrElse(this::promoteToAdmin, this::createDefaultAdmin);
    }

    private void createDefaultAdmin() {
        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .email(adminEmail)
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
    }

    private void promoteToAdmin(User user) {
        boolean changed = false;

        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            changed = true;
        }

        if (!passwordEncoder.matches(adminPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }
}
