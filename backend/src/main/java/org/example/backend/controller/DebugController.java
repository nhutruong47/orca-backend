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
        // Find or create a dummy owner
        User owner = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPassword(passwordEncoder.encode("Admin@123"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        List<Team> factories = List.of(
                createMockTeam("Xưởng Rang Đắk Lắk", "Đắk Lắk, Tây Nguyên", "Rang cà phê,Đóng gói,Gia công OEM", 95, 4.8, 125, 100.0, 5, owner, "50 kg", "3-5 Ngày", 8, "Đang nhận đơn", 25, "1000 m2"),
                createMockTeam("Xưởng Rang Gia Lai", "Gia Lai, Tây Nguyên", "Arabica Specialty,OEM Coffee", 92, 4.9, 87, 85.0, 8, owner, "100 kg", "5-7 Ngày", 5, "Sắp kín lịch", 15, "800 m2"),
                createMockTeam("Cà Phê Mộc Sơn", "Lâm Đồng", "Xay cà phê (Grinding),Rang mẫu / Test profile", 88, 4.5, 42, 150.0, 6, owner, "200 kg", "7-10 Ngày", 12, "Đang nhận đơn", 40, "1500 m2"),
                createMockTeam("Xưởng Rang Quận 9", "Hồ Chí Minh", "Rang cà phê (Roasting),Đóng gói (Packaging)", 98, 5.0, 210, 200.0, 4, owner, "500 kg", "3-5 Ngày", 15, "Tạm ngưng", 60, "2000 m2"),
                createMockTeam("Cầu Đất Roaster", "Lâm Đồng", "Cà phê đặc sản Cầu Đất, rang mộc thủ công", 90, 4.7, 65, 60.0, 8, owner, "10 kg", "2-3 Ngày", 3, "Đang nhận đơn", 10, "300 m2")
        );

        teamRepository.saveAll(factories);

        List<InventoryItem> products = new java.util.ArrayList<>();
        for (Team f : factories) {
            products.add(createMockProduct(f, "Arabica Cầu Đất", "ROASTED", 500.0, "kg", null, null, null, null, null, null, null, false));
            products.add(createMockProduct(f, "Robusta Honey", "GREEN", 1000.0, "kg", null, null, null, null, null, null, null, false));
            products.add(createMockProduct(f, "Espresso Blend", "PACKAGED", 200.0, "kg", null, null, null, null, null, null, null, false));
            products.add(createMockProduct(f, "Culi Đặc Biệt", "ROASTED", 150.0, "kg", null, null, null, null, null, null, null, false));
            products.add(createMockProduct(f, "Arabica Blend", "GROUND", 300.0, "kg", null, null, null, null, null, null, null, false));
        }

        // Add 5 Featured Products linked to the first factory
        Team mainFactory = factories.get(0);
        products.add(createMockProduct(mainFactory, "Ethiopia Yirgacheffe G1", "ROASTED", 100.0, "kg",
                "450.000đ", "Sơ chế Natural với nốt hương hoa nhài và trà đen đặc trưng. Được thu hoạch từ vùng trồng Yirgacheffe danh tiếng, mang lại trải nghiệm hương vị tinh tế, nhẹ nhàng và hậu vị ngọt kéo dài.",
                "/coffee-hero.png", "Yirgacheffe, Ethiopia", "Light - Medium", "Natural", "Hoa nhài, Trà đen, Cam chanh, Mật ong", true));
        products.add(createMockProduct(mainFactory, "Colombia Supremo", "ROASTED", 50.0, "kg",
                "380.000đ", "Vị đậm đà, body mượt mà với hương chocolate và hạt dẻ.",
                "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?auto=format&fit=crop&w=720&q=85",
                "Huila, Colombia", "Medium", "Washed", "Chocolate, Caramel, Hạt dẻ", true));
        products.add(createMockProduct(mainFactory, "Kenya AA Top", "ROASTED", 20.0, "kg",
                "550.000đ", "Độ chua sáng, nốt hương trái cây nhiệt đới rõ nét.",
                "https://images.unsplash.com/photo-1587293852726-70cdb56c2866?auto=format&fit=crop&w=720&q=85",
                "Nyeri, Kenya", "Light", "Washed", "Blackberry, Chanh vàng, Mía đường", true));
        products.add(createMockProduct(mainFactory, "Máy đo độ ẩm S3", "PACKAGED", 5.0, "chiếc",
                "2.100.000đ", "Thiết bị cầm tay độ chính xác cao cho hạt xanh. Giúp kiểm soát chất lượng cà phê nhân xanh trước khi rang một cách dễ dàng và nhanh chóng.",
                "https://images.unsplash.com/photo-1510707577719-ae7c14805e3a?auto=format&fit=crop&w=720&q=85",
                "Đài Loan", "", "", "", true));
        products.add(createMockProduct(mainFactory, "Dịch vụ Rang Test", "PACKAGED", 999.0, "lần",
                "350.000đ", "Gói 5 mẫu profile khác nhau cho 1kg hạt. Phù hợp cho khách hàng muốn tìm ra profile rang tối ưu nhất cho dòng hạt mới trước khi sản xuất số lượng lớn.",
                "https://images.unsplash.com/photo-1580933073521-dc49ac0d4e6a?auto=format&fit=crop&w=720&q=85",
                "", "", "", "", true));

        inventoryRepository.saveAll(products);

        return Map.of("message", "Đã tạo " + factories.size() + " xưởng và " + products.size() + " sản phẩm thành công!");
    }

    private InventoryItem createMockProduct(Team team, String type, String state, Double quantity, String unit, String price, String description, String imageUrl, String origin, String roastLevel, String processing, String tasteNotes, boolean isFeatured) {
        InventoryItem item = new InventoryItem();
        item.setTeam(team);
        item.setProductType(type);
        item.setProductState(state);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setLowStockThreshold(50.0);
        item.setPrice(price);
        item.setDescription(description);
        item.setImageUrl(imageUrl);
        item.setOrigin(origin);
        item.setRoastLevel(roastLevel);
        item.setProcessing(processing);
        item.setTasteNotes(tasteNotes);
        item.setIsFeatured(isFeatured);
        return item;
    }

    private Team createMockTeam(String name, String region, String specialty, int score, double rating, int orders, double capacity, int delivery, User owner, String moq, String leadTime, int yearsInOperation, String statusBadge, int employeeCount, String factorySize) {
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
        
        t.setRating(rating);
        t.setReviewCount((int) (Math.random() * 50) + 10);
        
        t.setMoq(moq);
        t.setLeadTime(leadTime);
        t.setYearsInOperation(yearsInOperation);
        t.setStatusBadge(statusBadge);
        t.setEmployeeCount(employeeCount);
        t.setFactorySize(factorySize);
        t.setVerificationStatus("APPROVED");

        String mockCapabilities = "{\"services\":[\"Rang cà phê\",\"Đóng gói\",\"Gia công OEM\"],\"coffeeTypes\":[\"Arabica\",\"Robusta\",\"Culi / Peaberry\",\"Moka\",\"Catimor\",\"Blend (Phối trộn)\"],\"packagingFormats\":[\"Túi 250g\",\"Túi 500g\",\"Túi 1kg\",\"Bao 5kg\"]}";
        String mockEquipment = "{\"roasters\":[{\"model\":\"Probat P25\",\"capacity\":\"25kg/mẻ\",\"year\":\"2021\"},{\"model\":\"Bühler Infinity\",\"capacity\":\"120kg/mẻ\",\"year\":\"2019\"}],\"packaging\":[\"Máy đóng gói tự động\",\"Máy hút chân không\"],\"grinders\":[\"Mahlkönig EK43\",\"Ditting KR804\"],\"qc\":[\"Máy đo màu rang\",\"Máy đo độ ẩm\",\"Khúc xạ kế\"]}";
        String mockPortfolio = "[{\"name\":\"Dự án OEM Chuỗi Cafe\",\"type\":\"OEM\",\"image\":\"https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=900&q=85\"},{\"name\":\"Gia công xuất khẩu\",\"type\":\"Export\",\"image\":\"https://images.unsplash.com/photo-1504630083234-14187a9df0f5?auto=format&fit=crop&w=900&q=85\"}]";
        String mockReviews = "[{\"author\":\"Nguyen Van A\",\"company\":\"The Coffee Shop\",\"rating\":5,\"date\":\"10/06/2026\",\"content\":\"Chất lượng rang ổn định, giao hàng đúng hẹn.\"},{\"author\":\"Tran Thi B\",\"company\":\"Daily Roast\",\"rating\":4,\"date\":\"02/05/2026\",\"content\":\"Máy móc hiện đại, làm việc chuyên nghiệp, hỗ trợ tốt.\"}]";
        String mockCertificates = "[{\"name\":\"ISO 22000:2018\",\"issueDate\":\"12/05/2022\",\"expDate\":\"12/05/2025\",\"status\":\"Verified\"},{\"name\":\"HACCP\",\"issueDate\":\"10/08/2023\",\"expDate\":\"10/08/2026\",\"status\":\"Verified\"}]";
        
        t.setMetadata(String.format("{\"capabilitiesMock\":%s,\"equipmentMock\":%s,\"portfolioMock\":%s,\"reviewsMock\":%s,\"certificatesMock\":%s}", mockCapabilities, mockEquipment, mockPortfolio, mockReviews, mockCertificates));

        return t;
    }
}
