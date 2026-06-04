package org.example.backend.repository;

import org.example.backend.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByTeamIdOrderByLastUpdatedDesc(UUID teamId);
}
