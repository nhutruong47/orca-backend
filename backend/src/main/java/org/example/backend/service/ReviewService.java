package org.example.backend.service;

import org.example.backend.dto.ReviewDTO;
import org.example.backend.entity.Review;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.ReviewRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final TeamRepository teamRepo;

    public ReviewService(ReviewRepository reviewRepo, TeamRepository teamRepo) {
        this.reviewRepo = reviewRepo;
        this.teamRepo = teamRepo;
    }

    public List<ReviewDTO> getReviewsByTeam(UUID teamId) {
        return reviewRepo.findBySellerTeamIdOrderByCreatedAtDesc(teamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public long getReviewCount(UUID teamId) {
        return reviewRepo.countBySellerTeamId(teamId);
    }

    public Map<Integer, Long> getStarCounts(UUID teamId) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            counts.put(star, reviewRepo.countBySellerTeamIdAndRating(teamId, star));
        }
        return counts;
    }

    @Transactional
    public ReviewDTO updateReview(UUID reviewId, int rating, String comment, String deliveryResult, User currentUser) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        validateReviewOwner(review, currentUser);
        validateRating(rating);
        validateDeliveryResult(deliveryResult);

        int oldRating = review.getRating();
        String oldDeliveryResult = review.getDeliveryResult();
        review.setRating(rating);
        review.setComment(comment);
        review.setDeliveryResult(deliveryResult);
        if (review.getOrder() != null) {
            review.getOrder().setDeliveryStatus(deliveryResult);
        }

        Team sellerTeam = review.getSellerTeam();
        sellerTeam.setSumRatings(Math.max(0.0, sellerTeam.getSumRatings() + rating - oldRating));
        adjustDeliveryStats(sellerTeam, oldDeliveryResult, deliveryResult);
        teamRepo.save(sellerTeam);

        return toDTO(reviewRepo.save(review));
    }

    @Transactional
    public void deleteReview(UUID reviewId, User currentUser) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        validateReviewOwner(review, currentUser);

        Team sellerTeam = review.getSellerTeam();
        sellerTeam.setTotalRatings(Math.max(0, sellerTeam.getTotalRatings() - 1));
        sellerTeam.setSumRatings(Math.max(0.0, sellerTeam.getSumRatings() - review.getRating()));
        teamRepo.save(sellerTeam);
        reviewRepo.delete(review);
    }

    private void validateReviewOwner(Review review, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Bạn cần đăng nhập để thao tác đánh giá.");
        }
        boolean isBuyerUser = review.getBuyerUser() != null
                && review.getBuyerUser().getId().equals(currentUser.getId());
        boolean isBuyerTeamOwner = review.getBuyerTeam() != null
                && review.getBuyerTeam().getOwner() != null
                && review.getBuyerTeam().getOwner().getId().equals(currentUser.getId());
        if (!isBuyerUser && !isBuyerTeamOwner) {
            throw new RuntimeException("Bạn chỉ được sửa hoặc xóa đánh giá của mình.");
        }
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá phải từ 1 đến 5 sao.");
        }
    }

    private void validateDeliveryResult(String deliveryResult) {
        if (!"ON_TIME".equals(deliveryResult) && !"LATE".equals(deliveryResult) && !"NOT_DELIVERED".equals(deliveryResult)) {
            throw new RuntimeException("Trạng thái giao hàng không hợp lệ.");
        }
    }

    private void adjustDeliveryStats(Team team, String oldResult, String newResult) {
        if (oldResult != null && oldResult.equals(newResult)) return;
        if ("ON_TIME".equals(oldResult)) {
            team.setOnTimeOrders(Math.max(0, team.getOnTimeOrders() - 1));
        } else if ("LATE".equals(oldResult)) {
            team.setLateOrders(Math.max(0, team.getLateOrders() - 1));
        }
        if ("ON_TIME".equals(newResult)) {
            team.setOnTimeOrders(team.getOnTimeOrders() + 1);
        } else if ("LATE".equals(newResult)) {
            team.setLateOrders(team.getLateOrders() + 1);
        }
    }

    public ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId().toString());
        dto.setOrderId(r.getOrder().getId().toString());
        dto.setBuyerTeamId(r.getBuyerTeam() != null ? r.getBuyerTeam().getId().toString() : null);
        dto.setBuyerUserId(r.getBuyerUser() != null ? r.getBuyerUser().getId().toString() : null);
        dto.setSellerTeamId(r.getSellerTeam().getId().toString());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setDeliveryResult(r.getDeliveryResult());
        dto.setCreatedAt(r.getCreatedAt());
        if (r.getBuyerTeam() != null) dto.setBuyerTeamName(r.getBuyerTeam().getName());
        if (r.getBuyerUser() != null) {
            String name = r.getBuyerUser().getFullName();
            dto.setBuyerUserName(name != null && !name.isBlank() ? name : r.getBuyerUser().getUsername());
        }
        return dto;
    }
}
