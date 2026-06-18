package org.example.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DailyBoardDTO {

    private LocalDate date;
    private StageSummary roast;
    private StageSummary qc;
    private StageSummary packaging;
    private double totalTargetKg;
    private double totalActualKg;
    private double completionRate;
    private List<OrderStageRow> orderRows;
    private int totalWorkers;
    private double totalWorkerHours;

    public static class StageSummary {
        private double targetKg;
        private double actualKg;
        private double completionRate;
        private int orderCount;
        private double workerHours;

        public double getTargetKg() { return targetKg; }
        public void setTargetKg(double targetKg) { this.targetKg = targetKg; }
        public double getActualKg() { return actualKg; }
        public void setActualKg(double actualKg) { this.actualKg = actualKg; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
        public double getWorkerHours() { return workerHours; }
        public void setWorkerHours(double workerHours) { this.workerHours = workerHours; }
    }

    public static class OrderStageRow {
        private String orderId;
        private String orderCode;
        private String title;
        private String customerName;
        private double outputTarget;
        private double completedQuantity;
        private double remainingQuantity;
        private double progressPercent;
        private double roastActual;
        private double qcActual;
        private double packagingActual;
        private double roastTarget;
        private double qcTarget;
        private double packagingTarget;
        private String riskLevel;
        private String stageStatus;
        private int daysToDeadline;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public double getOutputTarget() { return outputTarget; }
        public void setOutputTarget(double outputTarget) { this.outputTarget = outputTarget; }
        public double getCompletedQuantity() { return completedQuantity; }
        public void setCompletedQuantity(double completedQuantity) { this.completedQuantity = completedQuantity; }
        public double getRemainingQuantity() { return remainingQuantity; }
        public void setRemainingQuantity(double remainingQuantity) { this.remainingQuantity = remainingQuantity; }
        public double getProgressPercent() { return progressPercent; }
        public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
        public double getRoastActual() { return roastActual; }
        public void setRoastActual(double roastActual) { this.roastActual = roastActual; }
        public double getQcActual() { return qcActual; }
        public void setQcActual(double qcActual) { this.qcActual = qcActual; }
        public double getPackagingActual() { return packagingActual; }
        public void setPackagingActual(double packagingActual) { this.packagingActual = packagingActual; }
        public double getRoastTarget() { return roastTarget; }
        public void setRoastTarget(double roastTarget) { this.roastTarget = roastTarget; }
        public double getQcTarget() { return qcTarget; }
        public void setQcTarget(double qcTarget) { this.qcTarget = qcTarget; }
        public double getPackagingTarget() { return packagingTarget; }
        public void setPackagingTarget(double packagingTarget) { this.packagingTarget = packagingTarget; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getStageStatus() { return stageStatus; }
        public void setStageStatus(String stageStatus) { this.stageStatus = stageStatus; }
        public int getDaysToDeadline() { return daysToDeadline; }
        public void setDaysToDeadline(int daysToDeadline) { this.daysToDeadline = daysToDeadline; }
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public StageSummary getRoast() { return roast; }
    public void setRoast(StageSummary roast) { this.roast = roast; }
    public StageSummary getQc() { return qc; }
    public void setQc(StageSummary qc) { this.qc = qc; }
    public StageSummary getPackaging() { return packaging; }
    public void setPackaging(StageSummary packaging) { this.packaging = packaging; }
    public double getTotalTargetKg() { return totalTargetKg; }
    public void setTotalTargetKg(double totalTargetKg) { this.totalTargetKg = totalTargetKg; }
    public double getTotalActualKg() { return totalActualKg; }
    public void setTotalActualKg(double totalActualKg) { this.totalActualKg = totalActualKg; }
    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    public List<OrderStageRow> getOrderRows() { return orderRows; }
    public void setOrderRows(List<OrderStageRow> orderRows) { this.orderRows = orderRows; }
    public int getTotalWorkers() { return totalWorkers; }
    public void setTotalWorkers(int totalWorkers) { this.totalWorkers = totalWorkers; }
    public double getTotalWorkerHours() { return totalWorkerHours; }
    public void setTotalWorkerHours(double totalWorkerHours) { this.totalWorkerHours = totalWorkerHours; }
}
