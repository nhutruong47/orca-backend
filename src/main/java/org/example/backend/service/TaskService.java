package org.example.backend.service;

import org.example.backend.dto.TaskDTO;
import org.example.backend.dto.SalaryDTO;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepo;
    private final GoalRepository goalRepo;
    private final UserRepository userRepo;
    private final TaskChecklistRepository checklistRepo;
    private final NotificationService notificationService;
    private final TeamMemberRepository teamMemberRepo;

    public TaskService(TaskRepository taskRepo, GoalRepository goalRepo,
            UserRepository userRepo, TaskChecklistRepository checklistRepo,
            NotificationService notificationService, TeamMemberRepository teamMemberRepo) {
        this.taskRepo = taskRepo;
        this.goalRepo = goalRepo;
        this.userRepo = userRepo;
        this.checklistRepo = checklistRepo;
        this.notificationService = notificationService;
        this.teamMemberRepo = teamMemberRepo;
    }

    public List<TaskDTO> getByGoal(UUID goalId) {
        return taskRepo.findByGoalId(goalId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TaskDTO> getByMember(UUID memberId) {
        return taskRepo.findByMemberId(memberId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TaskDTO> getAll() {
        return taskRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TaskDTO create(TaskDTO dto) {
        Task t = new Task();
        t.setTitle(dto.getTitle());
        t.setDescription(dto.getDescription());
        t.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        t.setDeadline(dto.getDeadline());
        t.setStatus("PENDING");
        t.setWorkload(dto.getWorkload());

        if (dto.getGoalId() != null) {
            goalRepo.findById(UUID.fromString(dto.getGoalId())).ifPresent(t::setGoal);
        }
        if (dto.getMemberId() != null) {
            userRepo.findById(UUID.fromString(dto.getMemberId())).ifPresent(t::setMember);
        }
        return toDTO(taskRepo.save(t));
    }

    public TaskDTO updateStatus(UUID id, String status) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setStatus(status);
        if ("COMPLETED".equals(status)) {
            t.setCompletionPercentage(100);
        }
        Task saved = taskRepo.save(t);

        // Update goal progress
        updateGoalProgress(saved.getGoal().getId());

        return toDTO(saved);
    }

    public TaskDTO updateWorkload(UUID id, Double actualWorkload) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setActualWorkload(actualWorkload);
        return toDTO(taskRepo.save(t));
    }

    public TaskDTO assign(UUID id, UUID memberId) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User member = userRepo.findById(memberId).orElseThrow(() -> new RuntimeException("User not found"));
        t.setMember(member);
        t.setAcceptanceStatus("WAITING");
        Task saved = taskRepo.save(t);

        // Send notification to assigned member
        notificationService.createAndSend(
            member,
            "Nhiệm vụ mới",
            "Bạn được giao nhiệm vụ: " + t.getTitle(),
            "TASK_ASSIGNED",
            t.getId()
        );

        return toDTO(saved);
    }

    /** Employee accepts or rejects assigned task */
    public TaskDTO respondToTask(UUID taskId, UUID userId, boolean accepted) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        if (t.getMember() == null || !t.getMember().getId().equals(userId)) {
            throw new RuntimeException("Bạn không được giao nhiệm vụ này");
        }
        t.setAcceptanceStatus(accepted ? "ACCEPTED" : "REJECTED");
        if (accepted) {
            t.setStatus("IN_PROGRESS");
        }
        Task saved = taskRepo.save(t);

        // Notify group owner
        Goal goal = t.getGoal();
        if (goal != null && goal.getTeam() != null) {
            UUID ownerId = goal.getTeam().getOwner().getId();
            User owner = userRepo.findById(ownerId).orElse(null);
            if (owner != null) {
                String status = accepted ? "CHẤP NHẬN" : "TỪ CHỐI";
                notificationService.createAndSend(
                    owner,
                    "Phản hồi nhiệm vụ",
                    t.getMember().getUsername() + " đã " + status + " nhiệm vụ: " + t.getTitle(),
                    accepted ? "TASK_ACCEPTED" : "TASK_REJECTED",
                    t.getId()
                );
            }
        }

        return toDTO(saved);
    }

    /** Calculate salary report for all members in a team */
    public List<SalaryDTO> getSalaryReport(UUID teamId) {
        List<TeamMember> members = teamMemberRepo.findByTeamId(teamId);
        List<SalaryDTO> report = new ArrayList<>();

        for (TeamMember tm : members) {
            User member = tm.getUser();
            List<Task> tasks = taskRepo.findByMemberId(member.getId());
            // Filter tasks belonging to this team's goals
            List<Task> teamTasks = tasks.stream()
                .filter(t -> t.getGoal() != null && t.getGoal().getTeam() != null
                    && t.getGoal().getTeam().getId().equals(teamId))
                .collect(Collectors.toList());

            int totalTasks = teamTasks.size();
            int completed = (int) teamTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            double totalWorkload = teamTasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .mapToDouble(t -> t.getActualWorkload() != null ? t.getActualWorkload() : (t.getWorkload() != null ? t.getWorkload() : 0))
                .sum();

            // Use hourly rate from tasks or default 50000 VND/hour
            double avgRate = teamTasks.stream()
                .filter(t -> t.getHourlyRate() != null)
                .mapToDouble(Task::getHourlyRate)
                .average().orElse(50000);

            SalaryDTO dto = new SalaryDTO();
            dto.setMemberId(member.getId().toString());
            dto.setMemberName(member.getFullName() != null ? member.getFullName() : member.getUsername());
            dto.setTotalTasks(totalTasks);
            dto.setCompletedTasks(completed);
            dto.setTotalWorkload(totalWorkload);
            dto.setTotalActualWorkload(totalWorkload);
            dto.setHourlyRate(avgRate);
            dto.setEstimatedSalary(totalWorkload * avgRate);
            report.add(dto);
        }
        return report;
    }

    public void delete(UUID id) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        UUID goalId = t.getGoal().getId();
        checklistRepo.deleteAll(checklistRepo.findByTaskIdOrderBySortOrderAsc(id));
        taskRepo.deleteById(id);
        updateGoalProgress(goalId);
    }

    // === Checklist ===
    public List<Map<String, Object>> getChecklist(UUID taskId) {
        return checklistRepo.findByTaskIdOrderBySortOrderAsc(taskId).stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId().toString());
                    m.put("content", c.getContent());
                    m.put("checked", c.getChecked());
                    m.put("sortOrder", c.getSortOrder());
                    return m;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> addChecklistItem(UUID taskId, String content) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        TaskChecklist c = new TaskChecklist();
        c.setTask(t);
        c.setContent(content);
        c.setChecked(false);
        int count = checklistRepo.findByTaskIdOrderBySortOrderAsc(taskId).size();
        c.setSortOrder(count);
        checklistRepo.save(c);
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId().toString());
        m.put("content", c.getContent());
        m.put("checked", c.getChecked());
        return m;
    }

    public void toggleChecklistItem(UUID checklistId) {
        TaskChecklist c = checklistRepo.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        c.setChecked(!c.getChecked());
        checklistRepo.save(c);

        // Update task completion percentage
        UUID taskId = c.getTask().getId();
        List<TaskChecklist> items = checklistRepo.findByTaskIdOrderBySortOrderAsc(taskId);
        long checked = items.stream().filter(TaskChecklist::getChecked).count();
        int pct = items.isEmpty() ? 0 : (int) (checked * 100 / items.size());

        Task t = taskRepo.findById(taskId).orElseThrow();
        t.setCompletionPercentage(pct);
        if (pct == 100)
            t.setStatus("COMPLETED");
        taskRepo.save(t);

        updateGoalProgress(t.getGoal().getId());
    }

    // === KPI ===
    public Map<String, Object> getMemberKpi(UUID memberId) {
        List<Task> tasks = taskRepo.findByMemberId(memberId);
        long total = tasks.size();
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long overdue = tasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(java.time.LocalDateTime.now())
                        && !"COMPLETED".equals(t.getStatus()))
                .count();
        double avgCompletion = tasks.stream()
                .mapToInt(t -> t.getCompletionPercentage() != null ? t.getCompletionPercentage() : 0).average()
                .orElse(0);

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("totalTasks", total);
        kpi.put("completedTasks", completed);
        kpi.put("overdueTasks", overdue);
        kpi.put("completionRate", total > 0 ? (completed * 100 / total) : 0);
        kpi.put("avgCompletionPercentage", Math.round(avgCompletion));
        return kpi;
    }

    private void updateGoalProgress(UUID goalId) {
        List<Task> tasks = taskRepo.findByGoalId(goalId);
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        goalRepo.findById(goalId).ifPresent(g -> {
            g.setTotalTasks(tasks.size());
            g.setCompletedTasks((int) completed);
            if (completed == tasks.size() && !tasks.isEmpty()) {
                g.setStatus("DONE");
            }
            goalRepo.save(g);
        });
    }

    private TaskDTO toDTO(Task t) {
        TaskDTO dto = new TaskDTO();
        dto.setId(t.getId() != null ? t.getId().toString() : null);
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setPriority(t.getPriority());
        dto.setStatus(t.getStatus());
        dto.setAcceptanceStatus(t.getAcceptanceStatus());
        dto.setHourlyRate(t.getHourlyRate());
        dto.setWorkload(t.getWorkload());
        dto.setActualWorkload(t.getActualWorkload());
        dto.setCompletionPercentage(t.getCompletionPercentage());
        dto.setDeadline(t.getDeadline());
        dto.setGoalId(t.getGoal() != null ? t.getGoal().getId().toString() : null);
        dto.setGoalTitle(t.getGoal() != null ? t.getGoal().getTitle() : null);
        dto.setMemberId(t.getMember() != null ? t.getMember().getId().toString() : null);
        dto.setMemberName(t.getMember() != null ? t.getMember().getUsername() : null);
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}
