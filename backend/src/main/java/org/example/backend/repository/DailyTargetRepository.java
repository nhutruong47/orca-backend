package org.example.backend.repository;

import org.example.backend.entity.DailyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyTargetRepository extends JpaRepository<DailyTarget, UUID> {
    List<DailyTarget> findByPlanIdOrderByTargetDateAsc(UUID planId);

    List<DailyTarget> findByOrderIdOrderByTargetDateAsc(UUID orderId);

    Optional<DailyTarget> findByOrderIdAndTargetDate(UUID orderId, LocalDate targetDate);

    @Query("SELECT d FROM DailyTarget d WHERE d.order.team.id = :teamId AND d.targetDate = :date")
    Optional<DailyTarget> findByTeamIdAndDate(@Param("teamId") UUID teamId, @Param("date") LocalDate date);

    @Query("SELECT d FROM DailyTarget d WHERE d.order.id = :orderId AND d.targetDate BETWEEN :start AND :end ORDER BY d.targetDate ASC")
    List<DailyTarget> findByOrderIdAndDateRange(
            @Param("orderId") UUID orderId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
