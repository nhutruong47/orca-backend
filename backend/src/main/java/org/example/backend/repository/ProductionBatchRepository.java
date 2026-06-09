package org.example.backend.repository;

import org.example.backend.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, UUID> {
    List<ProductionBatch> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
    List<ProductionBatch> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
