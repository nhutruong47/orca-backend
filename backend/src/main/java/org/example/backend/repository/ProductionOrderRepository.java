package org.example.backend.repository;

import org.example.backend.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, UUID> {
    List<ProductionOrder> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    List<ProductionOrder> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, String status);

    @Query("SELECT o FROM ProductionOrder o WHERE o.team.id = :teamId " +
           "AND o.status IN :statuses ORDER BY o.internalDeadline ASC")
    List<ProductionOrder> findByTeamIdAndStatusInOrderByDeadline(
            @Param("teamId") UUID teamId,
            @Param("statuses") List<String> statuses);

    @Query("SELECT o FROM ProductionOrder o WHERE o.team.id = :teamId " +
           "AND o.internalDeadline < :now " +
           "AND o.status NOT IN ('COMPLETED', 'DELIVERED', 'CANCELLED')")
    List<ProductionOrder> findOverdueOrders(@Param("teamId") UUID teamId, @Param("now") LocalDateTime now);

    long countByTeamIdAndStatus(UUID teamId, String status);
}
