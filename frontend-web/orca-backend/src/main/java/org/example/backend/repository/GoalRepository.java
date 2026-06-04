package org.example.backend.repository;

import org.example.backend.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByTeamId(UUID teamId);

    List<Goal> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Goal> findAllByOrderByCreatedAtDesc();
}
