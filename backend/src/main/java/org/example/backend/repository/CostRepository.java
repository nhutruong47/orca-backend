package org.example.backend.repository;

import org.example.backend.entity.Cost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CostRepository extends JpaRepository<Cost, UUID> {

    @Query("SELECT c FROM Cost c WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.payer) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:status IS NULL OR :status = '' OR c.status = :status) " +
           "ORDER BY c.date DESC")
    Page<Cost> searchCosts(@Param("search") String search, 
                           @Param("categoryId") UUID categoryId, 
                           @Param("status") String status, 
                           Pageable pageable);

    @Query("SELECT c FROM Cost c WHERE c.date >= :startDate AND c.date <= :endDate")
    List<Cost> findByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
