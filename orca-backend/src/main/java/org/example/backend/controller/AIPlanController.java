package org.example.backend.controller;

import org.example.backend.entity.AIPlan;
import org.example.backend.entity.AIPlanItem;
import org.example.backend.entity.Team;
import org.example.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-plans")
public class AIPlanController {

    private final AIPlanRepository aiPlanRepo;
    private final AIPlanItemRepository aiPlanItemRepo;
    private final TeamRepository teamRepo;
    private final ProductionOrderRepository orderRepo;
    private final ProductionBatchRepository batchRepo;

    public AIPlanController(AIPlanRepository aiPlanRepo,
            AIPlanItemRepository aiPlanItemRepo,
            TeamRepository teamRepo,
            ProductionOrderRepository orderRepo,
            ProductionBatchRepository batchRepo) {
        this.aiPlanRepo = aiPlanRepo;
        this.aiPlanItemRepo = aiPlanItemRepo;
        this.teamRepo = teamRepo;
        this.orderRepo = orderRepo;
        this.batchRepo = batchRepo;
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getPlans(@PathVariable UUID teamId) {
        return ResponseEntity.ok(aiPlanRepo.findByTeamIdOrderByCreatedAtDesc(teamId));
    }

    @PostMapping("/teams/{teamId}")
    public ResponseEntity<?> createPlan(@PathVariable UUID teamId, @RequestBody AIPlan plan) {
        Optional<Team> team = teamRepo.findById(teamId);
        if (team.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
        }
        plan.setTeam(team.get());
        if (plan.getOrder() != null && plan.getOrder().getId() != null) {
            orderRepo.findById(plan.getOrder().getId()).ifPresent(plan::setOrder);
        }
        if (plan.getBatch() != null && plan.getBatch().getId() != null) {
            batchRepo.findById(plan.getBatch().getId()).ifPresent(plan::setBatch);
        }
        plan.setStatus(plan.getStatus() != null ? plan.getStatus() : "DRAFT");
        return ResponseEntity.ok(aiPlanRepo.save(plan));
    }

    @GetMapping("/{planId}/items")
    public ResponseEntity<?> getItems(@PathVariable UUID planId) {
        return ResponseEntity.ok(aiPlanItemRepo.findByAiPlanId(planId));
    }

    @PostMapping("/{planId}/items")
    public ResponseEntity<?> addItem(@PathVariable UUID planId, @RequestBody AIPlanItem item) {
        Optional<AIPlan> plan = aiPlanRepo.findById(planId);
        if (plan.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "AI plan not found"));
        }
        item.setAiPlan(plan.get());
        item.setStatus(item.getStatus() != null ? item.getStatus() : "DRAFT");
        return ResponseEntity.ok(aiPlanItemRepo.save(item));
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID planId, @RequestBody Map<String, String> body) {
        Optional<AIPlan> plan = aiPlanRepo.findById(planId);
        if (plan.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "AI plan not found"));
        }
        plan.get().setStatus(body.getOrDefault("status", plan.get().getStatus()));
        return ResponseEntity.ok(aiPlanRepo.save(plan.get()));
    }
}
