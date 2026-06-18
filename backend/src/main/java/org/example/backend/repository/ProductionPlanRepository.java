package org.example.backend.repository;

import org.example.backend.entity.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, UUID> {
    List<ProductionPlan> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Query("SELECT p FROM ProductionPlan p WHERE p.order.team.id = :teamId ORDER BY p.createdAt DESC")
    List<ProductionPlan> findByTeamId(@Param("teamId") UUID teamId);

    Optional<ProductionPlan> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
