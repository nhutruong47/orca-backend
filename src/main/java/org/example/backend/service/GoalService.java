package org.example.backend.service;

import org.example.backend.dto.GoalDTO;
import org.example.backend.dto.AiParseResult;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
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
        if (!team.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the group owner can create goals");
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
        
        List<String> memberNames = teamMemberRepo.findByTeamId(team.getId()).stream()
                .map(tm -> tm.getUser().getUsername())
                .collect(Collectors.toList());


        if (!generatedTasks.isEmpty()) {
            // Tạo tasks từ AI plan
            int taskCount = 0;
            int memberIndex = 0;
            for (Map<String, Object> tp : generatedTasks) {
                Task task = new Task();
                task.setGoal(saved);
                task.setTitle((String) tp.get("title"));
                String suggestedRole = (String) tp.get("assigneeRole");
                String desc = (String) tp.get("description");
                if (suggestedRole != null) {
                    desc = "[Vai trò gợi ý: " + suggestedRole + "] " + (desc != null ? desc : "");
                }
                task.setDescription(desc);
                
                Object workloadObj = tp.get("workload");
                if (workloadObj instanceof Number) {
                    task.setWorkload(((Number) workloadObj).doubleValue());
                } else if (workloadObj instanceof String) {
                    try { task.setWorkload(Double.parseDouble((String) workloadObj)); } catch (Exception e) { task.setWorkload(1.0); }
                } else {
                    task.setWorkload(1.0);
                }

                Object priorityObj = tp.get("priority");
                if (priorityObj instanceof Number) {
                    task.setPriority(((Number) priorityObj).intValue());
                } else if (priorityObj instanceof String) {
                    try { task.setPriority(Integer.parseInt((String) priorityObj)); } catch (Exception e) { task.setPriority(2); }
                } else {
                    task.setPriority(2);
                }
                
                task.setDeadline(parsedDeadline);
                task.setStatus("PENDING");

                // Phân công (Assignee): Ưu tiên tên AI chỉ định, fallback round-robin
                String aiAssignee = (String) tp.get("assignee");
                boolean assigned = false;
                if (aiAssignee != null && !aiAssignee.isEmpty()) {
                    Optional<User> matchedUser = userRepo.findByUsernameIgnoreCase(aiAssignee.trim());
                    if (matchedUser.isPresent()) {
                        task.setMember(matchedUser.get());
                        assigned = true;
                        System.out.println("✅ AI giao task '" + task.getTitle() + "' cho: " + aiAssignee);
                    }
                }
                if (!assigned && !memberNames.isEmpty()) {
                    String fallback = memberNames.get(memberIndex % memberNames.size());
                    userRepo.findByUsername(fallback).ifPresent(task::setMember);
                    memberIndex++;
                    System.out.println("⚠️ Fallback round-robin giao task '" + task.getTitle() + "' cho: " + fallback);
                }

                taskRepo.save(task);
                taskCount++;
            }

            saved.setTotalTasks(taskCount);
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

    public void delete(UUID id) {
        taskRepo.deleteAll(taskRepo.findByGoalId(id));
        goalRepo.deleteById(id);
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
}
