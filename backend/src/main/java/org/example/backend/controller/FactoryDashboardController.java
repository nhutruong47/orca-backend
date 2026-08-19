package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.FactoryDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/production/dashboard")

public class FactoryDashboardController {

    private final FactoryDashboardService dashboardService;
    private final AccessControlService accessControlService;

    public FactoryDashboardController(FactoryDashboardService dashboardService,
                                      AccessControlService accessControlService) {
        this.dashboardService = dashboardService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getDashboard(@PathVariable UUID teamId,
                                          @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        return ResponseEntity.ok(dashboardService.getDashboard(teamId));
    }

    @GetMapping("/{teamId}/productivity")
    public ResponseEntity<?> getProductivity(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        return ResponseEntity.ok(dashboardService.getProductivitySummary(teamId, startDate, endDate));
    }
}
