package org.example.backend.service;

import org.example.backend.entity.Goal;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.Role;
import org.example.backend.entity.Task;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TaskService taskService;

    public AdminService(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            GoalRepository goalRepository,
            TaskRepository taskRepository,
            InterGroupOrderRepository orderRepository,
            TaskService taskService) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.orderRepository = orderRepository;
        this.taskService = taskService;
    }

    public Map<String, Object> getOverview() {
        List<User> users = userRepository.findAll();
        List<Team> teams = teamRepository.findAll();
        List<Goal> goals = goalRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<InterGroupOrder> orders = orderRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", users.size());
        overview.put("adminUsers", users.stream().filter(user -> user.getRole() == Role.ADMIN).count());
        overview.put("totalTeams", teams.size());
        overview.put("publishedTeams", teams.stream().filter(Team::isPublished).count());
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
        overview.put("orderStatusCounts", countByStatus(orders, InterGroupOrder::getStatus));
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
        map.put("completedOrders", team.getCompletedOrders());
        map.put("cancelledOrders", team.getCancelledOrders());
        map.put("totalOrders", team.getTotalOrders());
        map.put("trustScore", team.getTotalOrders() > 0
                ? (int) ((double) team.getCompletedOrders() / team.getTotalOrders() * 100)
                : 100);
        return map;
    }

    private Map<String, Object> toOrderMap(InterGroupOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId().toString());
        map.put("title", order.getTitle());
        map.put("description", safeText(order.getDescription(), ""));
        map.put("buyerTeamId", order.getBuyerTeam() != null ? order.getBuyerTeam().getId().toString() : "");
        map.put("buyerTeamName", order.getBuyerTeam() != null ? order.getBuyerTeam().getName() : "");
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

    private String safeText(String value, String fallback) {
        return value != null ? value : fallback;
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

    private int compareDateDesc(LocalDateTime left, LocalDateTime right) {
        return Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                .reversed()
                .compare(left, right);
    }
}
