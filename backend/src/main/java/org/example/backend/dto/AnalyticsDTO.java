package org.example.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsDTO {

    private WorkerStatsByStage workerStats;
    private OrderStats orderStats;
    private List<DailyProductionRecord> dailyTrend;
    private List<StageEfficiency> stageEfficiency;
    private List<OrderAnalytics> orderAnalytics;
    private double overallProductivity;
    private double totalKgThisWeek;
    private double totalHoursThisWeek;

    public static class WorkerStatsByStage {
        private double roastHours;
        private double qcHours;
        private double packagingHours;
        private double totalHours;
        private double roastKg;
        private double qcKg;
        private double packagingKg;
        private double roastProductivity;
        private double qcProductivity;
        private double packagingProductivity;

        public double getRoastHours() { return roastHours; }
        public void setRoastHours(double roastHours) { this.roastHours = roastHours; }
        public double getQcHours() { return qcHours; }
        public void setQcHours(double qcHours) { this.qcHours = qcHours; }
        public double getPackagingHours() { return packagingHours; }
        public void setPackagingHours(double packagingHours) { this.packagingHours = packagingHours; }
        public double getTotalHours() { return totalHours; }
        public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
        public double getRoastKg() { return roastKg; }
        public void setRoastKg(double roastKg) { this.roastKg = roastKg; }
        public double getQcKg() { return qcKg; }
        public void setQcKg(double qcKg) { this.qcKg = qcKg; }
        public double getPackagingKg() { return packagingKg; }
        public void setPackagingKg(double packagingKg) { this.packagingKg = packagingKg; }
        public double getRoastProductivity() { return roastProductivity; }
        public void setRoastProductivity(double roastProductivity) { this.roastProductivity = roastProductivity; }
        public double getQcProductivity() { return qcProductivity; }
        public void setQcProductivity(double qcProductivity) { this.qcProductivity = qcProductivity; }
        public double getPackagingProductivity() { return packagingProductivity; }
        public void setPackagingProductivity(double packagingProductivity) { this.packagingProductivity = packagingProductivity; }
    }

    public static class OrderStats {
        private long total;
        private long completed;
        private long inProduction;
        private long pending;
        private long atRisk;
        private double avgLeadTime;
        private double avgYield;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }
        public long getInProduction() { return inProduction; }
        public void setInProduction(long inProduction) { this.inProduction = inProduction; }
        public long getPending() { return pending; }
        public void setPending(long pending) { this.pending = pending; }
        public long getAtRisk() { return atRisk; }
        public void setAtRisk(long atRisk) { this.atRisk = atRisk; }
        public double getAvgLeadTime() { return avgLeadTime; }
        public void setAvgLeadTime(double avgLeadTime) { this.avgLeadTime = avgLeadTime; }
        public double getAvgYield() { return avgYield; }
        public void setAvgYield(double avgYield) { this.avgYield = avgYield; }
    }

    public static class DailyProductionRecord {
        private LocalDate date;
        private double targetKg;
        private double actualKg;
        private double completionRate;
        private double roastKg;
        private double qcKg;
        private double packagingKg;
        private double workerHours;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public double getTargetKg() { return targetKg; }
        public void setTargetKg(double targetKg) { this.targetKg = targetKg; }
        public double getActualKg() { return actualKg; }
        public void setActualKg(double actualKg) { this.actualKg = actualKg; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        public double getRoastKg() { return roastKg; }
        public void setRoastKg(double roastKg) { this.roastKg = roastKg; }
        public double getQcKg() { return qcKg; }
        public void setQcKg(double qcKg) { this.qcKg = qcKg; }
        public double getPackagingKg() { return packagingKg; }
        public void setPackagingKg(double packagingKg) { this.packagingKg = packagingKg; }
        public double getWorkerHours() { return workerHours; }
        public void setWorkerHours(double workerHours) { this.workerHours = workerHours; }
    }

    public static class StageEfficiency {
        private String stage;
        private double totalTargetKg;
        private double totalActualKg;
        private double efficiency;
        private double avgProductivity;
        private double failRate;

        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public double getTotalTargetKg() { return totalTargetKg; }
        public void setTotalTargetKg(double totalTargetKg) { this.totalTargetKg = totalTargetKg; }
        public double getTotalActualKg() { return totalActualKg; }
        public void setTotalActualKg(double totalActualKg) { this.totalActualKg = totalActualKg; }
        public double getEfficiency() { return efficiency; }
        public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
        public double getAvgProductivity() { return avgProductivity; }
        public void setAvgProductivity(double avgProductivity) { this.avgProductivity = avgProductivity; }
        public double getFailRate() { return failRate; }
        public void setFailRate(double failRate) { this.failRate = failRate; }
    }

    public static class OrderAnalytics {
        private String orderId;
        private String orderCode;
        private String title;
        private double outputTarget;
        private double completedQuantity;
        private double progressPercent;
        private double expectedYield;
        private double actualYield;
        private double leadTimeDays;
        private String riskLevel;
        private String status;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public double getOutputTarget() { return outputTarget; }
        public void setOutputTarget(double outputTarget) { this.outputTarget = outputTarget; }
        public double getCompletedQuantity() { return completedQuantity; }
        public void setCompletedQuantity(double completedQuantity) { this.completedQuantity = completedQuantity; }
        public double getProgressPercent() { return progressPercent; }
        public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
        public double getExpectedYield() { return expectedYield; }
        public void setExpectedYield(double expectedYield) { this.expectedYield = expectedYield; }
        public double getActualYield() { return actualYield; }
        public void setActualYield(double actualYield) { this.actualYield = actualYield; }
        public double getLeadTimeDays() { return leadTimeDays; }
        public void setLeadTimeDays(double leadTimeDays) { this.leadTimeDays = leadTimeDays; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public WorkerStatsByStage getWorkerStats() { return workerStats; }
    public void setWorkerStats(WorkerStatsByStage workerStats) { this.workerStats = workerStats; }
    public OrderStats getOrderStats() { return orderStats; }
    public void setOrderStats(OrderStats orderStats) { this.orderStats = orderStats; }
    public List<DailyProductionRecord> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<DailyProductionRecord> dailyTrend) { this.dailyTrend = dailyTrend; }
    public List<StageEfficiency> getStageEfficiency() { return stageEfficiency; }
    public void setStageEfficiency(List<StageEfficiency> stageEfficiency) { this.stageEfficiency = stageEfficiency; }
    public List<OrderAnalytics> getOrderAnalytics() { return orderAnalytics; }
    public void setOrderAnalytics(List<OrderAnalytics> orderAnalytics) { this.orderAnalytics = orderAnalytics; }
    public double getOverallProductivity() { return overallProductivity; }
    public void setOverallProductivity(double overallProductivity) { this.overallProductivity = overallProductivity; }
    public double getTotalKgThisWeek() { return totalKgThisWeek; }
    public void setTotalKgThisWeek(double totalKgThisWeek) { this.totalKgThisWeek = totalKgThisWeek; }
    public double getTotalHoursThisWeek() { return totalHoursThisWeek; }
    public void setTotalHoursThisWeek(double totalHoursThisWeek) { this.totalHoursThisWeek = totalHoursThisWeek; }
}
