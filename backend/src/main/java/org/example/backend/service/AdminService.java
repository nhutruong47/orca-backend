package org.example.backend.service;

import org.example.backend.entity.Goal;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.PaymentTransaction;
import org.example.backend.entity.ProductionBatch;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.Role;
import org.example.backend.entity.Task;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.PaymentTransactionRepository;
import org.example.backend.repository.ProductionBatchRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final InterGroupOrderRepository orderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final TaskService taskService;

    public AdminService(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            GoalRepository goalRepository,
            TaskRepository taskRepository,
            InterGroupOrderRepository orderRepository,
            ProductionOrderRepository productionOrderRepository,
            ProductionBatchRepository productionBatchRepository,
            PaymentTransactionRepository paymentRepository,
            TaskService taskService) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.orderRepository = orderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.productionBatchRepository = productionBatchRepository;
        this.paymentRepository = paymentRepository;
        this.taskService = taskService;
    }

    public Map<String, Object> getOverview() {
        List<User> users = userRepository.findAll();
        List<Team> teams = teamRepository.findAll();
        List<Goal> goals = goalRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<InterGroupOrder> orders = orderRepository.findAll();
        List<ProductionOrder> productionOrders = productionOrderRepository.findAll();
        List<ProductionBatch> productionBatches = productionBatchRepository.findAll();
        List<PaymentTransaction> payments = paymentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = monthStart.minusMonths(1);
        LocalDateTime yearStart = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
        LocalDateTime previousYearStart = yearStart.minusYears(1);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", users.size());
        overview.put("adminUsers", users.stream().filter(user -> user.getRole() == Role.ADMIN).count());
        overview.put("memberUsers", users.stream().filter(user -> user.getRole() != Role.ADMIN).count());
        overview.put("newUsersThisMonth", countCreatedBetweenUsers(users, monthStart, now));
        overview.put("newUsersPreviousMonth", countCreatedBetweenUsers(users, previousMonthStart, monthStart));
        overview.put("totalTeams", teams.size());
        overview.put("publishedTeams", teams.stream().filter(Team::isPublished).count());
        overview.put("newTeamsThisMonth", countCreatedBetweenTeams(teams, monthStart, now));
        overview.put("newTeamsPreviousMonth", countCreatedBetweenTeams(teams, previousMonthStart, monthStart));
        overview.put("totalGoals", goals.size());
        overview.put("activeGoals", goals.stream().filter(goal -> !"DONE".equals(goal.getStatus())).count());
        overview.put("totalTasks", tasks.size());
        overview.put("completedTasks", tasks.stream().filter(task -> "COMPLETED".equals(task.getStatus())).count());
        overview.put("overdueTasks", tasks.stream()
                .filter(task -> task.getDeadline() != null
                        && task.getDeadline().isBefore(now)
                        && !"COMPLETED".equals(task.getStatus()))
                .count());
        overview.put("totalOrders", orders.size());
        overview.put("activeOrders", orders.stream()
                .filter(order -> "PENDING".equals(order.getStatus()) || "ACCEPTED".equals(order.getStatus()))
                .count());
        overview.put("totalProductionOrders", productionOrders.size());
        overview.put("activeProductionOrders", productionOrders.stream()
                .filter(order -> isActiveStatus(order.getStatus()))
                .count());
        overview.put("overdueProductionOrders", productionOrders.stream()
                .filter(order -> order.getDeadline() != null
                        && order.getDeadline().isBefore(now)
                        && isActiveStatus(order.getStatus()))
                .count());
        overview.put("totalBatches", productionBatches.size());
        overview.put("activeBatches", productionBatches.stream()
                .filter(batch -> isActiveStatus(batch.getStatus()))
                .count());
        overview.put("completedBatches", productionBatches.stream()
                .filter(batch -> isCompletedStatus(batch.getStatus()))
                .count());
        overview.put("paidPayments", payments.stream().filter(this::isPaidPayment).count());
        overview.put("totalPayments", payments.size());
        overview.put("revenueThisMonth", sumPaidBetween(payments, monthStart, now));
        overview.put("revenuePreviousMonth", sumPaidBetween(payments, previousMonthStart, monthStart));
        overview.put("revenueThisYear", sumPaidBetween(payments, yearStart, now));
        overview.put("revenuePreviousYear", sumPaidBetween(payments, previousYearStart, yearStart));
        overview.put("revenueTotal", payments.stream()
                .filter(this::isPaidPayment)
                .mapToLong(PaymentTransaction::getAmount)
                .sum());
        overview.put("orderStatusCounts", countByStatus(orders, InterGroupOrder::getStatus));
        overview.put("productionOrderStatusCounts", countByStatus(productionOrders, ProductionOrder::getStatus));
        overview.put("batchStatusCounts", countByStatus(productionBatches, ProductionBatch::getStatus));
        overview.put("taskStatusCounts", countByStatus(tasks, Task::getStatus));
        overview.put("recentUsers", users.stream()
                .sorted(this::compareCreatedAtDesc)
                .limit(5)
                .map(this::toUserMap)
                .toList());
        overview.put("recentTeams", teams.stream()
                .sorted(this::compareCreatedAtDesc)
                .limit(5)
                .map(this::toTeamMap)
                .toList());

        return overview;
    }

    private long countCreatedBetweenUsers(List<User> users, LocalDateTime start, LocalDateTime end) {
        return users.stream().filter(user -> isBetween(user.getCreatedAt(), start, end)).count();
    }

    private long countCreatedBetweenTeams(List<Team> teams, LocalDateTime start, LocalDateTime end) {
        return teams.stream().filter(team -> isBetween(team.getCreatedAt(), start, end)).count();
    }

    private long sumPaidBetween(List<PaymentTransaction> payments, LocalDateTime start, LocalDateTime end) {
        return payments.stream()
                .filter(this::isPaidPayment)
                .filter(payment -> isBetween(payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(), start, end))
                .mapToLong(PaymentTransaction::getAmount)
                .sum();
    }

    private boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private boolean isPaidPayment(PaymentTransaction payment) {
        return "PAID".equalsIgnoreCase(safeText(payment.getStatus(), ""));
    }

    private boolean isActiveStatus(String status) {
        String normalized = safeText(status, "").toUpperCase(Locale.ROOT);
        return !List.of("COMPLETED", "COMPLETE", "DONE", "CANCELLED", "CANCELED", "REJECTED").contains(normalized);
    }

    private boolean isCompletedStatus(String status) {
        String normalized = safeText(status, "").toUpperCase(Locale.ROOT);
        return List.of("COMPLETED", "COMPLETE", "DONE").contains(normalized);
    }

    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll().stream()
                .sorted(this::compareCreatedAtDesc)
                .map(this::toUserMap)
                .toList();
    }

    public List<Map<String, Object>> getTeams() {
        return teamRepository.findAll().stream()
                .sorted(this::compareCreatedAtDesc)
                .map(this::toTeamMap)
                .toList();
    }

    public List<Map<String, Object>> getOrders() {
        return orderRepository.findAll().stream()
                .sorted(this::compareCreatedAtDesc)
                .map(this::toOrderMap)
                .toList();
    }

    public List<Map<String, Object>> getTasks() {
        return taskRepository.findAll().stream()
                .sorted(this::compareCreatedAtDesc)
                .map(this::toTaskMap)
                .toList();
    }

    public List<Map<String, Object>> getPayments() {
        return paymentRepository.findAll().stream()
                .sorted(this::comparePaidAtDesc)
                .map(this::toPaymentMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateUserRole(UUID userId, String role, User currentUser) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role is required");
        }

        Role nextRole;
        try {
            nextRole = Role.valueOf(role.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Role is invalid");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (target.getId().equals(currentUser.getId()) && nextRole != Role.ADMIN) {
            throw new RuntimeException("You cannot remove your own admin access");
        }

        long adminCount = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();
        if (target.getRole() == Role.ADMIN && nextRole != Role.ADMIN && adminCount <= 1) {
            throw new RuntimeException("At least one admin account is required");
        }

        target.setRole(nextRole);
        return toUserMap(userRepository.save(target));
    }

    @Transactional
    public Map<String, Object> updateTeamPublication(UUID teamId, boolean published) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setPublished(published);
        return toTeamMap(teamRepository.save(team));
    }

    @Transactional
    public Map<String, Object> updateTeamVerification(UUID teamId, String status, String rejectReason) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Verification status is required");
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new RuntimeException("Verification status is invalid");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setVerificationStatus(normalized);
        team.setVerificationRejectReason("REJECTED".equals(normalized) ? safeText(rejectReason, "") : "");
        return toTeamMap(teamRepository.save(team));
    }

    @Transactional
    public Map<String, Object> updateTaskStatus(UUID taskId, String status) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Status is required");
        }

        String normalized = status.toUpperCase(Locale.ROOT);
        if (!List.of("PENDING", "IN_PROGRESS", "COMPLETED").contains(normalized)) {
            throw new RuntimeException("Task status is invalid");
        }

        taskService.updateStatus(taskId, normalized);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        return toTaskMap(task);
    }

    private <T> Map<String, Long> countByStatus(List<T> items, Function<T, String> statusGetter) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> safeText(statusGetter.apply(item), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId().toString());
        map.put("username", user.getUsername());
        map.put("fullName", safeText(user.getFullName(), ""));
        map.put("email", safeText(user.getEmail(), ""));
        map.put("role", user.getRole().name());
        map.put("chipId", safeText(user.getChipId(), ""));
        map.put("aiPlan", safeText(user.getAiPlan(), "free"));
        map.put("aiPlanExpiresAt", user.getAiPlanExpiresAt() != null ? user.getAiPlanExpiresAt().toString() : null);
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return map;
    }

    private Map<String, Object> toTeamMap(Team team) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", team.getId().toString());
        map.put("name", team.getName());
        map.put("description", safeText(team.getDescription(), ""));
        map.put("ownerId", team.getOwner() != null ? team.getOwner().getId().toString() : "");
        map.put("ownerName", team.getOwner() != null ? team.getOwner().getUsername() : "");
        map.put("memberCount", teamMemberRepository.findByTeamId(team.getId()).size());
        map.put("createdAt", team.getCreatedAt() != null ? team.getCreatedAt().toString() : null);
        map.put("published", team.isPublished());
        map.put("specialty", safeText(team.getSpecialty(), ""));
        map.put("capacity", safeText(team.getCapacity(), ""));
        map.put("region", safeText(team.getRegion(), ""));
        map.put("factoryType", safeText(team.getFactoryType(), ""));
        map.put("capacityValue", team.getCapacityValue());
        map.put("capacityUnit", safeText(team.getCapacityUnit(), ""));
        map.put("factoryImageUrl", safeText(team.getFactoryImageUrl(), ""));
        map.put("factoryImages", splitList(team.getFactoryImages()));
        map.put("verificationStatus", safeText(team.getVerificationStatus(), "NOT_SUBMITTED"));
        map.put("businessLicense", safeText(team.getBusinessLicense(), ""));
        map.put("businessAddress", safeText(team.getBusinessAddress(), ""));
        map.put("websiteUrl", safeText(team.getWebsiteUrl(), ""));
        map.put("facebookUrl", safeText(team.getFacebookUrl(), ""));
        map.put("certificates", splitList(team.getCertificates()));
        map.put("certificationDocument", safeText(team.getCertificationDocument(), ""));
        map.put("verificationRejectReason", safeText(team.getVerificationRejectReason(), ""));
        map.put("completedOrders", team.getCompletedOrders());
        map.put("cancelledOrders", team.getCancelledOrders());
        map.put("totalOrders", team.getTotalOrders());
        map.put("trustScore", team.getTotalOrders() > 0
                ? (int) ((double) team.getCompletedOrders() / team.getTotalOrders() * 100)
                : 0);
        return map;
    }

    private Map<String, Object> toOrderMap(InterGroupOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId().toString());
        map.put("title", order.getTitle());
        map.put("description", safeText(order.getDescription(), ""));
        map.put("buyerTeamId", order.getBuyerTeam() != null ? order.getBuyerTeam().getId().toString() : "");
        String buyerUserName = order.getBuyerUser() != null
                ? safeText(order.getBuyerUser().getFullName(), order.getBuyerUser().getUsername())
                : "";
        map.put("buyerTeamName", order.getBuyerTeam() != null ? order.getBuyerTeam().getName() : buyerUserName);
        map.put("buyerUserId", order.getBuyerUser() != null ? order.getBuyerUser().getId().toString() : "");
        map.put("buyerUserName", buyerUserName);
        map.put("sellerTeamId", order.getSellerTeam() != null ? order.getSellerTeam().getId().toString() : "");
        map.put("sellerTeamName", order.getSellerTeam() != null ? order.getSellerTeam().getName() : "");
        map.put("quantity", order.getQuantity());
        map.put("deadline", order.getDeadline() != null ? order.getDeadline().toString() : null);
        map.put("status", order.getStatus());
        map.put("linkedGoalId", order.getLinkedGoalId() != null ? order.getLinkedGoalId().toString() : null);
        map.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        map.put("cancelledBy", safeText(order.getCancelledBy(), ""));
        return map;
    }

    private Map<String, Object> toTaskMap(Task task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId().toString());
        map.put("title", task.getTitle());
        map.put("description", safeText(task.getDescription(), ""));
        map.put("goalId", task.getGoal() != null ? task.getGoal().getId().toString() : "");
        map.put("goalTitle", task.getGoal() != null ? task.getGoal().getTitle() : "");
        map.put("teamId", task.getGoal() != null && task.getGoal().getTeam() != null
                ? task.getGoal().getTeam().getId().toString()
                : "");
        map.put("teamName", task.getGoal() != null && task.getGoal().getTeam() != null
                ? task.getGoal().getTeam().getName()
                : "");
        map.put("memberId", task.getMember() != null ? task.getMember().getId().toString() : "");
        map.put("memberName", task.getMember() != null ? task.getMember().getUsername() : "");
        map.put("priority", task.getPriority());
        map.put("status", task.getStatus());
        map.put("acceptanceStatus", task.getAcceptanceStatus());
        map.put("completionPercentage", task.getCompletionPercentage() != null ? task.getCompletionPercentage() : 0);
        map.put("deadline", task.getDeadline() != null ? task.getDeadline().toString() : null);
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        return map;
    }

    private Map<String, Object> toPaymentMap(PaymentTransaction payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId().toString());
        map.put("txnRef", payment.getTxnRef());
        map.put("userId", payment.getUser() != null ? payment.getUser().getId().toString() : "");
        map.put("username", payment.getUser() != null ? payment.getUser().getUsername() : "");
        map.put("fullName", payment.getUser() != null ? safeText(payment.getUser().getFullName(), "") : "");
        map.put("email", payment.getUser() != null ? safeText(payment.getUser().getEmail(), "") : "");
        map.put("planId", payment.getPlanId());
        map.put("amount", payment.getAmount());
        map.put("status", payment.getStatus());
        map.put("bankCode", safeText(payment.getBankCode(), ""));
        map.put("paymentMethod", safeText(payment.getPaymentMethod(), ""));
        map.put("createdAt", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
        map.put("paidAt", payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
        return map;
    }

    private String safeText(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private int compareCreatedAtDesc(User left, User right) {
        return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
    }

    private int compareCreatedAtDesc(Team left, Team right) {
        return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
    }

    private int compareCreatedAtDesc(InterGroupOrder left, InterGroupOrder right) {
        return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
    }

    private int compareCreatedAtDesc(Task left, Task right) {
        return compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
    }

    private int comparePaidAtDesc(PaymentTransaction left, PaymentTransaction right) {
        LocalDateTime leftDate = left.getPaidAt() != null ? left.getPaidAt() : left.getCreatedAt();
        LocalDateTime rightDate = right.getPaidAt() != null ? right.getPaidAt() : right.getCreatedAt();
        return compareDateDesc(leftDate, rightDate);
    }

    private int compareDateDesc(LocalDateTime left, LocalDateTime right) {
        return Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                .reversed()
                .compare(left, right);
    }
}
