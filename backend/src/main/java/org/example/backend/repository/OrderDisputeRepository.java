package org.example.backend.repository;

import org.example.backend.entity.OrderDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDisputeRepository extends JpaRepository<OrderDispute, UUID> {
    List<OrderDispute> findByOrderId(UUID orderId);
}
