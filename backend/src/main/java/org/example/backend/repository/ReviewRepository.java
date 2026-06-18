package org.example.backend.repository;

import org.example.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findBySellerTeamIdOrderByCreatedAtDesc(UUID sellerTeamId);
    long countBySellerTeamId(UUID sellerTeamId);
    long countBySellerTeamIdAndRating(UUID sellerTeamId, int rating);
    boolean existsByOrderId(UUID orderId);
    Optional<Review> findByOrderId(UUID orderId);
    Optional<Review> findByOrderIdAndBuyerUserId(UUID orderId, UUID buyerUserId);
}
