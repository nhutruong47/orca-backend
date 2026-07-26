package org.example.backend.repository;

import org.example.backend.entity.OrderProofImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderProofImageRepository extends JpaRepository<OrderProofImage, UUID> {
    List<OrderProofImage> findByOrderId(UUID orderId);
}
