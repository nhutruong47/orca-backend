package org.example.backend.service;

import org.example.backend.dto.GoalDTO;
import org.example.backend.dto.AiParseResult;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class GoalService {

    private final GoalRepository goalRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final TeamRepository teamRepo;
    private final TeamMemberRepository teamMemberRepo;
    private final AiServiceClient aiServiceClient;

    public GoalService(GoalRepository goalRepo, TaskRepository taskRepo, UserRepository userRepo,
            TeamRepository teamRepo, TeamMemberRepository teamMemberRepo, AiServiceClient aiServiceClient) {
        this.goalRepo = goalRepo;
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.teamRepo = teamRepo;
        this.teamMemberRepo = teamMemberRepo;
        this.aiServiceClient = aiServiceClient;
    }

    /** Lấy goals theo team */
    public List<GoalDTO> getByTeam(UUID teamId) {
        return goalRepo.findByTeamId(teamId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /** Tạo goal trong team — Owner nhập mục tiêu + deadline, AI chia task */
    public GoalDTO create(GoalDTO dto, User currentUser) {
        Team team = teamRepo.findById(UUID.fromString(dto.getTeamId()))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Chỉ Owner được tạo goal
        if (!isTeamManager(team, currentUser)) {
            throw new RuntimeException("Only managers can create production plans");
        }

        Goal g = new Goal();
        g.setTitle(dto.getTitle());
        g.setOutputTarget(dto.getOutputTarget());
        g.setRawInstruction(dto.getRawInstruction());
        g.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        g.setChatLog(dto.getChatLog());

        // Parse deadline from string
        LocalDateTime parsedDeadline = parseDeadline(dto.getDeadline());
        g.setDeadline(parsedDeadline);

        g.setTeam(team);
        g.setOwner(currentUser);
        g.setStatus("PLANNING");

        Goal saved = goalRepo.save(g);

        List<Map<String, Object>> generatedTasks = dto.getTasks();
        boolean explicitApprovedTasks = generatedTasks != null && !generatedTasks.isEmpty();
        boolean wantAi = dto.getUseAi() != null && dto.getUseAi();

        if ((generatedTasks == null || generatedTasks.isEmpty()) && wantAi && currentUser.isAiTrialActive()) {
            // ONLY re-generate if no explicit tasks list was provided from the frontend preview
            List<TeamMember> teamMembers = teamMemberRepo.findByTeamId(team.getId());
            Map<String, List<String>> memberLabels = new java.util.LinkedHashMap<>();
            List<String> memberNames = new ArrayList<>();
            for (TeamMember tm : teamMembers) {
                String name = tm.getUser().getUsername();
                memberNames.add(name);
                memberLabels.put(name, tm.getJobLabels() != null ? tm.getJobLabels() : Collections.emptyList());
            }

            AiParseResult parseResult = aiServiceClient.generateTaskPlan(
                    dto.getOutputTarget(),
                    dto.getDeadline() != null ? dto.getDeadline() : "",
                    saved.getPriority(),
                    team.getId(),
                    memberLabels);

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                saved.setAiParsedData(mapper.writeValueAsString(parseResult));
            } catch (Exception e) {
                System.err.println("Lỗi lưu JSON aiParsedData: " + e.getMessage());
            }

            generatedTasks = parseResult.getTasks() != null ? parseResult.getTasks() : new ArrayList<>();
        } else if (generatedTasks != null && !generatedTasks.isEmpty()) {
            System.out.println("✅ Using explicitly passed approved tasks count: " + generatedTasks.size());
        } else {
            generatedTasks = new ArrayList<>();
        }
        
        List<TeamMember> assignableMembers = teamMemberRepo.findByTeamId(team.getId());
        List<String> memberNames = assignableMembers.stream()
                .map(tm -> tm.getUser().getUsername())
                .collect(Collectors.toList());


        if (!generatedTasks.isEmpty()) {
            // Tạo tasks từ AI plan
            int totalTaskCount = generatedTasks.size();
            int memberIndex = 0;
            double totalGoalTarget = extractQuantity(saved.getOutputTarget());
            for (Map<String, Object> tp : generatedTasks) {
                Task task = new Task();
                task.setGoal(saved);
                task.setTitle(requireTaskTitle(tp));
                String suggestedRole = (String) tp.get("assigneeRole");
                String desc = (String) tp.get("description");
                if (suggestedRole != null) {
                    desc = "[Vai trò gợi ý: " + suggestedRole + "] " + (desc != null ? desc : "");
                }
                task.setDescription(desc);

                task.setWorkload(parseWorkload(tp.get("workload"), explicitApprovedTasks));
                task.setPriority(parsePriority(tp.get("priority")));
                
                task.setDeadline(parsedDeadline);
                task.setOutputTarget(totalGoalTarget > 0 ? totalGoalTarget : 0.0);
                task.setActualOutput(0.0);
                task.setStatus("READY");

                // FE-approved task ưu tiên memberId/suggestedAssigneeId. Legacy AI task vẫn fallback round-robin.
                boolean assigned = false;
                User assignedUser = null;
                String memberId = firstNonBlank(asString(tp.get("memberId")), asString(tp.get("suggestedAssigneeId")));
                if (memberId != null) {
                    assignedUser = resolveTeamMember(team.getId(), memberId);
                    task.setMember(assignedUser);
                    assigned = true;
                }

                String aiAssignee = firstText(tp,
                        "assignee",
                        "suggestedAssignee",
                        "primaryWorker",
                        "worker",
                        "member",
                        "employee",
                        "nhanSu",
                        "personnel");
                if (aiAssignee != null && !aiAssignee.isEmpty()) {
                    Optional<User> matchedUser = findTeamUser(assignableMembers, aiAssignee.trim());
                    if (matchedUser.isPresent()) {
                        assignedUser = matchedUser.get();
                        task.setMember(assignedUser);
                        assigned = true;
                        System.out.println("✅ AI giao task '" + task.getTitle() + "' cho: " + aiAssignee);
                    }
                }
                if (!assigned && !explicitApprovedTasks && !memberNames.isEmpty()) {
                    String fallback = memberNames.get(memberIndex % memberNames.size());
                    assignedUser = userRepo.findByUsername(fallback).orElse(null);
                    if (assignedUser != null) {
                        task.setMember(assignedUser);
                    }
                    memberIndex++;
                    System.out.println("⚠️ Fallback round-robin giao task '" + task.getTitle() + "' cho: " + fallback);
                }

                String aiBackupAssignee = firstText(tp,
                        "backupMember",
                        "backupAssignee",
                        "backupWorker",
                        "replacement",
                        "substitute",
                        "secondaryAssignee",
                        "backupPerson");
                if (aiBackupAssignee != null && !aiBackupAssignee.isEmpty()) {
                    Optional<User> matchedBackup = findTeamUser(assignableMembers, aiBackupAssignee.trim());
                    if (matchedBackup.isPresent() && !matchedBackup.get().getId().equals(assignedUser != null ? assignedUser.getId() : null)) {
                        task.setBackupMember(matchedBackup.get());
                        System.out.println("✅ AI gán người thay cho task '" + task.getTitle() + "': " + aiBackupAssignee);
                    }
                } else if (assignedUser != null && !explicitApprovedTasks && !memberNames.isEmpty()) {
                    final User currentAssignee = assignedUser;
                    List<User> candidateBackups = assignableMembers.stream()
                            .map(TeamMember::getUser)
                            .filter(user -> !user.getId().equals(currentAssignee.getId()))
                            .collect(Collectors.toList());
                    if (!candidateBackups.isEmpty()) {
                        User backupUser = candidateBackups.get(memberIndex % candidateBackups.size());
                        task.setBackupMember(backupUser);
                        System.out.println("✅ AI gán người thay fallback cho task '" + task.getTitle() + "': " + backupUser.getUsername());
                    }
                }

                taskRepo.save(task);
            }

            saved.setTotalTasks(totalTaskCount);
            saved.setCompletedTasks(0);
            saved.setStatus("PUBLISHED");
        } else {
            // HẾT HẠN → Tạo goal rỗng, user tự thêm task thủ công
            saved.setTotalTasks(0);
            saved.setCompletedTasks(0);
            saved.setStatus("PUBLISHED");
            saved.setAiParsedData(null);
        }

        goalRepo.save(saved);

        return toDTO(saved);
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

    public GoalDTO updateStatus(UUID id, String status) {
        Goal g = goalRepo.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        g.setStatus(status);

        List<Task> tasks = taskRepo.findByGoalId(id);
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        g.setCompletedTasks((int) completed);
        g.setTotalTasks(tasks.size());

        if (completed == tasks.size() && !tasks.isEmpty()) {
            g.setStatus("DONE");
        }

        return toDTO(goalRepo.save(g));
    }

    public GoalDTO updateStatus(UUID id, String status, User actor) {
        Goal g = goalRepo.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!isTeamManager(g.getTeam(), actor)) {
            throw new RuntimeException("Only managers can update production plans");
        }
        return updateStatus(id, status);
    }

    public void delete(UUID id) {
        taskRepo.deleteAll(taskRepo.findByGoalId(id));
        goalRepo.deleteById(id);
    }

    public void delete(UUID id, User actor) {
        Goal g = goalRepo.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!isTeamManager(g.getTeam(), actor)) {
            throw new RuntimeException("Only managers can delete production plans");
        }
        delete(id);
    }

    private boolean isTeamManager(Team team, User user) {
        if (team == null || user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN || (team.getOwner() != null && team.getOwner().getId().equals(user.getId()))) {
            return true;
        }
        return teamMemberRepo.findByTeamIdAndUserId(team.getId(), user.getId())
                .map(tm -> tm.getGroupRole() == GroupRole.ADMIN)
                .orElse(false);
    }

    private GoalDTO toDTO(Goal g) {
        GoalDTO dto = new GoalDTO();
        dto.setId(g.getId() != null ? g.getId().toString() : null);
        dto.setTitle(g.getTitle());
        dto.setOutputTarget(g.getOutputTarget());
        dto.setRawInstruction(g.getRawInstruction());
        dto.setAiParsedData(g.getAiParsedData());
        dto.setPriority(g.getPriority());
        dto.setStatus(g.getStatus());
        dto.setDeadline(g.getDeadline() != null ? g.getDeadline().toString() : null);
        dto.setTotalTasks(g.getTotalTasks());
        dto.setCompletedTasks(g.getCompletedTasks());
        dto.setTeamId(g.getTeam() != null ? g.getTeam().getId().toString() : null);
        dto.setTeamName(g.getTeam() != null ? g.getTeam().getName() : null);
        dto.setOwnerId(g.getOwner() != null ? g.getOwner().getId().toString() : null);
        dto.setOwnerName(g.getOwner() != null ? g.getOwner().getUsername() : null);
        dto.setCreatedAt(g.getCreatedAt());
        dto.setChatLog(g.getChatLog());
        return dto;
    }

    private LocalDateTime parseDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return null;
        }
        try {
            // Try ISO format first
            return LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // If it's a number (like "10"), treat it as days from now
                int days = Integer.parseInt(deadlineStr.trim());
                return LocalDateTime.now().plusDays(days);
            } catch (NumberFormatException e2) {
                // Fallback: 7 days
                return LocalDateTime.now().plusDays(7);
            }
        }
    }

    private String requireTaskTitle(Map<String, Object> taskPayload) {
        String title = asString(taskPayload.get("title"));
        if (title == null || title.isBlank()) {
            title = asString(taskPayload.get("description"));
        }
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Task title is required");
        }
        return title.trim();
    }

    private Double parseWorkload(Object workloadObj, boolean required) {
        Double workload = null;
        if (workloadObj instanceof Number number) {
            workload = number.doubleValue();
        } else if (workloadObj instanceof String str && !str.isBlank()) {
            try {
                workload = Double.parseDouble(str);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Task workload must be a number");
            }
        }

        if (workload == null) {
            if (required) {
                throw new RuntimeException("Task workload is required");
            }
            return 1.0;
        }
        if (workload <= 0) {
            throw new RuntimeException("Task workload must be greater than 0");
        }
        return workload;
    }

    private Integer parsePriority(Object priorityObj) {
        Integer priority = null;
        if (priorityObj instanceof Number number) {
            priority = number.intValue();
        } else if (priorityObj instanceof String str && !str.isBlank()) {
            try {
                priority = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Task priority must be a number from 1 to 5");
            }
        }

        if (priority == null) {
            return 2;
        }
        if (priority < 1 || priority > 5) {
            throw new RuntimeException("Task priority must be between 1 and 5");
        }
        return priority;
    }

    private User resolveTeamMember(UUID teamId, String userId) {
        UUID memberUuid;
        try {
            memberUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid task memberId");
        }

        if (!teamMemberRepo.existsByTeamIdAndUserId(teamId, memberUuid)) {
            throw new RuntimeException("Assigned member must belong to the team");
        }
        return userRepo.findById(memberUuid)
                .orElseThrow(() -> new RuntimeException("Assigned member not found"));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String text && !text.trim().isEmpty()) {
                return text.trim();
            }
        }
        return null;
    }

    private Optional<User> findTeamUser(List<TeamMember> members, String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return members.stream()
                .map(TeamMember::getUser)
                .filter(user -> {
                    String username = user.getUsername() != null ? user.getUsername().toLowerCase(Locale.ROOT) : "";
                    String fullName = user.getFullName() != null ? user.getFullName().toLowerCase(Locale.ROOT) : "";
                    return username.equals(normalized)
                            || fullName.equals(normalized)
                            || username.contains(normalized)
                            || fullName.contains(normalized);
                })
                .findFirst();
    }
}
