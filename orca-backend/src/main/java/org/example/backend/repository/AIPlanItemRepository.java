package org.example.backend.repository;

import org.example.backend.entity.AIPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIPlanItemRepository extends JpaRepository<AIPlanItem, UUID> {
    List<AIPlanItem> findByAiPlanId(UUID aiPlanId);
}
