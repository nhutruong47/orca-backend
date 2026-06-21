package org.example.backend.controller;

import org.example.backend.dto.AttendanceDTO;
import org.example.backend.dto.UpdateAttendanceRequest;
import org.example.backend.entity.Attendance;
import org.example.backend.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin("*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in/{userId}/{teamId}")
    public ResponseEntity<?> checkIn(
            @PathVariable UUID userId,
            @PathVariable UUID teamId,
            @RequestBody(required = false) Map<String, Object> body) {

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

    @PostMapping("/check-out/{userId}/{teamId}")
    public ResponseEntity<?> checkOut(@PathVariable UUID userId, @PathVariable UUID teamId) {
        try {
            return ResponseEntity.ok(attendanceService.checkOut(userId, teamId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/today/{userId}/{teamId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable UUID userId, @PathVariable UUID teamId) {
        AttendanceDTO dto = attendanceService.getTodayAttendance(userId, teamId);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history/{userId}/{teamId}")
    public ResponseEntity<?> getAttendanceHistory(@PathVariable UUID userId, @PathVariable UUID teamId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(userId, teamId));
    }

    @GetMapping("/team-today/{teamId}")
    public ResponseEntity<?> getTeamAttendanceToday(@PathVariable UUID teamId) {
        return ResponseEntity.ok(attendanceService.getTeamAttendanceToday(teamId));
    }

    @GetMapping("/team-history/{teamId}")
    public ResponseEntity<?> getTeamAttendanceHistory(@PathVariable UUID teamId) {
        return ResponseEntity.ok(attendanceService.getTeamAttendanceHistory(teamId));
    }

    @PutMapping("/update/{attendanceId}")
    public ResponseEntity<?> updateAttendance(@PathVariable UUID attendanceId, @RequestBody UpdateAttendanceRequest req) {
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
