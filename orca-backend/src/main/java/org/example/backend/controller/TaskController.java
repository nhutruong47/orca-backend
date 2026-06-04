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

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<TaskDTO>> getByMember(@PathVariable UUID memberId) {
        return ResponseEntity.ok(taskService.getByMember(memberId));
    }

    @GetMapping("/member/{memberId}/kpi")
    public ResponseEntity<?> getMemberKpi(@PathVariable UUID memberId) {
        return ResponseEntity.ok(taskService.getMemberKpi(memberId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaskDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.create(dto, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody TaskDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.update(id, dto, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.updateStatus(id, body.get("status"), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable UUID id, @RequestBody Map<String, Integer> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.updateProgress(id, body.get("percentage"), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/workload")
    public ResponseEntity<?> updateWorkload(@PathVariable UUID id, @RequestBody Map<String, Double> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.updateWorkload(id, body.get("actualWorkload"), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.assign(id, UUID.fromString(body.get("memberId")), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(taskService.transferPrimary(
                    id,
                    UUID.fromString(body.get("toMemberId")),
                    body.get("reason"),
                    user,
                    body.getOrDefault("actorType", "MANAGER")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/transfers")
    public ResponseEntity<?> getTransfers(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTransfers(id));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<?> addDependency(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.addDependency(
                    id,
                    UUID.fromString(body.get("dependsOnTaskId")),
                    body.getOrDefault("dependencyType", "FINISH_TO_START"),
                    user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/dependencies")
    public ResponseEntity<?> getDependencies(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getDependencies(id));
    }

    @PatchMapping("/{id}/backup")
    public ResponseEntity<?> setBackup(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.setBackup(id, UUID.fromString(body.get("memberId")), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/supervisor")
    public ResponseEntity<?> setSupervisor(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(taskService.setSupervisor(id, UUID.fromString(body.get("memberId")), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // === Checklist ===
    @GetMapping("/{id}/checklists")
    public ResponseEntity<?> getChecklist(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getChecklist(id));
    }

    @GetMapping("/{id}/checklist")
    public ResponseEntity<?> getChecklistAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getChecklist(id));
    }

    @PostMapping("/{id}/checklists")
    public ResponseEntity<?> addChecklistItem(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(taskService.addChecklistItem(id, body.get("content")));
    }

    @PatchMapping("/checklists/{checklistId}/toggle")
    public ResponseEntity<?> toggleChecklist(@PathVariable UUID checklistId, @AuthenticationPrincipal User user) {
        taskService.toggleChecklistItem(checklistId, user);
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
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            taskService.delete(id, user);
            return ResponseEntity.ok(Map.of("message", "Đã xóa task"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
