package org.example.backend;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamRepository;
import org.example.backend.service.InterGroupOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class InterGroupOrderLifecycleTest {

    @Autowired
    private InterGroupOrderService interGroupOrderService;

    @Test
    public void contextLoads() {
        assertNotNull(interGroupOrderService);
    }
}
