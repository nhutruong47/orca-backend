package org.example.backend.service;

import org.example.backend.dto.TaskDTO;
import org.example.backend.dto.SalaryDTO;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepo;
    private final GoalRepository goalRepo;
    private final UserRepository userRepo;
    private final TaskChecklistRepository checklistRepo;
    private final NotificationService notificationService;
    private final TeamMemberRepository teamMemberRepo;
    private final ProductionOrderRepository orderRepo;
    private final ProductionBatchRepository batchRepo;
    private final TaskAssignmentRepository assignmentRepo;
    private final TaskTransferRepository transferRepo;
    private final TaskDependencyRepository dependencyRepo;
    private final TaskAuditLogRepository auditRepo;

    public TaskService(TaskRepository taskRepo, GoalRepository goalRepo,
            UserRepository userRepo, TaskChecklistRepository checklistRepo,
            NotificationService notificationService, TeamMemberRepository teamMemberRepo,
            ProductionOrderRepository orderRepo, ProductionBatchRepository batchRepo,
            TaskAssignmentRepository assignmentRepo, TaskTransferRepository transferRepo,
            TaskDependencyRepository dependencyRepo, TaskAuditLogRepository auditRepo) {
        this.taskRepo = taskRepo;
        this.goalRepo = goalRepo;
        this.userRepo = userRepo;
        this.checklistRepo = checklistRepo;
        this.notificationService = notificationService;
        this.teamMemberRepo = teamMemberRepo;
        this.orderRepo = orderRepo;
        this.batchRepo = batchRepo;
        this.assignmentRepo = assignmentRepo;
        this.transferRepo = transferRepo;
        this.dependencyRepo = dependencyRepo;
        this.auditRepo = auditRepo;
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
        t.setTaskCode(dto.getTaskCode());
        t.setTitle(dto.getTitle());
        t.setDescription(dto.getDescription());
        t.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        t.setDeadline(dto.getDeadline());
        t.setDueTime(dto.getDueTime() != null ? dto.getDueTime() : dto.getDeadline());
        t.setStartTime(dto.getStartTime());
        t.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        t.setActualStart(dto.getActualStart());
        t.setActualEnd(dto.getActualEnd());
        t.setProductionStage(dto.getProductionStage());
        t.setOutputTarget(dto.getOutputTarget());
        t.setActualOutput(dto.getActualOutput());
        t.setDefectQuantity(dto.getDefectQuantity());
        t.setCreatedByType(dto.getCreatedByType() != null ? dto.getCreatedByType() : "MANAGER");
        t.setStatus("READY");
        t.setWorkload(dto.getWorkload());

        if (dto.getGoalId() != null) {
            goalRepo.findById(UUID.fromString(dto.getGoalId())).ifPresent(t::setGoal);
            if ((t.getOutputTarget() == null || t.getOutputTarget() <= 0) && t.getGoal() != null) {
                double totalTarget = extractQuantity(t.getGoal().getOutputTarget());
                long existingTasks = taskRepo.findByGoalId(t.getGoal().getId()).size();
                t.setOutputTarget(totalTarget > 0 ? totalTarget / Math.max(1L, existingTasks + 1L) : 0.0);
            }
        }
        if (dto.getOrderId() != null) {
            orderRepo.findById(UUID.fromString(dto.getOrderId())).ifPresent(t::setOrder);
        }
        if (dto.getBatchId() != null) {
            batchRepo.findById(UUID.fromString(dto.getBatchId())).ifPresent(t::setBatch);
        }
        if (dto.getMemberId() != null) {
            userRepo.findById(UUID.fromString(dto.getMemberId())).ifPresent(t::setMember);
        }
        if (dto.getCreatedById() != null) {
            userRepo.findById(UUID.fromString(dto.getCreatedById())).ifPresent(t::setCreatedBy);
        }
        t.setCompletionPercentage(calculateCompletionPercentage(t));
        Task saved = taskRepo.save(t);
        if (saved.getMember() != null) {
            upsertAssignment(saved, saved.getMember(), "PRIMARY", saved.getCreatedBy(), saved.getCreatedByType());
        }
        audit(saved, "CREATE", "Task created", saved.getCreatedBy(), saved.getCreatedByType());
        return toDTO(saved);
    }

    public TaskDTO create(TaskDTO dto, User actor) {
        if (dto.getGoalId() == null) {
            throw new RuntimeException("Task must belong to a production plan");
        }
        Goal goal = goalRepo.findById(UUID.fromString(dto.getGoalId()))
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        requireTeamManager(goal.getTeam(), actor, "create tasks");
        dto.setCreatedById(actor != null ? actor.getId().toString() : dto.getCreatedById());
        dto.setCreatedByType("MANAGER");
        return create(dto);
    }

    public TaskDTO update(UUID id, TaskDTO dto) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        if (dto.getTitle() != null) {
            if (dto.getTitle().trim().isEmpty()) {
                throw new RuntimeException("Task title is required");
            }
            t.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            t.setDescription(dto.getDescription().trim());
        }
        if (dto.getPriority() != null) {
            t.setPriority(dto.getPriority());
        }
        if (dto.getDeadline() != null) {
            t.setDeadline(dto.getDeadline());
        }
        if (dto.getWorkload() != null) {
            t.setWorkload(dto.getWorkload());
        }
        if (dto.getHourlyRate() != null) {
            t.setHourlyRate(dto.getHourlyRate());
        }
        if (dto.getProductionStage() != null) {
            t.setProductionStage(dto.getProductionStage());
        }
        if (dto.getStartTime() != null) {
            t.setStartTime(dto.getStartTime());
        }
        if (dto.getDueTime() != null) {
            t.setDueTime(dto.getDueTime());
            t.setDeadline(dto.getDueTime());
        }
        if (dto.getEstimatedDurationMinutes() != null) {
            t.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        }
        if (dto.getActualStart() != null) {
            t.setActualStart(dto.getActualStart());
        }
        if (dto.getActualEnd() != null) {
            t.setActualEnd(dto.getActualEnd());
        }
        if (dto.getOutputTarget() != null) {
            t.setOutputTarget(dto.getOutputTarget());
        }
        if (dto.getActualOutput() != null) {
            t.setActualOutput(dto.getActualOutput());
        }
        if (dto.getOutputTarget() != null || dto.getActualOutput() != null) {
            t.setCompletionPercentage(calculateCompletionPercentage(t));
        }
        if (dto.getDefectQuantity() != null) {
            t.setDefectQuantity(dto.getDefectQuantity());
        }
        if (dto.getOrderId() != null) {
            orderRepo.findById(UUID.fromString(dto.getOrderId())).ifPresent(t::setOrder);
        }
        if (dto.getBatchId() != null) {
            batchRepo.findById(UUID.fromString(dto.getBatchId())).ifPresent(t::setBatch);
        }
        if (dto.getUpdatedById() != null) {
            userRepo.findById(UUID.fromString(dto.getUpdatedById())).ifPresent(t::setUpdatedBy);
        }
        if (dto.getUpdatedByType() != null) {
            t.setUpdatedByType(dto.getUpdatedByType());
        }
        Task saved = taskRepo.save(t);
        audit(saved, "UPDATE", "Task updated", saved.getUpdatedBy(), saved.getUpdatedByType());
        return toDTO(saved);
    }

    public TaskDTO update(UUID id, TaskDTO dto, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        if (isTeamManager(t, actor)) {
            return update(id, dto);
        }
        if (!isAssignedWorker(t, actor)) {
            throw new RuntimeException("Only assigned staff or managers can update this task");
        }
        if (hasManagerOnlyFields(dto)) {
            throw new RuntimeException("Only managers can edit task plan, deadline, priority, or assignment");
        }
        if (dto.getActualOutput() != null) {
            t.setActualOutput(dto.getActualOutput());
        }
        if (dto.getActualOutput() != null || dto.getOutputTarget() != null) {
            t.setCompletionPercentage(calculateCompletionPercentage(t));
        }
        if (dto.getDefectQuantity() != null) {
            t.setDefectQuantity(dto.getDefectQuantity());
        }
        if (dto.getActualWorkload() != null) {
            t.setActualWorkload(dto.getActualWorkload());
        }
        t.setUpdatedBy(actor);
        t.setUpdatedByType("STAFF");
        Task saved = taskRepo.save(t);
        audit(saved, "UPDATE_OUTPUT", "Production output updated", actor, "STAFF");
        return toDTO(saved);
    }

    public TaskDTO updateStatus(UUID id, String status) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        String nextStatus = normalizeStatus(status);
        validateStatusTransition(t, nextStatus);
        t.setStatus(nextStatus);
        if ("IN_PROGRESS".equals(nextStatus) && t.getActualStart() == null) {
            t.setActualStart(java.time.LocalDateTime.now());
        }
        if ("COMPLETED".equals(nextStatus)) {
            t.setCompletionPercentage(Math.max(calculateCompletionPercentage(t), 100));
            if (t.getActualEnd() == null) {
                t.setActualEnd(java.time.LocalDateTime.now());
            }
        }
        Task saved = taskRepo.save(t);

        // Update goal progress
        if (saved.getGoal() != null) {
            updateGoalProgress(saved.getGoal().getId());
        }
        refreshDownstreamTasks(saved.getId());

        return toDTO(saved);
    }

    public TaskDTO updateStatus(UUID id, String status, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "update task status");
        return updateStatus(id, status);
    }

    public TaskDTO updateProgress(UUID id, int percentage) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        int pct = clampProgress(percentage);
        if (pct > 0 && hasIncompleteDependencies(id)) {
            t.setStatus("BLOCKED");
            taskRepo.save(t);
            throw new RuntimeException("Task dependencies are not completed");
        }
        t.setCompletionPercentage(pct);
        if (pct == 100) {
            t.setStatus("WAITING_APPROVAL");
        } else if (pct > 0) {
            t.setStatus("IN_PROGRESS");
            if (t.getActualStart() == null) {
                t.setActualStart(java.time.LocalDateTime.now());
            }
        } else {
            refreshWorkflowState(t);
        }
        Task saved = taskRepo.save(t);
        if (saved.getGoal() != null) {
            updateGoalProgress(saved.getGoal().getId());
        }
        return toDTO(saved);
    }

    public TaskDTO updateProgress(UUID id, int percentage, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "update task progress");
        return updateProgress(id, percentage);
    }

    private double extractQuantity(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|g|tấn|tan|ton|t)?", Pattern.CASE_INSENSITIVE).matcher(raw.toLowerCase());
        if (!matcher.find()) {
            return 0.0;
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null) {
            return value;
        }
        return switch (unit) {
            case "tấn", "tan", "ton", "t" -> value * 1000.0;
            case "kg" -> value;
            case "g" -> value / 1000.0;
            default -> value;
        };
    }

    private int calculateCompletionPercentage(Task task) {
        Double target = task.getOutputTarget();
        Double actual = task.getActualOutput();
        if (target != null && target > 0 && actual != null) {
            double ratio = Math.min(1.0, actual / target);
            return (int) Math.round(ratio * 100.0);
        }
        return task.getCompletionPercentage() != null ? task.getCompletionPercentage() : 0;
    }

    public TaskDTO updateWorkload(UUID id, Double actualWorkload) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setActualWorkload(actualWorkload);
        return toDTO(taskRepo.save(t));
    }

    public TaskDTO updateWorkload(UUID id, Double actualWorkload, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        if (!isTeamManager(t, actor) && !isAssignedWorker(t, actor)) {
            throw new RuntimeException("Only assigned staff or managers can update production output");
        }
        t.setActualWorkload(actualWorkload);
        t.setUpdatedBy(actor);
        t.setUpdatedByType(isTeamManager(t, actor) ? "MANAGER" : "STAFF");
        Task saved = taskRepo.save(t);
        audit(saved, "UPDATE_OUTPUT", "Production output updated", actor, saved.getUpdatedByType());
        return toDTO(saved);
    }

    public TaskDTO assign(UUID id, UUID memberId) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User member = userRepo.findById(memberId).orElseThrow(() -> new RuntimeException("User not found"));
        t.setMember(member);
        t.setAcceptanceStatus("WAITING");
        Task saved = taskRepo.save(t);
        upsertAssignment(saved, member, "PRIMARY", null, "MANAGER");
        audit(saved, "ASSIGN_PRIMARY", "Primary worker assigned to " + member.getUsername(), null, "MANAGER");

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

    public TaskDTO assign(UUID id, UUID memberId, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "assign workers");
        return assign(id, memberId);
    }

    public TaskDTO transferPrimary(UUID taskId, UUID toMemberId, String reason, UUID actorId, String actorType) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User from = t.getMember();
        User to = userRepo.findById(toMemberId).orElseThrow(() -> new RuntimeException("User not found"));
        User actor = actorId != null ? userRepo.findById(actorId).orElse(null) : null;

        TaskTransfer transfer = new TaskTransfer();
        transfer.setTask(t);
        transfer.setFromEmployee(from);
        transfer.setToEmployee(to);
        transfer.setReason(reason);
        transfer.setProgressPercent(t.getCompletionPercentage() != null ? t.getCompletionPercentage() : 0);
        transfer.setTransferredBy(actor);
        transfer.setTransferredByType(actorType != null ? actorType : "MANAGER");
        transferRepo.save(transfer);

        t.setMember(to);
        t.setAcceptanceStatus("WAITING");
        t.setUpdatedBy(actor);
        t.setUpdatedByType(actorType != null ? actorType : "MANAGER");
        Task saved = taskRepo.save(t);
        upsertAssignment(saved, to, "PRIMARY", actor, actorType != null ? actorType : "MANAGER");
        audit(saved, "TRANSFER", "Transferred from "
                + (from != null ? from.getUsername() : "unassigned") + " to " + to.getUsername()
                + (reason != null && !reason.isBlank() ? ". Reason: " + reason : ""), actor, actorType);
        return toDTO(saved);
    }

    public TaskDTO transferPrimary(UUID taskId, UUID toMemberId, String reason, User actor, String actorType) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "transfer workers");
        return transferPrimary(taskId, toMemberId, reason, actor != null ? actor.getId() : null, actorType);
    }

    public TaskDTO addDependency(UUID taskId, UUID dependsOnTaskId, String dependencyType) {
        if (taskId.equals(dependsOnTaskId)) {
            throw new RuntimeException("Task cannot depend on itself");
        }
        Task task = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        Task dependsOn = taskRepo.findById(dependsOnTaskId).orElseThrow(() -> new RuntimeException("Dependency task not found"));
        int currentProgress = task.getCompletionPercentage() != null ? task.getCompletionPercentage() : 0;
        if (currentProgress > 0 && !"COMPLETED".equals(dependsOn.getStatus())) {
            throw new RuntimeException("Cannot add an incomplete dependency to a task that already has progress");
        }
        if (createsDependencyCycle(taskId, dependsOnTaskId, new HashSet<>())) {
            throw new RuntimeException("Task dependency cycle is not allowed");
        }
        TaskDependency dependency = dependencyRepo.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId).orElseGet(TaskDependency::new);
        dependency.setTask(task);
        dependency.setDependsOnTask(dependsOn);
        dependency.setDependencyType(dependencyType != null ? dependencyType : "FINISH_TO_START");
        dependencyRepo.save(dependency);
        refreshWorkflowState(task);
        taskRepo.save(task);
        audit(task, "ADD_DEPENDENCY", "Depends on task " + dependsOn.getTaskCode(), null, "MANAGER");
        return toDTO(task);
    }

    public TaskDTO addDependency(UUID taskId, UUID dependsOnTaskId, String dependencyType, User actor) {
        Task task = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(task, actor, "edit task dependencies");
        return addDependency(taskId, dependsOnTaskId, dependencyType);
    }

    public List<Map<String, Object>> getDependencies(UUID taskId) {
        return dependencyRepo.findByTaskId(taskId).stream().map(dep -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", dep.getId().toString());
            m.put("taskId", dep.getTask().getId().toString());
            m.put("dependsOnTaskId", dep.getDependsOnTask().getId().toString());
            m.put("dependsOnTaskCode", dep.getDependsOnTask().getTaskCode());
            m.put("dependsOnTaskTitle", dep.getDependsOnTask().getTitle());
            m.put("dependencyType", dep.getDependencyType());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTransfers(UUID taskId) {
        return transferRepo.findByTaskIdOrderByTransferTimeDesc(taskId).stream().map(tr -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", tr.getId().toString());
            m.put("taskId", tr.getTask().getId().toString());
            m.put("fromEmployeeId", tr.getFromEmployee() != null ? tr.getFromEmployee().getId().toString() : null);
            m.put("fromEmployeeName", tr.getFromEmployee() != null ? tr.getFromEmployee().getUsername() : null);
            m.put("toEmployeeId", tr.getToEmployee().getId().toString());
            m.put("toEmployeeName", tr.getToEmployee().getUsername());
            m.put("reason", tr.getReason());
            m.put("progressPercent", tr.getProgressPercent());
            m.put("transferTime", tr.getTransferTime());
            m.put("transferredByType", tr.getTransferredByType());
            return m;
        }).collect(Collectors.toList());
    }

    /** Employee accepts or rejects assigned task */
    public TaskDTO respondToTask(UUID taskId, UUID userId, boolean accepted) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        User actor = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!isTeamManager(t, actor) && (t.getMember() == null || !t.getMember().getId().equals(userId))) {
            throw new RuntimeException("Bạn không được giao nhiệm vụ này");
        }
        t.setAcceptanceStatus(accepted ? "ACCEPTED" : "REJECTED");
        if (accepted) {
            if (hasIncompleteDependencies(taskId)) {
                t.setStatus("BLOCKED");
                taskRepo.save(t);
                throw new RuntimeException("Task dependencies are not completed");
            }
            t.setStatus("READY");
            Task saved = taskRepo.save(t);
            // Notify group owner
            notifyOwnerOfResponse(t, true);
            return toDTO(saved);
        } else {
            // If rejected and there is a backup member, promote backup to assigned and notify
            User backup = t.getBackupMember();
            if (backup != null) {
                t.setMember(backup);
                t.setBackupMember(null);
                t.setAcceptanceStatus("WAITING");
                Task saved = taskRepo.save(t);

                // Notify the new assignee
                notificationService.createAndSend(
                    backup,
                    "Bạn được chọn làm sao lưu",
                    "Bạn được chỉ định thay thế nhiệm vụ: " + t.getTitle(),
                    "TASK_ASSIGNED",
                    t.getId()
                );

                // Notify owner about replacement
                notifyOwnerOfResponse(t, false);

                return toDTO(saved);
            } else {
                t.setAcceptanceStatus("REJECTED");
                Task saved = taskRepo.save(t);
                notifyOwnerOfResponse(t, false);
                return toDTO(saved);
            }
        }
    }

    private void notifyOwnerOfResponse(Task t, boolean accepted) {
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
        assignmentRepo.deleteAll(assignmentRepo.findByTaskId(id));
        transferRepo.deleteAll(transferRepo.findByTaskIdOrderByTransferTimeDesc(id));
        dependencyRepo.deleteAll(dependencyRepo.findByTaskId(id));
        dependencyRepo.deleteAll(dependencyRepo.findByDependsOnTaskId(id));
        auditRepo.deleteAll(auditRepo.findByTaskIdOrderByCreatedAtDesc(id));
        taskRepo.deleteById(id);
        updateGoalProgress(goalId);
    }

    public void delete(UUID id, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "delete tasks");
        delete(id);
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
        if (pct > 0 && hasIncompleteDependencies(taskId)) {
            t.setStatus("BLOCKED");
            taskRepo.save(t);
            throw new RuntimeException("Task dependencies are not completed");
        }
        t.setCompletionPercentage(pct);
        if (pct == 100) {
            t.setStatus("WAITING_APPROVAL");
        } else if (pct > 0) {
            t.setStatus("IN_PROGRESS");
        } else {
            refreshWorkflowState(t);
        }
        taskRepo.save(t);

        if (t.getGoal() != null) {
            updateGoalProgress(t.getGoal().getId());
        }
    }

    public void toggleChecklistItem(UUID checklistId, User actor) {
        TaskChecklist c = checklistRepo.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));
        requireTeamManager(c.getTask(), actor, "update task progress");
        toggleChecklistItem(checklistId);
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
        if (goalId == null) {
            return;
        }
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
        dto.setTaskCode(t.getTaskCode());
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setPriority(t.getPriority());
        dto.setStatus(t.getStatus());
        dto.setAcceptanceStatus(t.getAcceptanceStatus());
        dto.setHourlyRate(t.getHourlyRate());
        dto.setWorkload(t.getWorkload());
        dto.setActualWorkload(t.getActualWorkload());
        dto.setCompletionPercentage(t.getCompletionPercentage());
        dto.setProductionStage(t.getProductionStage());
        dto.setStartTime(t.getStartTime());
        dto.setDueTime(t.getDueTime());
        dto.setEstimatedDurationMinutes(t.getEstimatedDurationMinutes());
        dto.setActualStart(t.getActualStart());
        dto.setActualEnd(t.getActualEnd());
        dto.setOutputTarget(t.getOutputTarget());
        dto.setActualOutput(t.getActualOutput());
        dto.setDefectQuantity(t.getDefectQuantity());
        dto.setDeadline(t.getDeadline());
        dto.setOrderId(t.getOrder() != null ? t.getOrder().getId().toString() : null);
        dto.setOrderCode(t.getOrder() != null ? t.getOrder().getOrderCode() : null);
        dto.setBatchId(t.getBatch() != null ? t.getBatch().getId().toString() : null);
        dto.setBatchCode(t.getBatch() != null ? t.getBatch().getBatchCode() : null);
        dto.setGoalId(t.getGoal() != null ? t.getGoal().getId().toString() : null);
        dto.setGoalTitle(t.getGoal() != null ? t.getGoal().getTitle() : null);
        dto.setMemberId(t.getMember() != null ? t.getMember().getId().toString() : null);
        dto.setMemberName(t.getMember() != null ? t.getMember().getUsername() : null);
        dto.setBackupMemberId(t.getBackupMember() != null ? t.getBackupMember().getId().toString() : null);
        dto.setBackupMemberName(t.getBackupMember() != null ? t.getBackupMember().getUsername() : null);
        assignmentRepo.findByTaskIdAndRoleAndActiveTrue(t.getId(), "SUPERVISOR").stream().findFirst().ifPresent(a -> {
            dto.setSupervisorId(a.getWorker() != null ? a.getWorker().getId().toString() : null);
            dto.setSupervisorName(a.getWorker() != null ? a.getWorker().getUsername() : null);
        });
        List<TaskDependency> dependencies = dependencyRepo.findByTaskId(t.getId());
        dto.setDependencyTaskCodes(dependencies.stream()
                .map(TaskDependency::getDependsOnTask)
                .filter(Objects::nonNull)
                .map(dep -> dep.getTaskCode() != null ? dep.getTaskCode() : dep.getId().toString())
                .collect(Collectors.toList()));
        dto.setDependencyTaskTitles(dependencies.stream()
                .map(TaskDependency::getDependsOnTask)
                .filter(Objects::nonNull)
                .map(Task::getTitle)
                .collect(Collectors.toList()));
        dto.setCreatedById(t.getCreatedBy() != null ? t.getCreatedBy().getId().toString() : null);
        dto.setCreatedByName(t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : null);
        dto.setCreatedByType(t.getCreatedByType());
        dto.setUpdatedById(t.getUpdatedBy() != null ? t.getUpdatedBy().getId().toString() : null);
        dto.setUpdatedByName(t.getUpdatedBy() != null ? t.getUpdatedBy().getUsername() : null);
        dto.setUpdatedByType(t.getUpdatedByType());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }

    public TaskDTO setBackup(UUID id, UUID memberId) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User member = userRepo.findById(memberId).orElseThrow(() -> new RuntimeException("User not found"));
        t.setBackupMember(member);
        Task saved = taskRepo.save(t);
        upsertAssignment(saved, member, "BACKUP", null, "MANAGER");
        audit(saved, "ASSIGN_BACKUP", "Backup worker assigned to " + member.getUsername(), null, "MANAGER");

        // Notify backup member
        notificationService.createAndSend(
            member,
            "Bạn được chọn làm sao lưu",
            "Bạn được chỉ định làm sao lưu cho nhiệm vụ: " + t.getTitle(),
            "TASK_ASSIGNED",
            t.getId()
        );

        return toDTO(saved);
    }

    public TaskDTO setBackup(UUID id, UUID memberId, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "assign backup workers");
        return setBackup(id, memberId);
    }

    public TaskDTO setSupervisor(UUID id, UUID memberId) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User member = userRepo.findById(memberId).orElseThrow(() -> new RuntimeException("User not found"));
        upsertAssignment(t, member, "SUPERVISOR", null, "MANAGER");
        audit(t, "ASSIGN_SUPERVISOR", "Supervisor assigned to " + member.getUsername(), null, "MANAGER");
        return toDTO(t);
    }

    public TaskDTO setSupervisor(UUID id, UUID memberId, User actor) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        requireTeamManager(t, actor, "assign supervisors");
        return setSupervisor(id, memberId);
    }

    private void upsertAssignment(Task task, User worker, String role, User actor, String actorType) {
        assignmentRepo.findByTaskIdAndRoleAndActiveTrue(task.getId(), role)
                .forEach(existing -> {
                    existing.setActive(false);
                    assignmentRepo.save(existing);
                });
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setWorker(worker);
        assignment.setRole(role);
        assignment.setAssignedBy(actor);
        assignment.setAssignedByType(actorType != null ? actorType : "MANAGER");
        assignment.setActive(true);
        assignmentRepo.save(assignment);
    }

    private void audit(Task task, String action, String detail, User actor, String actorType) {
        TaskAuditLog log = new TaskAuditLog();
        log.setTask(task);
        log.setAction(action);
        log.setDetail(detail);
        log.setActor(actor);
        log.setActorType(actorType != null ? actorType : "MANAGER");
        auditRepo.save(log);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Task status is required");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of("PENDING", "BLOCKED", "READY", "IN_PROGRESS", "WAITING_APPROVAL", "COMPLETED", "CANCELLED");
        if (!allowed.contains(normalized)) {
            throw new RuntimeException("Invalid task status");
        }
        return normalized;
    }

    private int clampProgress(Integer percentage) {
        if (percentage == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, percentage));
    }

    private boolean hasIncompleteDependencies(UUID taskId) {
        return dependencyRepo.findByTaskId(taskId).stream()
                .anyMatch(dep -> dep.getDependsOnTask() != null
                        && !"COMPLETED".equals(dep.getDependsOnTask().getStatus()));
    }

    private void validateStatusTransition(Task task, String nextStatus) {
        if ("IN_PROGRESS".equals(nextStatus) || "WAITING_APPROVAL".equals(nextStatus) || "COMPLETED".equals(nextStatus)) {
            if (hasIncompleteDependencies(task.getId())) {
                task.setStatus("BLOCKED");
                taskRepo.save(task);
                throw new RuntimeException("Task dependencies are not completed");
            }
        }
        if ("COMPLETED".equals(nextStatus) && (task.getCompletionPercentage() == null || task.getCompletionPercentage() < 100)) {
            throw new RuntimeException("Task must reach 100% before completion");
        }
    }

    private void refreshWorkflowState(Task task) {
        if ("COMPLETED".equals(task.getStatus()) || "CANCELLED".equals(task.getStatus()) || "WAITING_APPROVAL".equals(task.getStatus())) {
            return;
        }
        int pct = task.getCompletionPercentage() != null ? task.getCompletionPercentage() : 0;
        if (hasIncompleteDependencies(task.getId())) {
            task.setStatus("BLOCKED");
        } else if (pct > 0) {
            task.setStatus("IN_PROGRESS");
        } else {
            task.setStatus("READY");
        }
    }

    private void refreshDownstreamTasks(UUID completedTaskId) {
        dependencyRepo.findByDependsOnTaskId(completedTaskId).forEach(dep -> {
            Task downstream = dep.getTask();
            if (downstream != null) {
                refreshWorkflowState(downstream);
                taskRepo.save(downstream);
            }
        });
    }

    private boolean createsDependencyCycle(UUID taskId, UUID dependsOnTaskId, Set<UUID> visited) {
        if (taskId.equals(dependsOnTaskId)) {
            return true;
        }
        if (!visited.add(dependsOnTaskId)) {
            return false;
        }
        return dependencyRepo.findByTaskId(dependsOnTaskId).stream()
                .map(TaskDependency::getDependsOnTask)
                .filter(Objects::nonNull)
                .anyMatch(dep -> createsDependencyCycle(taskId, dep.getId(), visited));
    }

    private boolean isAssignedWorker(Task task, User actor) {
        return task != null && actor != null && task.getMember() != null
                && task.getMember().getId().equals(actor.getId());
    }

    private boolean isTeamManager(Task task, User actor) {
        return task != null && isTeamManager(task.getGoal() != null ? task.getGoal().getTeam() : null, actor);
    }

    private boolean isTeamManager(Team team, User actor) {
        if (team == null || actor == null) {
            return false;
        }
        if (actor.getRole() == Role.ADMIN || (team.getOwner() != null && team.getOwner().getId().equals(actor.getId()))) {
            return true;
        }
        return teamMemberRepo.findByTeamIdAndUserId(team.getId(), actor.getId())
                .map(tm -> tm.getGroupRole() == GroupRole.ADMIN)
                .orElse(false);
    }

    private void requireTeamManager(Task task, User actor, String action) {
        if (!isTeamManager(task, actor)) {
            throw new RuntimeException("Only managers can " + action);
        }
    }

    private void requireTeamManager(Team team, User actor, String action) {
        if (!isTeamManager(team, actor)) {
            throw new RuntimeException("Only managers can " + action);
        }
    }

    private boolean hasManagerOnlyFields(TaskDTO dto) {
        return dto.getTaskCode() != null
                || dto.getTitle() != null
                || dto.getDescription() != null
                || dto.getPriority() != null
                || dto.getStatus() != null
                || dto.getProductionStage() != null
                || dto.getStartTime() != null
                || dto.getDueTime() != null
                || dto.getEstimatedDurationMinutes() != null
                || dto.getActualStart() != null
                || dto.getActualEnd() != null
                || dto.getOutputTarget() != null
                || dto.getDeadline() != null
                || dto.getOrderId() != null
                || dto.getBatchId() != null
                || dto.getMemberId() != null
                || dto.getBackupMemberId() != null
                || dto.getSupervisorId() != null;
    }
}
