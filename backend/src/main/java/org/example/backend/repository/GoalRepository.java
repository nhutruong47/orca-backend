package org.example.backend.repository;

import org.example.backend.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    @EntityGraph(attributePaths = {"team", "owner"})
    List<Goal> findByTeamId(UUID teamId);

    @EntityGraph(attributePaths = {"team", "owner"})
    List<Goal> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"team", "owner"})
    List<Goal> findAllByOrderByCreatedAtDesc();

}
