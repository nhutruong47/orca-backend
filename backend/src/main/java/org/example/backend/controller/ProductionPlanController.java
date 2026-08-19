package org.example.backend.controller;

import org.example.backend.dto.DailyTargetDTO;
import org.example.backend.dto.ProductionPlanDTO;
import org.example.backend.service.ProductionPlanService;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/production/plans")

public class ProductionPlanController {

    private final ProductionPlanService planService;
    private final AccessControlService accessControlService;

    public ProductionPlanController(ProductionPlanService planService, AccessControlService accessControlService) {
        this.planService = planService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/orders/{orderId}/generate")
    public ResponseEntity<?> generatePlan(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireProductionOrderAccess(currentUser, orderId);
        try {
            ProductionPlanDTO plan = planService.generatePlan(orderId);
            return ResponseEntity.ok(plan);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireProductionPlanAccess(currentUser, planId);
        try {
            return ResponseEntity.ok(planService.getPlanById(planId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getPlansByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireProductionOrderAccess(currentUser, orderId);
        return ResponseEntity.ok(planService.getPlansByOrder(orderId));
    }

    @PatchMapping("/{planId}/approve")
    public ResponseEntity<?> approvePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> body) {
        accessControlService.requireProductionPlanAccess(currentUser, planId);
        try {
            UUID approvedBy = null;
            if (body != null && body.get("approvedBy") != null) {
                approvedBy = UUID.fromString(body.get("approvedBy"));
            }
            return ResponseEntity.ok(planService.approvePlan(planId, approvedBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{planId}/daily-targets")
    public ResponseEntity<?> getDailyTargets(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireProductionPlanAccess(currentUser, planId);
        return ResponseEntity.ok(planService.getDailyTargetsByPlan(planId));
    }

    @PatchMapping("/daily-targets/{targetId}")
    public ResponseEntity<?> updateDailyActual(
            @PathVariable UUID targetId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        accessControlService.requireDailyTargetAccess(currentUser, targetId);
        try {
            Double actualRoastKg = getDouble(body, "actualRoastKg");
            Double actualQcKg = getDouble(body, "actualQcKg");
            Double actualQcFailKg = getDouble(body, "actualQcFailKg");
            Double actualPackagedKg = getDouble(body, "actualPackagedKg");
            Integer actualPackages = getInt(body, "actualPackages");
            String notes = getString(body, "notes");
            String issues = getString(body, "issues");

            DailyTargetDTO updated = planService.updateDailyActual(
                    targetId, actualRoastKg, actualQcKg, actualQcFailKg,
                    actualPackagedKg, actualPackages, notes, issues);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/today/{teamId}")
    public ResponseEntity<?> getTodayTarget(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        DailyTargetDTO target = planService.getTodayTarget(teamId);
        if (target != null) {
            return ResponseEntity.ok(target);
        }
        return ResponseEntity.noContent().build();
    }

    private String getString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
    }

    private Double getDouble(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer getInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
}
