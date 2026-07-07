package org.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CostDashboardStats {
    private BigDecimal totalToday;
    private BigDecimal totalMonth;
    private BigDecimal totalYear;
    private BigDecimal totalSystem;

    private BigDecimal monthOverMonthChange;
    private BigDecimal yearOverYearChange;

    private List<Map<String, Object>> dailyChart;
    private List<Map<String, Object>> monthlyChart;
    private List<Map<String, Object>> categoryChart;

    public CostDashboardStats() {}

    public BigDecimal getTotalToday() {
        return totalToday;
    }

    public void setTotalToday(BigDecimal totalToday) {
        this.totalToday = totalToday;
    }

    public BigDecimal getTotalMonth() {
        return totalMonth;
    }

    public void setTotalMonth(BigDecimal totalMonth) {
        this.totalMonth = totalMonth;
    }

    public BigDecimal getTotalYear() {
        return totalYear;
    }

    public void setTotalYear(BigDecimal totalYear) {
        this.totalYear = totalYear;
    }

    public BigDecimal getTotalSystem() {
        return totalSystem;
    }

    public void setTotalSystem(BigDecimal totalSystem) {
        this.totalSystem = totalSystem;
    }

    public BigDecimal getMonthOverMonthChange() {
        return monthOverMonthChange;
    }

    public void setMonthOverMonthChange(BigDecimal monthOverMonthChange) {
        this.monthOverMonthChange = monthOverMonthChange;
    }

    public BigDecimal getYearOverYearChange() {
        return yearOverYearChange;
    }

    public void setYearOverYearChange(BigDecimal yearOverYearChange) {
        this.yearOverYearChange = yearOverYearChange;
    }

    public List<Map<String, Object>> getDailyChart() {
        return dailyChart;
    }

    public void setDailyChart(List<Map<String, Object>> dailyChart) {
        this.dailyChart = dailyChart;
    }

    public List<Map<String, Object>> getMonthlyChart() {
        return monthlyChart;
    }

    public void setMonthlyChart(List<Map<String, Object>> monthlyChart) {
        this.monthlyChart = monthlyChart;
    }

    public List<Map<String, Object>> getCategoryChart() {
        return categoryChart;
    }

    public void setCategoryChart(List<Map<String, Object>> categoryChart) {
        this.categoryChart = categoryChart;
    }
}
