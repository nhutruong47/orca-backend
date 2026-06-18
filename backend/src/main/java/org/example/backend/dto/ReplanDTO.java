package org.example.backend.dto;

import java.util.List;
import java.util.UUID;

public class ReplanDTO {

    private UUID planId;
    private UUID orderId;
    private String orderCode;
    private double originalTargetKg;
    private double completedKg;
    private double remainingKg;
    private double shortFall;
    private int originalWorkingDays;
    private int remainingWorkingDays;
    private int newWorkingDaysNeeded;
    private List<ReplanDay> revisedDays;
    private String replanStrategy;
    private List<String> recommendations;
    private String riskLevel;
    private boolean needsReplan;

    public static class ReplanDay {
        private int dayIndex;
        private String date;
        private double originalTargetKg;
        private double revisedTargetKg;
        private double actualKg;
        private double cumulativeActual;
        private double cumulativeTarget;
        private String status;

        public int getDayIndex() { return dayIndex; }
        public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public double getOriginalTargetKg() { return originalTargetKg; }
        public void setOriginalTargetKg(double originalTargetKg) { this.originalTargetKg = originalTargetKg; }
        public double getRevisedTargetKg() { return revisedTargetKg; }
        public void setRevisedTargetKg(double revisedTargetKg) { this.revisedTargetKg = revisedTargetKg; }
        public double getActualKg() { return actualKg; }
        public void setActualKg(double actualKg) { this.actualKg = actualKg; }
        public double getCumulativeActual() { return cumulativeActual; }
        public void setCumulativeActual(double cumulativeActual) { this.cumulativeActual = cumulativeActual; }
        public double getCumulativeTarget() { return cumulativeTarget; }
        public void setCumulativeTarget(double cumulativeTarget) { this.cumulativeTarget = cumulativeTarget; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public double getOriginalTargetKg() { return originalTargetKg; }
    public void setOriginalTargetKg(double originalTargetKg) { this.originalTargetKg = originalTargetKg; }
    public double getCompletedKg() { return completedKg; }
    public void setCompletedKg(double completedKg) { this.completedKg = completedKg; }
    public double getRemainingKg() { return remainingKg; }
    public void setRemainingKg(double remainingKg) { this.remainingKg = remainingKg; }
    public double getShortFall() { return shortFall; }
    public void setShortFall(double shortFall) { this.shortFall = shortFall; }
    public int getOriginalWorkingDays() { return originalWorkingDays; }
    public void setOriginalWorkingDays(int originalWorkingDays) { this.originalWorkingDays = originalWorkingDays; }
    public int getRemainingWorkingDays() { return remainingWorkingDays; }
    public void setRemainingWorkingDays(int remainingWorkingDays) { this.remainingWorkingDays = remainingWorkingDays; }
    public int getNewWorkingDaysNeeded() { return newWorkingDaysNeeded; }
    public void setNewWorkingDaysNeeded(int newWorkingDaysNeeded) { this.newWorkingDaysNeeded = newWorkingDaysNeeded; }
    public List<ReplanDay> getRevisedDays() { return revisedDays; }
    public void setRevisedDays(List<ReplanDay> revisedDays) { this.revisedDays = revisedDays; }
    public String getReplanStrategy() { return replanStrategy; }
    public void setReplanStrategy(String replanStrategy) { this.replanStrategy = replanStrategy; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public boolean isNeedsReplan() { return needsReplan; }
    public void setNeedsReplan(boolean needsReplan) { this.needsReplan = needsReplan; }
}
