package org.example.backend.dto;

import java.time.LocalDateTime;

public class UpdateAttendanceRequest {
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
