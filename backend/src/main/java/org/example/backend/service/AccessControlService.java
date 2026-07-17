package org.example.backend.service;

import org.example.backend.entity.GroupRole;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.Role;
import org.example.backend.entity.Task;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.AttendanceRepository;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.TaskChecklistRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.ProductionPlanRepository;
import org.example.backend.repository.DailyTargetRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccessControlService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final InterGroupOrderRepository interGroupOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final TaskChecklistRepository taskChecklistRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final DailyTargetRepository dailyTargetRepository;

    public AccessControlService(
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            ProductionOrderRepository productionOrderRepository,
            InterGroupOrderRepository interGroupOrderRepository,
            InventoryRepository inventoryRepository,
            GoalRepository goalRepository,
            TaskRepository taskRepository,
            TaskChecklistRepository taskChecklistRepository,
            AttendanceRepository attendanceRepository,
            ProductionPlanRepository productionPlanRepository,
            DailyTargetRepository dailyTargetRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.interGroupOrderRepository = interGroupOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.taskChecklistRepository = taskChecklistRepository;
        this.attendanceRepository = attendanceRepository;
        this.productionPlanRepository = productionPlanRepository;
        this.dailyTargetRepository = dailyTargetRepository;
    }

    public void requireTeamMember(User user, UUID teamId) {
        requireTeamMember(requireUserId(user), teamId);
    }

    public void requireTeamMember(UUID userId, UUID teamId) {
        if (teamId == null || userId == null) {
            throw forbidden();
        }
        if (isSystemAdmin(userId)) {
            return;
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw forbidden();
        }
    }

    public void requireTeamAdmin(User user, UUID teamId) {
        requireTeamAdmin(requireUserId(user), teamId);
    }

    public void requireTeamAdmin(UUID userId, UUID teamId) {
        if (teamId == null || userId == null) {
            throw forbidden();
        }
        if (isSystemAdmin(userId)) {
            return;
        }
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(this::forbidden);
        if (membership.getGroupRole() != GroupRole.ADMIN) {
            throw forbidden();
        }
    }

    public void requireSelfOrTeamAdmin(User user, UUID targetUserId, UUID teamId) {
        UUID currentUserId = requireUserId(user);
        if (currentUserId.equals(targetUserId)) {
            requireTeamMember(currentUserId, teamId);
            return;
        }
        requireTeamAdmin(currentUserId, teamId);
    }

    public void validateTeamAccess(UUID userId, UUID teamId) {
        requireTeamMember(userId, teamId);
    }

    public void validateWorkspaceAccess(UUID userId, UUID workspaceId) {
        requireTeamMember(userId, workspaceId);
    }

    public void validateOrderAccess(UUID userId, UUID orderId) {
        requireProductionOrderAccess(userId, orderId);
    }

    public void requireProductionOrderAccess(User user, UUID orderId) {
        requireProductionOrderAccess(requireUserId(user), orderId);
    }

    public void requireProductionOrderAccess(UUID userId, UUID orderId) {
        ProductionOrder order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        requireTeamMember(userId, order.getTeam().getId());
    }

    public void requireProductionPlanAccess(User user, UUID planId) {
        var plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        requireTeamMember(user, plan.getOrder().getTeam().getId());
    }

    public void requireDailyTargetAccess(User user, UUID targetId) {
        var target = dailyTargetRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Daily target not found"));
        requireTeamMember(user, target.getOrder().getTeam().getId());
    }

    public void requireInventoryItemAccess(User user, UUID itemId) {
        InventoryItem item = inventoryRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        requireTeamMember(user, item.getTeam().getId());
    }

    public void requireGoalAccess(User user, UUID goalId) {
        var goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        requireTeamMember(user, goal.getTeam().getId());
    }

    public void requireTaskAccess(User user, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (task.getGoal() == null || task.getGoal().getTeam() == null) {
            throw forbidden();
        }
        requireTeamMember(user, task.getGoal().getTeam().getId());
    }

    public void requireTaskModifierAccess(User user, UUID taskId) {
        UUID userId = requireUserId(user);
        if (isSystemAdmin(userId)) {
            return;
        }
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (task.getGoal() == null || task.getGoal().getTeam() == null) {
            throw forbidden();
        }

        UUID teamId = task.getGoal().getTeam().getId();
        
        // 1. Is Team Admin?
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId).orElse(null);
        if (membership == null) {
            throw forbidden(); // Not even in the team
        }
        if (membership.getGroupRole() == GroupRole.ADMIN) {
            return; // Admins can modify any task
        }

        // 2. Is Assignee (member) or Supervisor?
        boolean isAssignee = task.getMember() != null && task.getMember().getId().equals(userId);
        boolean isSupervisor = task.getSupervisor() != null && task.getSupervisor().getId().equals(userId);
        
        if (!isAssignee && !isSupervisor) {
            throw forbidden(); // Normal members who are not assignee or supervisor cannot modify this task
        }
    }

    public void requireChecklistAccess(User user, UUID checklistId) {
        Task task = taskChecklistRepository.findById(checklistId)
                .map(checklist -> checklist.getTask())
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        if (task.getGoal() == null || task.getGoal().getTeam() == null) {
            throw forbidden();
        }
        requireTeamMember(user, task.getGoal().getTeam().getId());
    }

    public void requireInterGroupOrderAccess(User user, UUID orderId) {
        InterGroupOrder order = interGroupOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        requireInterGroupOrderAccess(user, order);
    }

    public void requireAttendanceAccess(User user, UUID attendanceId) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        requireSelfOrTeamAdmin(user, attendance.getUser().getId(), attendance.getTeam().getId());
    }

    public void requireInterGroupOrderAccess(User user, InterGroupOrder order) {
        UUID userId = requireUserId(user);
        if (isSystemAdmin(userId)) {
            return;
        }
        boolean buyerUser = order.getBuyerUser() != null && order.getBuyerUser().getId().equals(userId);
        boolean buyerTeamMember = order.getBuyerTeam() != null
                && teamMemberRepository.existsByTeamIdAndUserId(order.getBuyerTeam().getId(), userId);
        boolean sellerTeamMember = order.getSellerTeam() != null
                && teamMemberRepository.existsByTeamIdAndUserId(order.getSellerTeam().getId(), userId);
        if (!buyerUser && !buyerTeamMember && !sellerTeamMember) {
            throw forbidden();
        }
    }

    private UUID requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw forbidden();
        }
        return user.getId();
    }

    private boolean isSystemAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .filter(Role.ADMIN::equals)
                .isPresent();
    }

    private RuntimeException forbidden() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Forbidden");
    }
}
