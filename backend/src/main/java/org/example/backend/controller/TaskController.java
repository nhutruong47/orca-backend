package org.example.backend.controller;

import org.example.backend.dto.TaskDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private AccessControlService accessControlService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getAll());
    }

    @GetMapping("/by-goal/{goalId}")
    public ResponseEntity<?> getByGoal(@PathVariable UUID goalId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireGoalAccess(user, goalId);
            return ResponseEntity.ok(taskService.getByGoal(goalId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<?> getByMember(@PathVariable UUID memberId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getByMember(memberId));
    }

    @GetMapping("/member/{memberId}/kpi")
    public ResponseEntity<?> getMemberKpi(@PathVariable UUID memberId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.getMemberKpi(memberId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskDetail(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaskDTO dto, @AuthenticationPrincipal User user) {
        try {
            if (dto != null && dto.getGoalId() != null) {
                accessControlService.requireGoalAccess(user, UUID.fromString(dto.getGoalId()));
            }
            return ResponseEntity.ok(taskService.create(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable UUID id, @RequestBody Map<String, Object> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.update(id, body));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.updateStatus(id, body.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable UUID id, @RequestBody Map<String, Integer> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.updateProgress(id, body.get("percentage")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/workload")
    public ResponseEntity<?> updateWorkload(@PathVariable UUID id, @RequestBody Map<String, Double> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.updateWorkload(id, body.get("actualWorkload")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.assign(id, UUID.fromString(body.get("memberId"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/backup")
    public ResponseEntity<?> setBackup(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.setBackup(id, UUID.fromString(body.get("memberId"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/supervisor")
    public ResponseEntity<?> setSupervisor(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.setSupervisor(id, UUID.fromString(body.get("memberId"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable UUID id, @RequestBody Map<String, Object> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            String toMemberId = body.get("toMemberId") != null ? body.get("toMemberId").toString() : null;
            String reason = body.get("reason") != null ? body.get("reason").toString() : null;
            String actorType = body.get("actorType") != null ? body.get("actorType").toString() : null;
            return ResponseEntity.ok(taskService.transferTask(id, UUID.fromString(toMemberId), reason, actorType, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/transfers")
    public ResponseEntity<?> getTransfers(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.getTransfers(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<?> addDependency(@PathVariable UUID id, @RequestBody Map<String, Object> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            String dependsOnTaskId = body.get("dependsOnTaskId") != null ? body.get("dependsOnTaskId").toString() : null;
            String type = body.get("dependencyType") != null ? body.get("dependencyType").toString() : "FINISH_TO_START";
            return ResponseEntity.ok(taskService.addDependency(id, UUID.fromString(dependsOnTaskId), type));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/dependencies")
    public ResponseEntity<?> getDependencies(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.getDependencies(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // === Checklist (canonical) ===
    @GetMapping("/{id}/checklists")
    public ResponseEntity<?> getChecklist(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.getChecklist(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    @PostMapping("/{id}/checklists")
    public ResponseEntity<?> addChecklistItem(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            return ResponseEntity.ok(taskService.addChecklistItem(id, body.get("content")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Frontend uses /checklist/ (singular) + PATCH .../toggle
    @PatchMapping("/checklists/{checklistId}/toggle")
    public ResponseEntity<?> toggleChecklist(@PathVariable UUID checklistId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireChecklistAccess(user, checklistId);
            taskService.toggleChecklistItem(checklistId);
            return ResponseEntity.ok(Map.of("message", "Toggled"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<?> getSalaryReport(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(taskService.getSalaryReport(teamId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/salary/{teamId}/export-excel")
    public ResponseEntity<byte[]> exportSalaryExcel(@PathVariable UUID teamId, @AuthenticationPrincipal User user) throws Exception {
        accessControlService.requireTeamMember(user, teamId);
        byte[] excelBytes = taskService.exportSalaryExcel(teamId);
        String filename = "bang-luong-" + teamId + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excelBytes);
    }

    @PostMapping("/salary/{teamId}/payout")
    public ResponseEntity<?> payoutSalary(@PathVariable UUID teamId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskService.payoutSalary(teamId, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTaskAccess(user, id);
            taskService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa task"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
