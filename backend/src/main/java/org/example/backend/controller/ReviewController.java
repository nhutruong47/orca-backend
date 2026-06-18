package org.example.backend.controller;

import org.example.backend.dto.ReviewDTO;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamRepository;
import org.example.backend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private TeamRepository teamRepo;

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getReviewsByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(reviewService.getReviewsByTeam(teamId));
    }

    @GetMapping("/team/{teamId}/summary")
    public ResponseEntity<?> getTeamReviewSummary(@PathVariable UUID teamId) {
        var teamOpt = teamRepo.findById(teamId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
        }
        var team = teamOpt.get();
        long reviewCount = reviewService.getReviewCount(teamId);
        double avgRating = team.getTotalRatings() > 0
                ? team.getSumRatings() / team.getTotalRatings()
                : 0.0;
        double onTimeRate = team.getTotalOrders() > 0
                ? (double) team.getOnTimeOrders() / team.getTotalOrders() * 100
                : 0.0;
        return ResponseEntity.ok(Map.of(
                "avgRating", Math.round(avgRating * 10.0) / 10.0,
                "reviewCount", reviewCount,
                "onTimeRate", Math.round(onTimeRate),
                "completedOrders", team.getCompletedOrders(),
                "totalOrders", team.getTotalOrders(),
                "onTimeOrders", team.getOnTimeOrders(),
                "lateOrders", team.getLateOrders(),
                "starCounts", reviewService.getStarCounts(teamId)
        ));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(@PathVariable UUID reviewId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User user) {
        try {
            int rating = ((Number) payload.get("rating")).intValue();
            String comment = (String) payload.getOrDefault("comment", "");
            String deliveryResult = (String) payload.get("deliveryResult");
            Integer qualityScore = payload.containsKey("qualityScore") ? ((Number) payload.get("qualityScore")).intValue() : null;
            Integer timeScore = payload.containsKey("timeScore") ? ((Number) payload.get("timeScore")).intValue() : null;
            Integer communicationScore = payload.containsKey("communicationScore") ? ((Number) payload.get("communicationScore")).intValue() : null;
            Integer supportScore = payload.containsKey("supportScore") ? ((Number) payload.get("supportScore")).intValue() : null;
            String replyText = (String) payload.get("replyText");

            return ResponseEntity.ok(reviewService.updateReview(reviewId, rating, comment, deliveryResult, qualityScore, timeScore, communicationScore, supportScore, replyText, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable UUID reviewId,
            @AuthenticationPrincipal User user) {
        try {
            reviewService.deleteReview(reviewId, user);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
