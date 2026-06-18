package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendances")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", length = 20)
    private ShiftType shiftType;

    @Column(name = "shift_start_time")
    private String shiftStartTime;

    @Column(name = "shift_end_time")
    private String shiftEndTime;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_stage", length = 50)
    private ProductionStage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;

    @Column(name = "break_minutes")
    private Integer breakMinutes = 30;

    @Column(name = "actual_work_hours")
    private Double actualWorkHours;

    @Column(name = "regular_hours")
    private Double regularHours = 0.0;

    @Column(name = "overtime_hours")
    private Double overtimeHours = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 20)
    private AttendanceStatus attendanceStatus = AttendanceStatus.ON_TIME;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ShiftType {
        SANG,    // Ca sang: 6:00 - 14:00
        CHIEU,   // Ca chieu: 14:00 - 22:00
        DEM,     // Ca dem: 22:00 - 6:00
        NGAY     // Lam ca ngay: 6:00 - 18:00
    }

    public enum ProductionStage {
        RANH_VA_CHON,  // Rang va chon
        RANG,           // Rang
        XAY,            // Xay
        DONG_GOI,       // Dong goi
        QA              // Kiem tra chat luong
    }

    public enum AttendanceStatus {
        ON_TIME,
        LATE,
        MISSING_CHECKOUT,
        ABSENT
    }

    public Attendance() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftType shiftType) { this.shiftType = shiftType; }
    public String getShiftStartTime() { return shiftStartTime; }
    public void setShiftStartTime(String shiftStartTime) { this.shiftStartTime = shiftStartTime; }
    public String getShiftEndTime() { return shiftEndTime; }
    public void setShiftEndTime(String shiftEndTime) { this.shiftEndTime = shiftEndTime; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public ProductionStage getStage() { return stage; }
    public void setStage(ProductionStage stage) { this.stage = stage; }
    public ProductionOrder getProductionOrder() { return productionOrder; }
    public void setProductionOrder(ProductionOrder productionOrder) { this.productionOrder = productionOrder; }
    public Integer getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(Integer breakMinutes) { this.breakMinutes = breakMinutes; }
    public Double getActualWorkHours() { return actualWorkHours; }
    public void setActualWorkHours(Double actualWorkHours) { this.actualWorkHours = actualWorkHours; }
    public Double getRegularHours() { return regularHours; }
    public void setRegularHours(Double regularHours) { this.regularHours = regularHours; }
    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
