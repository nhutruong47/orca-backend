package org.example.backend.service;

import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralized service for AI usage enforcement.
 * Removes duplicate limit-checking logic previously spread across
 * AiController and AiWorkflowController.
 */
@Service
@Transactional
public class AiUsageService {

    private static final int FREE_PLAN_LIMIT = 10;
    private static final int PROFESSIONAL_PLAN_LIMIT = 100;
    private static final int ENTERPRISE_PLAN_LIMIT = Integer.MAX_VALUE;
    private static final String ENTERPRISE_PLAN = "enterprise";
    private static final String PROFESSIONAL_PLAN = "professional";
    private static final String PLUS_PLAN = "plus";

    private final UserRepository userRepository;

    public AiUsageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Validates the user's trial status and atomically increments the AI usage counter.
     * Throws 402 PAYMENT_REQUIRED when the limit is exceeded or the trial is no longer active.
     */
    public void enforceAndIncrementUsage(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!user.isAiTrialActive()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Hết hạn gói miễn phí. Bạn cần nâng cấp gói để sử dụng tốt hơn.");
        }

        int limit = resolveLimit(user.getAiPlan());
        int updated = userRepository.incrementAiUsageIfUnderLimit(user.getId(), limit);
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Bạn đã đạt giới hạn sử dụng AI. Vui lòng nâng cấp gói dịch vụ.");
        }
    }

    private int resolveLimit(String plan) {
        if (plan == null) {
            return FREE_PLAN_LIMIT;
        }
        if (ENTERPRISE_PLAN.equalsIgnoreCase(plan)) {
            return ENTERPRISE_PLAN_LIMIT;
        }
        if (PROFESSIONAL_PLAN.equalsIgnoreCase(plan) || PLUS_PLAN.equalsIgnoreCase(plan)) {
            return PROFESSIONAL_PLAN_LIMIT;
        }
        return FREE_PLAN_LIMIT;
    }
}
