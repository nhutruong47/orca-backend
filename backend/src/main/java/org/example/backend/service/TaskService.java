package org.example.backend.service;

import org.example.backend.dto.TaskDTO;
import org.example.backend.dto.SalaryDTO;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
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
    private final AttendanceRepository attendanceRepo;

    public TaskService(TaskRepository taskRepo, GoalRepository goalRepo,
            UserRepository userRepo, TaskChecklistRepository checklistRepo,
            NotificationService notificationService, TeamMemberRepository teamMemberRepo,
            AttendanceRepository attendanceRepo) {
        this.taskRepo = taskRepo;
        this.goalRepo = goalRepo;
        this.userRepo = userRepo;
        this.checklistRepo = checklistRepo;
        this.notificationService = notificationService;
        this.teamMemberRepo = teamMemberRepo;
        this.attendanceRepo = attendanceRepo;
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

    public TaskDTO getById(UUID id) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        return toDTO(t);
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

    public TaskDTO updateProgress(UUID id, int percentage) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setCompletionPercentage(percentage);
        if (percentage == 100) {
            t.setStatus("COMPLETED");
        } else if (percentage > 0 && "PENDING".equals(t.getStatus())) {
            t.setStatus("IN_PROGRESS");
        }
        Task saved = taskRepo.save(t);
        updateGoalProgress(saved.getGoal().getId());
        return toDTO(saved);
    }

    public TaskDTO updateWorkload(UUID id, Double actualWorkload) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setActualWorkload(actualWorkload);
        return toDTO(taskRepo.save(t));
    }

    public TaskDTO update(UUID id, Map<String, Object> updates) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        if (updates.containsKey("title")) t.setTitle((String) updates.get("title"));
        if (updates.containsKey("description")) t.setDescription((String) updates.get("description"));
        if (updates.containsKey("priority")) {
            Object p = updates.get("priority");
            t.setPriority(p instanceof Integer ? (Integer) p : (p instanceof Number ? ((Number) p).intValue() : t.getPriority()));
        }
        if (updates.containsKey("actualOutput")) {
            Object val = updates.get("actualOutput");
            t.setActualOutput(val != null ? Double.valueOf(val.toString()) : null);
        }
        if (updates.containsKey("outputTarget")) {
            Object val = updates.get("outputTarget");
            t.setOutputTarget(val != null ? Double.valueOf(val.toString()) : null);
        }
        if (updates.containsKey("productionStage")) {
            t.setProductionStage((String) updates.get("productionStage"));
        }
        if (updates.containsKey("dueTime")) {
            String due = (String) updates.get("dueTime");
            t.setDueTime(due != null ? java.time.LocalDateTime.parse(due) : null);
        }
        if (updates.containsKey("deadline")) {
            String dead = (String) updates.get("deadline");
            t.setDeadline(dead != null ? java.time.LocalDateTime.parse(dead) : null);
        }

        if (updates.containsKey("actualOutput") || updates.containsKey("outputTarget")) {
            Double target = t.getOutputTarget() != null ? t.getOutputTarget() : (t.getWorkload() != null ? t.getWorkload() : 0.0);
            Double actual = t.getActualOutput() != null ? t.getActualOutput() : 0.0;
            
            if (target > 0) {
                if (actual >= target) {
                    t.setStatus("COMPLETED");
                    t.setCompletionPercentage(100);
                } else {
                    t.setCompletionPercentage((int) Math.round((actual / target) * 100));
                    if (actual > 0) {
                        t.setStatus("IN_PROGRESS");
                    } else {
                        t.setStatus("PENDING");
                    }
                }
            } else {
                if (actual > 0) {
                    t.setStatus("IN_PROGRESS");
                    t.setCompletionPercentage(50); // fallback or keep current
                } else {
                    t.setStatus("PENDING");
                    t.setCompletionPercentage(0);
                }
            }
        }

        Task saved = taskRepo.save(t);
        if (saved.getGoal() != null) {
            updateGoalProgress(saved.getGoal().getId());
        }
        return toDTO(saved);
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

            // Get attendance data for this member and team
            List<Attendance> attendances = attendanceRepo.findByUserIdAndTeamId(member.getId(), teamId);
            double totalRegularHours = attendances.stream().mapToDouble(a -> a.getRegularHours() != null ? a.getRegularHours() : 0.0).sum();
            double totalOvertimeHours = attendances.stream().mapToDouble(a -> a.getOvertimeHours() != null ? a.getOvertimeHours() : 0.0).sum();

            // Use hourly rate from tasks or default 50000 VND/hour
            double avgRate = teamTasks.stream()
                .filter(t -> t.getHourlyRate() != null)
                .mapToDouble(Task::getHourlyRate)
                .average().orElse(50000);
            
            // Overtime rate defaults to 1.5x of normal rate
            double defaultOvertimeRate = avgRate * 1.5;

            SalaryDTO dto = new SalaryDTO();
            dto.setMemberId(member.getId().toString());
            dto.setMemberName(member.getFullName() != null ? member.getFullName() : member.getUsername());
            dto.setTotalTasks(totalTasks);
            dto.setCompletedTasks(completed);
            dto.setTotalWorkload(totalWorkload);
            dto.setTotalActualWorkload(totalWorkload);
            
            dto.setRegularHours(totalRegularHours);
            dto.setOvertimeHours(totalOvertimeHours);
            dto.setHourlyRate(avgRate);
            dto.setOvertimeRate(defaultOvertimeRate);
            
            // Salary calculation based on Attendance, not Task Workload anymore
            // Fallback to totalWorkload if no attendance data exists (backward compatibility or testing)
            double billableRegular = totalRegularHours > 0 ? totalRegularHours : totalWorkload;
            dto.setEstimatedSalary((billableRegular * avgRate) + (totalOvertimeHours * defaultOvertimeRate));
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
        dto.setOutputTarget(t.getOutputTarget());
        dto.setActualOutput(t.getActualOutput());
        dto.setDeadline(t.getDeadline());
        dto.setDueTime(t.getDueTime());
        dto.setProductionStage(t.getProductionStage());
        dto.setGoalId(t.getGoal() != null ? t.getGoal().getId().toString() : null);
        dto.setGoalTitle(t.getGoal() != null ? t.getGoal().getTitle() : null);
        dto.setTeamId(t.getGoal() != null && t.getGoal().getTeam() != null ? t.getGoal().getTeam().getId().toString() : null);
        dto.setMemberId(t.getMember() != null ? t.getMember().getId().toString() : null);
        dto.setMemberName(t.getMember() != null ? t.getMember().getUsername() : null);
        dto.setBackupMemberId(t.getBackupMember() != null ? t.getBackupMember().getId().toString() : null);
        dto.setBackupMemberName(t.getBackupMember() != null ? t.getBackupMember().getUsername() : null);
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public TaskDTO setBackup(UUID id, UUID memberId) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        User member = userRepo.findById(memberId).orElseThrow(() -> new RuntimeException("User not found"));
        t.setBackupMember(member);
        Task saved = taskRepo.save(t);

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

    public byte[] exportSalaryExcel(UUID teamId) throws Exception {
        List<SalaryDTO> report = getSalaryReport(teamId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bang luong");
            String[] headers = {"STT", "Nhan vien", "Tong task", "Hoan thanh", "Gio thuong", "Gio tang ca",
                    "Don gia/gio (VND)", "Luong thuc nhan (VND)"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                hRow.createCell(i).setCellValue(headers[i]);
            }
            double totalSalary = 0;
            for (int i = 0; i < report.size(); i++) {
                SalaryDTO s = report.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(s.getMemberName());
                row.createCell(2).setCellValue(s.getTotalTasks());
                row.createCell(3).setCellValue(s.getCompletedTasks());
                row.createCell(4).setCellValue(s.getRegularHours());
                row.createCell(5).setCellValue(s.getOvertimeHours());
                row.createCell(6).setCellValue(s.getHourlyRate());
                double salary = (s.getRegularHours() > 0 ? s.getRegularHours() : s.getTotalWorkload()) * s.getHourlyRate()
                        + (s.getOvertimeHours() * s.getOvertimeRate());
                totalSalary += salary;
                row.createCell(7).setCellValue(salary);
            }
            Row totalRow = sheet.createRow(report.size() + 1);
            totalRow.createCell(0).setCellValue("");
            totalRow.createCell(1).setCellValue("TONG");
            totalRow.createCell(7).setCellValue(totalSalary);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public Map<String, Object> payoutSalary(UUID teamId, UUID actorId) {
        List<SalaryDTO> report = getSalaryReport(teamId);
        double totalSalary = report.stream()
                .mapToDouble(s -> {
                    // Use attendance hours if available, fallback to task workload (same as getSalaryReport)
                    double billableRegular = s.getRegularHours() > 0 ? s.getRegularHours() : s.getTotalWorkload();
                    double overtimeHours = s.getOvertimeHours() > 0 ? s.getOvertimeHours() : 0;
                    return (billableRegular * s.getHourlyRate())
                            + (overtimeHours * s.getOvertimeRate());
                })
                .sum();
        if (totalSalary <= 0) {
            throw new RuntimeException("Khong co luong de phat cho nhom nay.");
        }
        User actor = userRepo.findById(actorId).orElseThrow(() -> new RuntimeException("User not found"));
        String description = "Phat luong thang cho " + report.size() + " nhan vien. Tong quy: "
                + String.format("%,.0f", totalSalary) + " VND.";
        notificationService.createAndSend(
                actor,
                "Phat luong thanh cong",
                description,
                "SALARY_PAYOUT",
                null);
        return Map.of(
                "message", "Phat luong thanh cong",
                "totalEmployees", report.size(),
                "totalSalary", totalSalary,
                "currency", "VND");
    }
}
