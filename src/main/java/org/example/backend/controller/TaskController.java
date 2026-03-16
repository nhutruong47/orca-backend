package org.example.backend.controller;

import org.example.backend.dto.TaskDTO;
import org.example.backend.entity.User;
import org.example.backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAll() {
        return ResponseEntity.ok(taskService.getAll());
    }

    @GetMapping("/by-goal/{goalId}")
    public ResponseEntity<List<TaskDTO>> getByGoal(@PathVariable UUID goalId) {
        return ResponseEntity.ok(taskService.getByGoal(goalId));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskDTO>> getMyTasks(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getByMember(user.getId()));
    }

    @GetMapping("/my-kpi")
    public ResponseEntity<?> getMyKpi(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getMemberKpi(user.getId()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaskDTO dto) {
        try {
            return ResponseEntity.ok(taskService.create(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(taskService.updateStatus(id, body.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/workload")
    public ResponseEntity<?> updateWorkload(@PathVariable UUID id, @RequestBody Map<String, Double> body) {
        try {
            return ResponseEntity.ok(taskService.updateWorkload(id, body.get("actualWorkload")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(taskService.assign(id, UUID.fromString(body.get("memberId"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // === Checklist ===
    @GetMapping("/{id}/checklists")
    public ResponseEntity<?> getChecklist(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getChecklist(id));
    }

    @PostMapping("/{id}/checklists")
    public ResponseEntity<?> addChecklistItem(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(taskService.addChecklistItem(id, body.get("content")));
    }

    @PatchMapping("/checklists/{checklistId}/toggle")
    public ResponseEntity<?> toggleChecklist(@PathVariable UUID checklistId) {
        taskService.toggleChecklistItem(checklistId);
        return ResponseEntity.ok(Map.of("message", "Toggled"));
    }

    @PatchMapping("/{id}/respond")
    public ResponseEntity<?> respondToTask(@PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Boolean> body) {
        try {
            boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
            return ResponseEntity.ok(taskService.respondToTask(id, user.getId(), accepted));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/salary/{teamId}")
    public ResponseEntity<?> getSalaryReport(@PathVariable UUID teamId) {
        return ResponseEntity.ok(taskService.getSalaryReport(teamId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            taskService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa task"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
