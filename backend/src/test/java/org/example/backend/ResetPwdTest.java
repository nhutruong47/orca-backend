package org.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;

@SpringBootTest
public class ResetPwdTest {
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    public void resetPassword() {
        String newHash = passwordEncoder.encode("AnPhu@2026");
        int rows = jdbcTemplate.update("UPDATE users SET password = ? WHERE username = 'nguyen.minh.hai'", newHash);
        
        if (rows == 0) {
            String chipId = "USR-" + UUID.randomUUID().toString();
            String id = UUID.randomUUID().toString();
            jdbcTemplate.update("INSERT INTO users (id, username, password, role, chip_id, locked, created_at) VALUES (?::uuid, ?, ?, 'MEMBER', ?, false, CURRENT_TIMESTAMP)", 
                id, "nguyen.minh.hai", newHash, chipId);
            System.out.println(">>> INSERTED NEW USER nguyen.minh.hai");
        } else {
            System.out.println(">>> ROWS UPDATED: " + rows);
        }
        System.out.println(">>> NEW HASH: " + newHash);
    }
}
