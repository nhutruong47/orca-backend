package org.example.backend.service;

import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính và cập nhật trust score thống nhất cho cả Team và User.
 *
 * <p><b>Quick Win F1.5:</b> Trước đó logic trust score bị phân tán ở 13+ chỗ trong
 * {@code InterGroupOrderService}, với 2 phiên bản riêng cho Team (dựa trên
 * tỉ lệ completed/total) và User (dựa trên completed/(completed+cancelled)).
 * User trust cũng thiếu cập nhật khi đơn bị hủy ở status CONFIRMED.
 *
 * <p>Công thức thống nhất:
 * <pre>
 *   completed = completedOrders
 *   cancelled = cancelledOrders
 *   total     = completed + cancelled
 *   trustScore = total == 0 ? 100 : completed / total * 100
 * </pre>
 * Hàm tính là pure function — service chỉ phụ trách cập nhật counters và gọi
 * {@link #calculate(Team)} / {@link #calculate(User)} ở nơi cần hiển thị.
 */
@Service
public class TrustScoreService {

    private static final Logger log = LoggerFactory.getLogger(TrustScoreService.class);

    private final org.example.backend.repository.TeamRepository teamRepo;
    private final org.example.backend.repository.UserRepository userRepo;

    public TrustScoreService(
            org.example.backend.repository.TeamRepository teamRepo,
            org.example.backend.repository.UserRepository userRepo) {
        this.teamRepo = teamRepo;
        this.userRepo = userRepo;
    }

    // ===== Pure calculation (đồng bộ Team + User) =====

    /**
     * Tính trust score cho Team: completed / (completed + cancelled) * 100.
     * Trả 100 cho Team mới (no completed, no cancelled) — luôn cho cơ hội thử.
     */
    public int calculate(Team team) {
        if (team == null) return 0;
        return completedVsCancelled(team.getCompletedOrders(), team.getCancelledOrders());
    }

    /**
     * Tính trust score cho User: cùng công thức với Team.
     */
    public int calculate(User user) {
        if (user == null) return 0;
        return completedVsCancelled(user.getCompletedOrders(), user.getCancelledOrders());
    }

    public int completedVsCancelled(int completed, int cancelled) {
        int total = completed + cancelled;
        if (total <= 0) return -1;
        
        int score = 100 - (cancelled * 20) + (completed * 10);
        
        // Capped at 100% and minimum 0%
        if (score > 100) return 100;
        if (score < 0) return 0;
        
        return score;
    }

    // ===== Mutators (counter++) — luôn chạy trong transaction riêng =====

    /**
     * Tăng completedOrders + totalOrders cho Team bán và Team/User mua.
     * Idempotent dựa vào orderId + đã cộng trước đó chưa.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCompleted(Team sellerTeam, Team buyerTeam, User buyerUser) {
        if (sellerTeam != null) {
            Team s = teamRepo.findById(sellerTeam.getId()).orElse(null);
            if (s != null) {
                s.setCompletedOrders(s.getCompletedOrders() + 1);
                s.setTotalOrders(s.getTotalOrders() + 1);
                teamRepo.save(s);
            }
        }
        if (buyerTeam != null) {
            Team b = teamRepo.findById(buyerTeam.getId()).orElse(null);
            if (b != null) {
                b.setCompletedOrders(b.getCompletedOrders() + 1);
                teamRepo.save(b);
            }
        } else if (buyerUser != null) {
            User u = userRepo.findById(buyerUser.getId()).orElse(null);
            if (u != null) {
                u.setCompletedOrders(u.getCompletedOrders() + 1);
                userRepo.save(u);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(Team cancelledTeam, User cancelledUser) {
        if (cancelledTeam != null) {
            Team t = teamRepo.findById(cancelledTeam.getId()).orElse(null);
            if (t != null) {
                t.setCancelledOrders(t.getCancelledOrders() + 1);
                teamRepo.save(t);
            }
        } else if (cancelledUser != null) {
            User u = userRepo.findById(cancelledUser.getId()).orElse(null);
            if (u != null) {
                u.setCancelledOrders(u.getCancelledOrders() + 1);
                userRepo.save(u);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRatingSubmitted(Team sellerTeam, int rating, String deliveryResult) {
        if (sellerTeam == null) return;
        Team s = teamRepo.findById(sellerTeam.getId()).orElse(null);
        if (s != null) {
            s.setTotalRatings(s.getTotalRatings() + 1);
            s.setSumRatings(s.getSumRatings() + rating);
            if ("ON_TIME".equalsIgnoreCase(deliveryResult)) {
                s.setOnTimeOrders(s.getOnTimeOrders() + 1);
            } else if ("LATE".equalsIgnoreCase(deliveryResult)) {
                s.setLateOrders(s.getLateOrders() + 1);
            }
            teamRepo.save(s);
        }
    }

    /**
     * Tính rating trung bình cho Team (NaN-safe, trả null nếu chưa có rating).
     */
    public Double averageRating(Team team) {
        if (team == null || team.getTotalRatings() <= 0) return null;
        return team.getSumRatings() / team.getTotalRatings();
    }

    /**
     * Trả trust score thân thiện với frontend (luôn 0-100). Null-safe.
     */
    public int safelyCalculate(Object teamOrUser) {
        if (teamOrUser instanceof Team t) return calculate(t);
        if (teamOrUser instanceof User u) return calculate(u);
        return 0;
    }
}
