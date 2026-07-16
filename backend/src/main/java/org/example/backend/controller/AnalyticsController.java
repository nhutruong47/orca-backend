package org.example.backend.controller;

import org.example.backend.dto.AnalyticsDTO;
import org.example.backend.dto.ReplanDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.AnalyticsService;
import org.example.backend.service.AiReplanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/production/analytics")
@CrossOrigin("*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AiReplanService replanService;
    private final AccessControlService accessControlService;

    public AnalyticsController(AnalyticsService analyticsService, AiReplanService replanService,
                               AccessControlService accessControlService) {
        this.analyticsService = analyticsService;
        this.replanService = replanService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getAnalytics(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        accessControlService.requireTeamMember(currentUser, teamId);
        return ResponseEntity.ok(analyticsService.getAnalytics(teamId, startDate, endDate));
    }

    @GetMapping("/orders/{orderId}/replan")
    public ResponseEntity<?> analyzeReplan(@PathVariable UUID orderId,
                                           @AuthenticationPrincipal User currentUser) {
        accessControlService.requireProductionOrderAccess(currentUser, orderId);
        return ResponseEntity.ok(replanService.analyzeReplan(orderId));
    }

    @PostMapping("/orders/{orderId}/replan/apply")
    public ResponseEntity<?> applyReplan(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<Map<String, Object>> revisedTargets) {
        accessControlService.requireProductionOrderAccess(currentUser, orderId);
        replanService.applyReplan(orderId, revisedTargets);
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Da cap nhat ke hoach"));
    }
}
