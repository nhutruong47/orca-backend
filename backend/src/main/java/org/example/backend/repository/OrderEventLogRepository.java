package org.example.backend.repository;

import org.example.backend.entity.OrderEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderEventLogRepository extends JpaRepository<OrderEventLog, UUID> {
    List<OrderEventLog> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
