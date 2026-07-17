package org.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryVersionFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public InventoryVersionFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("UPDATE inventory_items SET version = 0 WHERE version IS NULL");
    }
}
