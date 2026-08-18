package org.example.backend.runner;

import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TempRunner implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== TEMP RUNNER: LISTING ALL USERS ===");
        List<User> users = userRepository.findAll();
        for (User u : users) {
            System.out.println("USER: " + u.getUsername() + ", ROLE: " + u.getRole() + ", ID: " + u.getId());
        }
        System.out.println("=== TEMP RUNNER DONE ===");
    }
}
