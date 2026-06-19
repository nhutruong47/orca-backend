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
import org.example.backend.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.entity.Role;
import org.example.backend.entity.InventoryItem;

@RestController
@RequestMapping("/api/debug")
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

    @GetMapping("/fix-images")
    @Transactional
    public Map<String, Object> fixImages() {
        List<Team> teams = teamRepository.findAll();
        List<String> images = java.util.Arrays.asList(
            "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=500&q=80",
            "https://images.unsplash.com/photo-1514432324607-a09d9b4aefda?w=500&q=80",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=500&q=80",
            "https://images.unsplash.com/photo-1504630083234-14187a9df0f5?w=500&q=80",
            "https://images.unsplash.com/photo-1498804103079-a6351b050096?w=500&q=80",
            "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500&q=80",
            "https://images.unsplash.com/photo-1497515114889-1c06568a37b8?w=500&q=80",
            "https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=500&q=80",
            "https://images.unsplash.com/photo-1511537190424-bbbab87ac5eb?w=500&q=80",
            "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=500&q=80",
            "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500&q=80",
            "https://images.unsplash.com/photo-1512568400610-62da28bc8a13?w=500&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=500&q=80",
            "https://images.unsplash.com/photo-1507133750050-4a2ce37285f1?w=500&q=80",
            "https://images.unsplash.com/photo-1524350876685-274059332603?w=500&q=80"
        );
        int count = 0;
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            if (t.getFactoryImageUrl() == null || t.getFactoryImageUrl().isBlank()) {
                t.setFactoryImageUrl(images.get(i % images.size()));
                count++;
            }
        }
        teamRepository.saveAll(teams);
        return Map.of("message", "Đã cập nhật ảnh cho " + count + " xưởng!");
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
    @Transactional
    public Map<String, Object> seedFactories() {
        try {
            entityManager.createNativeQuery("ALTER TABLE inventory_items ALTER COLUMN name DROP NOT NULL").executeUpdate();
        } catch (Exception e) {
            System.out.println("No need to alter name column: " + e.getMessage());
        }
        
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

        List<InventoryItem> products = new java.util.ArrayList<>();
        for (Team f : factories) {
            products.add(createMockProduct(f, "Arabica Cầu Đất", "ROASTED", 500.0, "kg"));
            products.add(createMockProduct(f, "Robusta Honey", "GREEN", 1000.0, "kg"));
            products.add(createMockProduct(f, "Espresso Blend", "PACKAGED", 200.0, "kg"));
            products.add(createMockProduct(f, "Culi Đặc Biệt", "ROASTED", 150.0, "kg"));
            products.add(createMockProduct(f, "Arabica Blend", "GROUND", 300.0, "kg"));
        }
        inventoryRepository.saveAll(products);

        return Map.of("message", "Đã tạo " + factories.size() + " xưởng và " + products.size() + " sản phẩm thành công!");
    }

    private InventoryItem createMockProduct(Team team, String type, String state, Double quantity, String unit) {
        InventoryItem item = new InventoryItem();
        item.setTeam(team);
        item.setProductType(type);
        item.setProductState(state);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setLowStockThreshold(50.0);
        return item;
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
