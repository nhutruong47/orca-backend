package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.PlanUsageDTO;
import org.example.backend.entity.SubscriptionPlan;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.exception.PlanLimitExceededException;
import org.example.backend.repository.SubscriptionPlanRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanQuotaService {

    private static final PlanLimits FREE = new PlanLimits("free", "Free", 3, 1, List.of("Free"));
    private static final PlanLimits PLUS = new PlanLimits("plus", "Plus", 30, 5,
            List.of("Plus", "Professional", "Chuyên nghiệp", "Tăng trưởng"));
    private static final PlanLimits ENTERPRISE = new PlanLimits("enterprise", "Doanh nghiệp", 500, 50,
            List.of("Doanh nghiệp", "Enterprise"));

    private final SubscriptionPlanRepository planRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public PlanUsageDTO getUsage(User owner) {
        PlanLimits limits = resolveLimits(owner);
        long workshopsUsed = teamRepository.countByOwnerId(owner.getId());
        long usersUsed = teamMemberRepository.countDistinctUsersByTeamOwnerId(owner.getId());
        return new PlanUsageDTO(
                limits.id(),
                limits.name(),
                usersUsed,
                limits.maxUsers(),
                workshopsUsed,
                limits.maxWorkshops(),
                usersUsed < limits.maxUsers(),
                workshopsUsed < limits.maxWorkshops());
    }

    public PlanUsageDTO getUsage(Team team) {
        return getUsage(team.getOwner());
    }

    public void requireWorkshopSlot(User owner) {
        PlanUsageDTO usage = getUsage(owner);
        if (!usage.canCreateWorkshop()) {
            throw new PlanLimitExceededException(
                    "WORKSHOPS",
                    usage.workshopsUsed(),
                    usage.maxWorkshops(),
                    "Gói " + usage.planName() + " chỉ cho phép tối đa " + usage.maxWorkshops()
                            + " xưởng. Vui lòng nâng cấp gói để tạo thêm xưởng.");
        }
    }

    public void requireMemberCapacity(Team team) {
        PlanUsageDTO usage = getUsage(team);
        if (!usage.canAddMember()) {
            throw memberLimitExceeded(usage);
        }
    }

    public void requireMemberSlot(Team team, User candidate) {
        User owner = team.getOwner();
        boolean alreadyUsesSeat = teamMemberRepository
                .existsByUserIdAndTeamOwnerId(candidate.getId(), owner.getId());
        if (alreadyUsesSeat) {
            return;
        }
        requireMemberCapacity(team);
    }

    private PlanLimitExceededException memberLimitExceeded(PlanUsageDTO usage) {
        return new PlanLimitExceededException(
                "USERS",
                usage.usersUsed(),
                usage.maxUsers(),
                "Gói " + usage.planName() + " chỉ cho phép tối đa " + usage.maxUsers()
                        + " nhân viên, tính cả chủ xưởng và quản trị viên. Vui lòng nâng cấp gói để thêm người mới.");
    }

    private PlanLimits resolveLimits(User owner) {
        String planId = normalizePlanId(owner);
        PlanLimits defaults = switch (planId) {
            case "plus" -> PLUS;
            case "enterprise" -> ENTERPRISE;
            default -> FREE;
        };

        Optional<SubscriptionPlan> configured = defaults.aliases().stream()
                .map(planRepository::findByNameIgnoreCase)
                .flatMap(Optional::stream)
                .findFirst();

        if (configured.isEmpty()) {
            return defaults;
        }

        SubscriptionPlan plan = configured.get();
        int maxUsers = positiveOrDefault(plan.getMaxUsers(), defaults.maxUsers());
        int maxWorkshops = positiveOrDefault(plan.getMaxWorkshops(), defaults.maxWorkshops());
        String name = plan.getName() == null || plan.getName().isBlank() ? defaults.name() : plan.getName();
        return new PlanLimits(defaults.id(), name, maxUsers, maxWorkshops, defaults.aliases());
    }

    private String normalizePlanId(User owner) {
        String planId = owner.getAiPlan() == null
                ? "free"
                : owner.getAiPlan().trim().toLowerCase(Locale.ROOT);
        if ("professional".equals(planId)) {
            planId = "plus";
        }
        if ("enterprise".equals(planId)
                && owner.getAiPlanExpiresAt() != null
                && !LocalDateTime.now().isBefore(owner.getAiPlanExpiresAt())) {
            return "free";
        }
        return planId;
    }

    private int positiveOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private record PlanLimits(
            String id,
            String name,
            int maxUsers,
            int maxWorkshops,
            List<String> aliases) {
    }
}
