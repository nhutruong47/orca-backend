package org.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class VersionFixRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public VersionFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("UPDATE teams SET version = 0 WHERE version IS NULL");
            System.out.println("Ran version fix runner for teams.");
        } catch (Exception e) {
            System.err.println("Failed to fix versions: " + e.getMessage());
        }
    }
}
