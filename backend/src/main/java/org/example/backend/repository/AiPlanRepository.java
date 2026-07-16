package org.example.backend.repository;

import org.example.backend.entity.AiPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiPlanRepository extends JpaRepository<AiPlan, UUID> {

    @EntityGraph(attributePaths = {"team", "owner"})
    List<AiPlan> findByTeam_IdOrderByUpdatedAtDesc(UUID teamId);

    @EntityGraph(attributePaths = {"team", "owner"})
    List<AiPlan> findByOwner_IdOrderByUpdatedAtDesc(UUID ownerId);

    long countByTeam_IdAndStatus(UUID teamId, String status);
}