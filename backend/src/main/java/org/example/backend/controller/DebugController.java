package org.example.backend.controller;

import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DebugController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.username:admin}") String adminUsername,
            @Value("${app.default-admin.password:Admin@123}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/admin-status")
    public Map<String, Object> adminStatus() {
        return userRepository.findByUsername(adminUsername)
                .<Map<String, Object>>map(user -> Map.of(
                        "configuredUsername", adminUsername,
                        "exists", true,
                        "role", user.getRole().name(),
                        "passwordMatchesConfiguredValue", passwordEncoder.matches(adminPassword, user.getPassword())))
                .orElseGet(() -> Map.of(
                        "configuredUsername", adminUsername,
                        "exists", false,
                        "passwordMatchesConfiguredValue", false));
    }
}
