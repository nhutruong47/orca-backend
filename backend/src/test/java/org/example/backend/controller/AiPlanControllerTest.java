package org.example.backend.controller;

import org.example.backend.entity.AiPlan;
import org.example.backend.entity.User;
import org.example.backend.service.AiPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiPlanControllerTest {

    private AiPlanService aiPlanService;
    private AiPlanController aiPlanController;
    private User testUser;

    @BeforeEach
    void setUp() {
        aiPlanService = mock(AiPlanService.class);
        aiPlanController = new AiPlanController(aiPlanService);
        testUser = new User();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void getByTeam_ReturnsOk() {
        UUID teamId = UUID.randomUUID();
        when(aiPlanService.listForTeam(teamId, testUser)).thenReturn(List.of(new AiPlan()));

        ResponseEntity<?> response = aiPlanController.getByTeam(teamId, testUser);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getByTeam_RequiresAuth() {
        ResponseEntity<?> response = aiPlanController.getByTeam(UUID.randomUUID(), null);
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void createDraft_ReturnsCreated() {
        UUID teamId = UUID.randomUUID();
        AiPlan plan = new AiPlan();
        when(aiPlanService.generateDraft(eq(teamId), eq(testUser), eq("test query"), any()))
                .thenReturn(plan);

        ResponseEntity<?> response = aiPlanController.createDraft(teamId, Map.of("query", "test query"), testUser);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(plan, response.getBody());
    }

    @Test
    void createDraft_MissingQuery() {
        ResponseEntity<?> response = aiPlanController.createDraft(UUID.randomUUID(), Map.of(), testUser);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void revise_ReturnsOk() {
        UUID planId = UUID.randomUUID();
        AiPlan plan = new AiPlan();
        when(aiPlanService.revise(eq(planId), eq(testUser), eq("make it better"))).thenReturn(plan);

        ResponseEntity<?> response = aiPlanController.revise(planId, Map.of("instruction", "make it better"), testUser);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void updateStatus_ReturnsOk() {
        UUID planId = UUID.randomUUID();
        AiPlan plan = new AiPlan();
        when(aiPlanService.updateStatus(eq(planId), eq("APPROVED"), eq(testUser))).thenReturn(plan);

        ResponseEntity<?> response = aiPlanController.updateStatus(planId, Map.of("status", "APPROVED"), testUser);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void promote_ReturnsOk() {
        UUID planId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        AiPlan plan = new AiPlan();
        when(aiPlanService.markPromoted(eq(planId), eq(goalId), eq(testUser))).thenReturn(plan);

        ResponseEntity<?> response = aiPlanController.promote(planId, Map.of("goalId", goalId.toString()), testUser);

        assertEquals(200, response.getStatusCodeValue());
    }
}
