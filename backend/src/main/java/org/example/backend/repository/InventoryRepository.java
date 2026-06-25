package org.example.backend.repository;

import org.example.backend.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByTeamIdOrderByLastUpdatedDesc(UUID teamId);

    List<InventoryItem> findByTeamIdOrderByProductTypeAscProductStateAsc(UUID teamId);

    Optional<InventoryItem> findByTeamIdAndProductTypeAndProductState(UUID teamId, String productType, String productState);

    List<InventoryItem> findByTeamIdAndProductType(UUID teamId, String productType);

    List<InventoryItem> findByTeamIdAndProductState(UUID teamId, String productState);

    List<InventoryItem> findByIsFeaturedTrue();
}
