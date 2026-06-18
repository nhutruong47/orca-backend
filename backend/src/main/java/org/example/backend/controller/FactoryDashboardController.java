package org.example.backend.controller;

import org.example.backend.service.FactoryDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/production/dashboard")
@CrossOrigin("*")
public class FactoryDashboardController {

    private final FactoryDashboardService dashboardService;

    public FactoryDashboardController(FactoryDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getDashboard(@PathVariable UUID teamId) {
        return ResponseEntity.ok(dashboardService.getDashboard(teamId));
    }

    @GetMapping("/{teamId}/productivity")
    public ResponseEntity<?> getProductivity(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dashboardService.getProductivitySummary(teamId, startDate, endDate));
    }
}
