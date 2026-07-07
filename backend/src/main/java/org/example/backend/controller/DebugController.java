package org.example.backend.controller;

import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.entity.Role;
import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.GroupRole;
import java.time.LocalDateTime;

/**
 * Debug / developer endpoints. **Only registered in the {@code dev} profile.**
 *
 * <p>The demo data endpoints — {@code /api/debug/seed-factories} and
 * {@code /api/debug/seed-20-members} — were the source of the auto-injected
 * "Arabica / Robusta / Đã xay" rows and the 20 fake "Xưởng …" factories
 * visible in the marketplace. They are preserved (as no-ops) in case a
 * developer needs to re-enable them locally by switching to the {@code dev}
 * Spring profile. The destructive demo seeds have been disabled regardless
 * of profile: callers receive an explicit error message instead of silent
 * database writes.
 */
@RestController
@RequestMapping("/api/debug")
@Profile("dev")
public class DebugController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final String adminUsername;
    private final String adminPassword;

    public DebugController(
            UserRepository userRepository,
            TeamRepository teamRepository,
            InventoryRepository inventoryRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager,
            @Value("${app.default-admin.username:admin}") String adminUsername,
            @Value("${app.default-admin.password:Admin@123}") String adminPassword) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.inventoryRepository = inventoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
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

    /**
     * Demo factory seeder. DISABLED — returns an explanatory error so callers
     * get a clear signal rather than silent database writes.
     */
    @GetMapping("/seed-factories")
    public Map<String, Object> seedFactories() {
        return Map.of(
                "error", "DEBUG_SEED_DISABLED",
                "message", "Demo factory seeding is disabled. " +
                        "Use the application's standard team-creation flow to add real factories."
        );
    }

    /**
     * 20-member demo seeder. DISABLED — see {@link #seedFactories()}.
     */
    @GetMapping("/seed-20-members")
    public Map<String, Object> seed20Members() {
        return Map.of(
                "error", "DEBUG_SEED_DISABLED",
                "message", "Demo member seeding is disabled. " +
                        "Invite real members via the application's invitation flow."
        );
    }
}
