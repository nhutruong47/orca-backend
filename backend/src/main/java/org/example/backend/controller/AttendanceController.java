package org.example.backend.controller;

import org.example.backend.dto.AttendanceDTO;
import org.example.backend.dto.UpdateAttendanceRequest;
import org.example.backend.entity.Attendance;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin("*")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AccessControlService accessControlService;

    public AttendanceController(AttendanceService attendanceService, AccessControlService accessControlService) {
        this.attendanceService = attendanceService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/check-in/{teamId}")
    public ResponseEntity<?> checkIn(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, Object> body) {

        accessControlService.requireTeamMember(currentUser, teamId);
        UUID userId = currentUser.getId();
        try {
            Attendance.ShiftType shiftType = Attendance.ShiftType.SANG;
            Attendance.ProductionStage stage = null;
            UUID orderId = null;
            Integer breakMinutes = 30;

            if (body != null) {
                if (body.get("shiftType") != null) {
                    shiftType = Attendance.ShiftType.valueOf(body.get("shiftType").toString());
                }
                if (body.get("stage") != null) {
                    stage = Attendance.ProductionStage.valueOf(body.get("stage").toString());
                }
                if (body.get("orderId") != null) {
                    orderId = UUID.fromString(body.get("orderId").toString());
                }
                if (body.get("breakMinutes") != null) {
                    breakMinutes = Integer.parseInt(body.get("breakMinutes").toString());
                }
            }

            AttendanceDTO dto = attendanceService.checkIn(userId, teamId, shiftType, stage, orderId, breakMinutes);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/check-out/{teamId}")
    public ResponseEntity<?> checkOut(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        UUID userId = currentUser.getId();
        try {
            return ResponseEntity.ok(attendanceService.checkOut(userId, teamId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/today/{teamId}")
    public ResponseEntity<?> getTodayAttendance(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        UUID userId = currentUser.getId();
        AttendanceDTO dto = attendanceService.getTodayAttendance(userId, teamId);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history/{teamId}")
    public ResponseEntity<?> getAttendanceHistory(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        UUID userId = currentUser.getId();
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(userId, teamId));
    }

    @GetMapping("/team-today/{teamId}")
    public ResponseEntity<?> getTeamAttendanceToday(@PathVariable UUID teamId, @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        return ResponseEntity.ok(attendanceService.getTeamAttendanceToday(teamId));
    }

    @GetMapping("/team-history/{teamId}")
    public ResponseEntity<?> getTeamAttendanceHistory(@PathVariable UUID teamId, @AuthenticationPrincipal User currentUser) {
        accessControlService.requireTeamMember(currentUser, teamId);
        return ResponseEntity.ok(attendanceService.getTeamAttendanceHistory(teamId));
    }

    @PutMapping("/update/{attendanceId}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable UUID attendanceId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateAttendanceRequest req) {
        accessControlService.requireAttendanceAccess(currentUser, attendanceId);
        try {
            return ResponseEntity.ok(attendanceService.updateAttendance(attendanceId, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stages")
    public ResponseEntity<?> getProductionStages() {
        return ResponseEntity.ok(Attendance.ProductionStage.values());
    }

    @GetMapping("/shifts")
    public ResponseEntity<?> getShiftTypes() {
        return ResponseEntity.ok(Attendance.ShiftType.values());
    }
}
