package org.example.backend.repository;

import org.example.backend.entity.AIPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIPlanRepository extends JpaRepository<AIPlan, UUID> {
    List<AIPlan> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}
