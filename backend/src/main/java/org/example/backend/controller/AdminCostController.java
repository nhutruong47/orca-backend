package org.example.backend.controller;

import org.example.backend.dto.*;
import org.example.backend.entity.User;
import org.example.backend.service.CostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCostController {

    private final CostService costService;

    public AdminCostController(CostService costService) {
        this.costService = costService;
    }

    // --- Costs ---

    @GetMapping("/costs")
    public ResponseEntity<Page<CostDto>> getCosts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(costService.searchCosts(search, categoryId, status, pageable));
    }

    @GetMapping("/costs/{id}")
    public ResponseEntity<CostDto> getCost(@PathVariable UUID id) {
        return ResponseEntity.ok(costService.getCostById(id));
    }

    @PostMapping("/costs")
    public ResponseEntity<CostDto> createCost(@RequestBody CostRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(costService.createCost(request, user));
    }

    @PutMapping("/costs/{id}")
    public ResponseEntity<CostDto> updateCost(@PathVariable UUID id, @RequestBody CostRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(costService.updateCost(id, request, user));
    }

    @DeleteMapping("/costs/{id}")
    public ResponseEntity<Void> deleteCost(@PathVariable UUID id) {
        costService.deleteCost(id);
        return ResponseEntity.ok().build();
    }

    // --- Cost Categories ---

    @GetMapping("/costs/categories")
    public ResponseEntity<List<CostCategoryDto>> getCostCategories() {
        return ResponseEntity.ok(costService.getAllCategories());
    }

    @PostMapping("/costs/categories")
    public ResponseEntity<CostCategoryDto> createCostCategory(@RequestBody CostCategoryRequest request) {
        return ResponseEntity.ok(costService.createCategory(request));
    }

    @PutMapping("/costs/categories/{id}")
    public ResponseEntity<CostCategoryDto> updateCostCategory(@PathVariable UUID id, @RequestBody CostCategoryRequest request) {
        return ResponseEntity.ok(costService.updateCategory(id, request));
    }

    @DeleteMapping("/costs/categories/{id}")
    public ResponseEntity<Void> deleteCostCategory(@PathVariable UUID id) {
        costService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // --- Statistics ---

    @GetMapping("/costs/dashboard")
    public ResponseEntity<CostDashboardStats> getCostDashboard() {
        return ResponseEntity.ok(costService.getDashboardStats());
    }
}
