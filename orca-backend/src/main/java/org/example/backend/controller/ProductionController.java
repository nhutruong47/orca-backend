package org.example.backend.controller;

import org.example.backend.entity.ProductionBatch;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.repository.ProductionBatchRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.entity.Team;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/production")
public class ProductionController {

    private final ProductionOrderRepository orderRepo;
    private final ProductionBatchRepository batchRepo;
    private final TeamRepository teamRepo;

    public ProductionController(ProductionOrderRepository orderRepo,
            ProductionBatchRepository batchRepo,
            TeamRepository teamRepo) {
        this.orderRepo = orderRepo;
        this.batchRepo = batchRepo;
        this.teamRepo = teamRepo;
    }

    @GetMapping("/teams/{teamId}/orders")
    public ResponseEntity<?> getOrders(@PathVariable UUID teamId) {
        return ResponseEntity.ok(orderRepo.findByTeamIdOrderByCreatedAtDesc(teamId));
    }

    @PostMapping("/teams/{teamId}/orders")
    public ResponseEntity<?> createOrder(@PathVariable UUID teamId, @RequestBody ProductionOrder order) {
        Optional<Team> team = teamRepo.findById(teamId);
        if (team.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
        }
        order.setTeam(team.get());
        return ResponseEntity.ok(orderRepo.save(order));
    }

    @GetMapping("/teams/{teamId}/batches")
    public ResponseEntity<?> getBatches(@PathVariable UUID teamId) {
        return ResponseEntity.ok(batchRepo.findByTeamIdOrderByCreatedAtDesc(teamId));
    }

    @PostMapping("/teams/{teamId}/batches")
    public ResponseEntity<?> createBatch(@PathVariable UUID teamId, @RequestBody ProductionBatch batch) {
        Optional<Team> team = teamRepo.findById(teamId);
        if (team.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
        }
        batch.setTeam(team.get());
        if (batch.getOrder() != null && batch.getOrder().getId() != null) {
            orderRepo.findById(batch.getOrder().getId()).ifPresent(batch::setOrder);
        }
        return ResponseEntity.ok(batchRepo.save(batch));
    }

    @GetMapping("/orders/{orderId}/batches")
    public ResponseEntity<?> getBatchesByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(batchRepo.findByOrderIdOrderByCreatedAtDesc(orderId));
    }
}
