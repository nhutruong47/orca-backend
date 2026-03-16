package org.example.backend.dto;

public class SalaryDTO {
    private String memberId;
    private String memberName;
    private int totalTasks;
    private int completedTasks;
    private double totalWorkload;
    private double totalActualWorkload;
    private double hourlyRate;
    private double estimatedSalary;

    // === Getters & Setters ===
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public double getTotalWorkload() { return totalWorkload; }
    public void setTotalWorkload(double totalWorkload) { this.totalWorkload = totalWorkload; }

    public double getTotalActualWorkload() { return totalActualWorkload; }
    public void setTotalActualWorkload(double totalActualWorkload) { this.totalActualWorkload = totalActualWorkload; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public double getEstimatedSalary() { return estimatedSalary; }
    public void setEstimatedSalary(double estimatedSalary) { this.estimatedSalary = estimatedSalary; }
}
