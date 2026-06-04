package org.example.backend.repository;

import org.example.backend.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {
    List<ProductionOrder> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}
