package org.example.backend.controller;

import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.example.backend.repository.TeamRepository;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.entity.Role;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DebugController(
            UserRepository userRepository,
            TeamRepository teamRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.username:admin}") String adminUsername,
            @Value("${app.default-admin.password:Admin@123}") String adminPassword) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
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

    @GetMapping("/seed-factories")
    public Map<String, Object> seedFactories() {
        // Find or create a dummy owner
        User owner = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPassword(passwordEncoder.encode("Admin@123"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        List<Team> factories = List.of(
                createMockTeam("Xưởng Rang Đắk Lắk", "Đắk Lắk, Tây Nguyên", "Xưởng rang quy mô lớn, công nghệ hot-air hiện đại", 95, 4.8, 35, 100.0, 20, owner),
                createMockTeam("Xưởng Rang Gia Lai", "Gia Lai, Tây Nguyên", "Chuyên gia công Arabica chất lượng cao", 92, 4.9, 27, 85.0, 12, owner),
                createMockTeam("Cà Phê Mộc Sơn", "Lâm Đồng", "Xưởng rang truyền thống, chuyên cà phê mộc nguyên chất", 88, 4.5, 40, 150.0, 6, owner),
                createMockTeam("Xưởng Rang Quận 9", "Hồ Chí Minh", "Chuyên cung cấp cho chuỗi quán cafe hiện đại", 98, 5.0, 50, 200.0, 4, owner),
                createMockTeam("Cầu Đất Roaster", "Lâm Đồng", "Cà phê đặc sản Cầu Đất, rang mộc thủ công", 90, 4.7, 22, 60.0, 8, owner)
        );

        teamRepository.saveAll(factories);

        return Map.of("message", "Đã tạo " + factories.size() + " xưởng mẫu thành công!");
    }

    private Team createMockTeam(String name, String region, String specialty, int score, double rating, int orders, double capacity, int delivery, User owner) {
        Team t = new Team();
        t.setName(name);
        t.setDescription("Đây là xưởng mẫu được tạo tự động.");
        t.setRegion(region);
        t.setSpecialty(specialty);
        t.setCapacityValue(capacity);
        t.setCapacityUnit("kg");
        t.setFactoryType("Xưởng rang & đóng gói");
        t.setInviteCode("SEED" + Math.round(Math.random() * 1000));
        t.setPublished(true);
        t.setOwner(owner);
        
        t.setCompletedOrders(orders);
        t.setOnTimeOrders(orders - 2);
        t.setTotalOrders(orders + 2);
        t.setTotalRatings(10);
        t.setSumRatings(10 * rating);
        return t;
    }
}
