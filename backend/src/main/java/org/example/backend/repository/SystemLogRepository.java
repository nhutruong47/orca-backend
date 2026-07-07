package org.example.backend.repository;

import org.example.backend.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {
    
    @Query("SELECT s FROM SystemLog s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.actionType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.actorName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.details) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SystemLog> searchLogs(@Param("search") String search, Pageable pageable);
}
