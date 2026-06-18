package org.example.backend.service;

import org.example.backend.dto.AttendanceDTO;
import org.example.backend.entity.Attendance;
import org.example.backend.entity.Attendance.ShiftType;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.AttendanceRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final UserRepository userRepo;
    private final TeamRepository teamRepo;
    private final ProductionOrderRepository orderRepo;

    private static final Map<ShiftType, String[]> SHIFT_HOURS = Map.of(
        ShiftType.SANG, new String[]{"06:00", "14:00"},
        ShiftType.CHIEU, new String[]{"14:00", "22:00"},
        ShiftType.DEM, new String[]{"22:00", "06:00"},
        ShiftType.NGAY, new String[]{"06:00", "18:00"}
    );

    public AttendanceService(AttendanceRepository attendanceRepo, UserRepository userRepo,
                            TeamRepository teamRepo, ProductionOrderRepository orderRepo) {
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
        this.teamRepo = teamRepo;
        this.orderRepo = orderRepo;
    }

    public AttendanceDTO checkIn(UUID userId, UUID teamId, ShiftType shiftType,
                                 Attendance.ProductionStage stage, UUID orderId, Integer breakMinutes) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepo.findByUserIdAndTeamIdAndDate(userId, teamId, today);
        if (existing.isPresent()) {
            throw new RuntimeException("Ban da check-in hom nay roi");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        Attendance a = new Attendance();
        a.setUser(user);
        a.setTeam(team);
        a.setDate(today);
        a.setCheckInTime(LocalDateTime.now());
        a.setShiftType(shiftType);
        a.setStage(stage);
        a.setBreakMinutes(breakMinutes != null ? breakMinutes : 30);

        String[] hours = SHIFT_HOURS.get(shiftType);
        a.setShiftStartTime(hours[0]);
        a.setShiftEndTime(hours[1]);

        if (orderId != null) {
            ProductionOrder order = orderRepo.findById(orderId).orElse(null);
            a.setProductionOrder(order);
        }

        return toDTO(attendanceRepo.save(a));
    }

    public AttendanceDTO checkOut(UUID userId, UUID teamId) {
        LocalDate today = LocalDate.now();
        Attendance a = attendanceRepo.findByUserIdAndTeamIdAndDate(userId, teamId, today)
                .orElseThrow(() -> new RuntimeException("Ban chua check-in hom nay"));

        if (a.getCheckOutTime() != null) {
            throw new RuntimeException("Ban da check-out hom nay roi");
        }

        a.setCheckOutTime(LocalDateTime.now());

        Duration workDuration = Duration.between(a.getCheckInTime(), a.getCheckOutTime());
        double totalMinutes = workDuration.toMinutes();
        double breakMins = a.getBreakMinutes() != null ? a.getBreakMinutes() : 30;
        double actualWorkMinutes = Math.max(0, totalMinutes - breakMins);
        double actualWorkHours = Math.round(actualWorkMinutes / 60.0 * 10.0) / 10.0;
        a.setActualWorkHours(actualWorkHours);

        double regularHours = Math.min(actualWorkHours, 8.0);
        double overtimeHours = Math.max(0, actualWorkHours - 8.0);
        a.setRegularHours(Math.round(regularHours * 10.0) / 10.0);
        a.setOvertimeHours(Math.round(overtimeHours * 10.0) / 10.0);

        if (a.getShiftType() != null) {
            String expectedStart = a.getShiftStartTime();
            if (expectedStart != null) {
                int expectedHour = Integer.parseInt(expectedStart.split(":")[0]);
                int actualHour = a.getCheckInTime().getHour();
                if (actualHour > expectedHour) {
                    a.setAttendanceStatus(Attendance.AttendanceStatus.LATE);
                } else {
                    a.setAttendanceStatus(Attendance.AttendanceStatus.ON_TIME);
                }
            } else {
                a.setAttendanceStatus(Attendance.AttendanceStatus.ON_TIME);
            }
        } else {
            a.setAttendanceStatus(Attendance.AttendanceStatus.ON_TIME);
        }

        return toDTO(attendanceRepo.save(a));
    }

    public AttendanceDTO getTodayAttendance(UUID userId, UUID teamId) {
        LocalDate today = LocalDate.now();
        return attendanceRepo.findByUserIdAndTeamIdAndDate(userId, teamId, today)
                .map(this::toDTO)
                .orElse(null);
    }

    public List<AttendanceDTO> getAttendanceHistory(UUID userId, UUID teamId) {
        return attendanceRepo.findByUserIdAndTeamId(userId, teamId).stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getTeamAttendanceToday(UUID teamId) {
        LocalDate today = LocalDate.now();
        return attendanceRepo.findByTeamIdAndDate(teamId, today).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Double> getStageWorkerHours(UUID teamId, LocalDate date) {
        List<Attendance> attendances = attendanceRepo.findByTeamIdAndDate(teamId, date);
        return attendances.stream()
                .filter(a -> a.getStage() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getStage().name(),
                        Collectors.summingDouble(a -> a.getRegularHours() + a.getOvertimeHours())
                ));
    }

    public Double getOrderWorkerHours(UUID orderId) {
        List<Attendance> attendances = attendanceRepo.findByOrderId(orderId);
        return attendances.stream()
                .mapToDouble(a -> a.getRegularHours() + a.getOvertimeHours())
                .sum();
    }

    private AttendanceDTO toDTO(Attendance a) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(a.getId().toString());
        dto.setUserId(a.getUser().getId().toString());
        dto.setUserName(a.getUser().getFullName());
        dto.setTeamId(a.getTeam().getId().toString());
        dto.setDate(a.getDate());
        dto.setShiftType(a.getShiftType() != null ? a.getShiftType().name() : null);
        dto.setShiftStartTime(a.getShiftStartTime());
        dto.setShiftEndTime(a.getShiftEndTime());
        dto.setCheckInTime(a.getCheckInTime());
        dto.setCheckOutTime(a.getCheckOutTime());
        dto.setProductionStage(a.getStage() != null ? a.getStage().name() : null);
        if (a.getProductionOrder() != null) {
            dto.setOrderId(a.getProductionOrder().getId().toString());
            dto.setOrderTitle(a.getProductionOrder().getTitle());
        }
        dto.setBreakMinutes(a.getBreakMinutes());
        dto.setActualWorkHours(a.getActualWorkHours());
        dto.setRegularHours(a.getRegularHours());
        dto.setOvertimeHours(a.getOvertimeHours());
        dto.setAttendanceStatus(a.getAttendanceStatus() != null ? a.getAttendanceStatus().name() : null);
        dto.setNotes(a.getNotes());
        return dto;
    }
}
