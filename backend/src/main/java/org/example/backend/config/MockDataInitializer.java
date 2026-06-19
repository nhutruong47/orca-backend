package org.example.backend.config;

import org.example.backend.entity.Role;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@DependsOn("defaultAdminInitializer")
public class MockDataInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;

    public MockDataInitializer(
            TeamRepository teamRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.username:admin}") String adminUsername) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
    }

    @Override
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            return;
        }

        String[] specialties = {"Cung ứng cà phê nhân", "Rang cà phê", "Đóng gói", "Gia công OEM", "Xử lý sau thu hoạch"};
        List<String> images = Arrays.asList(
            "https://images.unsplash.com/photo-1559525839-b184a4d698c7?w=500&q=80",
            "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=500&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=500&q=80",
            "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=500&q=80",
            "https://images.unsplash.com/photo-1563089145-599997674d42?w=500&q=80",
            "https://images.unsplash.com/photo-1442512595305-bd2700d599a0?w=500&q=80",
            "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=500&q=80",
            "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=500&q=80",
            "https://images.unsplash.com/photo-1504813184591-58d0426f8d05?w=500&q=80",
            "https://images.unsplash.com/photo-1551888419-f538eec4c278?w=500&q=80",
            "https://images.unsplash.com/photo-1587293852726-70cdb56c2866?w=500&q=80",
            "https://images.unsplash.com/photo-1524350876685-274059332603?w=500&q=80",
            "https://images.unsplash.com/photo-1512516624996-24ba08ffc05e?w=500&q=80",
            "https://images.unsplash.com/photo-1507133750050-4a2ce37285f1?w=500&q=80"
        );

        for (int i = 0; i < 20; i++) {
            // Create a unique owner for each factory
            String username = "factory_owner_" + (i + 1);
            User factoryOwner;
            Optional<User> existingUser = userRepository.findByUsername(username);
            if (existingUser.isPresent()) {
                factoryOwner = existingUser.get();
            } else {
                factoryOwner = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Chủ xưởng " + (i + 1))
                        .email("factory" + (i + 1) + "@orca.local")
                        .role(Role.MEMBER)
                        .build();
                factoryOwner = userRepository.save(factoryOwner);
            }

            Team team = new Team();
            String spec = specialties[i % specialties.length];
            team.setName("Xưởng " + spec + " " + (i + 1));
            team.setDescription("Đây là xưởng chuyên " + spec + " với công nghệ hiện đại, đáp ứng mọi nhu cầu của đối tác.");
            team.setOwner(factoryOwner);
            team.setPublished(true);
            team.setSpecialty(spec);
            team.setRegion(i % 2 == 0 ? "Việt Nam" : "Đắk Lắk");
            team.setCapacity("1000");
            team.setCapacityUnit("kg/tháng");
            team.setCapacityValue(1000.0);
            team.setVerificationStatus(i % 3 == 0 ? "PENDING" : "VERIFIED");
            team.setCompletedOrders(10 + i * 5);
            team.setTotalOrders(15 + i * 5);
            
            // Trust / Rating
            int onTime = (int) ((8 + i) * 0.85);
            team.setOnTimeOrders(onTime);
            team.setLateOrders((10 + i * 5) - onTime);
            int ratings = 5 + (i % 10);
            team.setTotalRatings(ratings);
            team.setSumRatings(ratings * (4.0 + (i % 3) * 0.5));
            team.setFactoryImageUrl(images.get(i % images.size()));
            team.setFactoryType("roastery");
            
            teamRepository.save(team);
        }
    }
}
