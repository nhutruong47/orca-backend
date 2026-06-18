package org.example.backend.service;

import org.example.backend.dto.AnalyticsDTO;
import org.example.backend.dto.AnalyticsDTO.*;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final DailyTargetRepository targetRepo;
    private final ProductionOrderRepository orderRepo;
    private final AttendanceRepository attendanceRepo;

    public AnalyticsService(DailyTargetRepository targetRepo,
                           ProductionOrderRepository orderRepo,
                           AttendanceRepository attendanceRepo) {
        this.targetRepo = targetRepo;
        this.orderRepo = orderRepo;
        this.attendanceRepo = attendanceRepo;
    }

    public AnalyticsDTO getAnalytics(UUID teamId, LocalDate startDate, LocalDate endDate) {
        AnalyticsDTO analytics = new AnalyticsDTO();
        WorkerStatsByStage ws = buildWorkerStats(teamId, startDate, endDate);
        analytics.setWorkerStats(ws);
        analytics.setOrderStats(buildOrderStats(teamId));
        analytics.setDailyTrend(buildDailyTrend(teamId, startDate, endDate));
        analytics.setStageEfficiency(buildStageEfficiency(teamId, startDate, endDate));
        analytics.setOrderAnalytics(buildOrderAnalytics(teamId));
        analytics.setOverallProductivity(ws.getTotalHours() > 0
                ? Math.round(ws.getRoastKg() + ws.getQcKg() + ws.getPackagingKg() / ws.getTotalHours() * 100.0) / 100.0 : 0);
        return analytics;
    }

    private WorkerStatsByStage buildWorkerStats(UUID teamId, LocalDate start, LocalDate end) {
        WorkerStatsByStage stats = new WorkerStatsByStage();
        List<Attendance> attendances = attendanceRepo.findByTeamIdAndDateBetween(teamId, start, end);

        double totalHours = attendances.stream()
                .filter(a -> a.getCheckOutTime() != null)
                .mapToDouble(a -> a.getRegularHours() + a.getOvertimeHours())
                .sum();
        stats.setTotalHours(Math.round(totalHours * 10.0) / 10.0);

        Map<String, Double> stageHours = new LinkedHashMap<>();
        Map<String, Double> stageKg = new LinkedHashMap<>();
        for (Attendance a : attendances) {
            if (a.getStage() != null && a.getCheckOutTime() != null) {
                String stage = stageKey(a.getStage());
                double hours = a.getRegularHours() + a.getOvertimeHours();
                stageHours.merge(stage, hours, Double::sum);
            }
        }
        stats.setRoastHours(round(stageHours.getOrDefault("RANG", 0.0)));
        stats.setQcHours(round(stageHours.getOrDefault("QA", 0.0)));
        stats.setPackagingHours(round(stageHours.getOrDefault("DONG_GOI", 0.0)));

        List<DailyTarget> targets = getTargetsInRange(teamId, start, end);
        double roastKg = targets.stream().filter(t -> t.getActualRoastKg() != null).mapToDouble(DailyTarget::getActualRoastKg).sum();
        double qcKg = targets.stream().filter(t -> t.getActualQcKg() != null).mapToDouble(DailyTarget::getActualQcKg).sum();
        double packagingKg = targets.stream().filter(t -> t.getActualPackagedKg() != null).mapToDouble(DailyTarget::getActualPackagedKg).sum();
        stats.setRoastKg(Math.round(roastKg * 100.0) / 100.0);
        stats.setQcKg(Math.round(qcKg * 100.0) / 100.0);
        stats.setPackagingKg(Math.round(packagingKg * 100.0) / 100.0);

        double totalKg = roastKg + qcKg + packagingKg;
        stats.setRoastProductivity(stats.getRoastHours() > 0 ? Math.round(roastKg / stats.getRoastHours() * 100.0) / 100.0 : 0);
        stats.setQcProductivity(stats.getQcHours() > 0 ? Math.round(qcKg / stats.getQcHours() * 100.0) / 100.0 : 0);
        stats.setPackagingProductivity(stats.getPackagingHours() > 0 ? Math.round(packagingKg / stats.getPackagingHours() * 100.0) / 100.0 : 0);

        return stats;
    }

    private OrderStats buildOrderStats(UUID teamId) {
        OrderStats stats = new OrderStats();
        List<ProductionOrder> orders = orderRepo.findByTeamIdOrderByCreatedAtDesc(teamId);
        stats.setTotal(orders.size());
        stats.setCompleted(orders.stream().filter(o -> "COMPLETED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())).count());
        stats.setInProduction(orders.stream().filter(o -> "IN_PRODUCTION".equals(o.getStatus())).count());
        stats.setPending(orders.stream().filter(o -> "PENDING".equals(o.getStatus()) || "CONFIRMED".equals(o.getStatus()) || "PLANNING".equals(o.getStatus())).count());

        long atRisk = orders.stream().filter(o -> {
            if (o.getInternalDeadline() == null) return false;
            if ("COMPLETED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) return false;
            return ChronoUnit.DAYS.between(LocalDate.now(), o.getInternalDeadline().toLocalDate()) <= 3;
        }).count();
        stats.setAtRisk(atRisk);

        double avgYield = orders.stream()
                .filter(o -> o.getExpectedYield() != null && o.getExpectedYield() > 0)
                .mapToDouble(ProductionOrder::getExpectedYield)
                .average()
                .orElse(0);
        stats.setAvgYield(Math.round(avgYield * 10000.0) / 10000.0);

        return stats;
    }

    private List<DailyProductionRecord> buildDailyTrend(UUID teamId, LocalDate start, LocalDate end) {
        List<DailyProductionRecord> records = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DailyProductionRecord r = new DailyProductionRecord();
            r.setDate(current);
            Optional<DailyTarget> tOpt = targetRepo.findByTeamIdAndDate(teamId, current);
            if (tOpt.isPresent()) {
                DailyTarget t = tOpt.get();
                r.setTargetKg(t.getTargetQuantityKg() != null ? t.getTargetQuantityKg() : 0);
                r.setActualKg(t.getTotalActualKg() != null ? t.getTotalActualKg() : 0);
                r.setCompletionRate(t.getCompletionRate() != null ? t.getCompletionRate() : 0);
                r.setRoastKg(t.getActualRoastKg() != null ? t.getActualRoastKg() : 0);
                r.setQcKg(t.getActualQcKg() != null ? t.getActualQcKg() : 0);
                r.setPackagingKg(t.getActualPackagedKg() != null ? t.getActualPackagedKg() : 0);
                r.setWorkerHours(t.getTotalWorkerHours() != null ? t.getTotalWorkerHours() : 0);
            }
            records.add(r);
            current = current.plusDays(1);
        }
        return records;
    }

    private List<StageEfficiency> buildStageEfficiency(UUID teamId, LocalDate start, LocalDate end) {
        List<StageEfficiency> list = new ArrayList<>();
        List<DailyTarget> targets = getTargetsInRange(teamId, start, end);

        String[] stages = {"RANG", "QC", "DONG_GOI"};
        String[] stageLabels = {"Rang", "QC", "Dong goi"};

        for (int i = 0; i < stages.length; i++) {
            StageEfficiency se = new StageEfficiency();
            se.setStage(stageLabels[i]);

            double targetKg = 0, actualKg = 0, failKg = 0, workerHours = 0;
            int daysWithData = 0;
            List<Double> productivities = new ArrayList<>();

            for (DailyTarget t : targets) {
                if (t.getIsHoliday() != null && t.getIsHoliday()) continue;
                double tKg = 0, aKg = 0;
                if ("RANG".equals(stages[i])) { tKg = n(t.getTargetRoastKg()); aKg = n(t.getActualRoastKg()); }
                else if ("QC".equals(stages[i])) { tKg = n(t.getTargetQcKg()); aKg = n(t.getActualQcKg()); failKg += n(t.getActualQcFailKg()); }
                else if ("DONG_GOI".equals(stages[i])) { tKg = n(t.getTargetPackagedKg()); aKg = n(t.getActualPackagedKg()); }

                targetKg += tKg;
                actualKg += aKg;
                workerHours += n(t.getTotalWorkerHours());
                if (tKg > 0 || aKg > 0) daysWithData++;
                if (t.getProductivityKgPerHour() != null) productivities.add(t.getProductivityKgPerHour());
            }

            se.setTotalTargetKg(Math.round(targetKg * 100.0) / 100.0);
            se.setTotalActualKg(Math.round(actualKg * 100.0) / 100.0);
            se.setEfficiency(targetKg > 0 ? Math.round(actualKg / targetKg * 10000.0) / 100.0 : 0);
            se.setAvgProductivity(productivities.isEmpty() ? 0 : Math.round(productivities.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0);
            se.setFailRate(actualKg > 0 ? Math.round(failKg / actualKg * 10000.0) / 100.0 : 0);
            list.add(se);
        }

        return list;
    }

    private List<OrderAnalytics> buildOrderAnalytics(UUID teamId) {
        List<ProductionOrder> orders = orderRepo.findByTeamIdAndStatusInOrderByDeadline(teamId,
                List.of("CONFIRMED", "PLANNING", "IN_PRODUCTION", "COMPLETED"));

        return orders.stream().map(o -> {
            OrderAnalytics a = new OrderAnalytics();
            a.setOrderId(o.getId().toString());
            a.setOrderCode(o.getOrderCode());
            a.setTitle(o.getTitle());
            a.setOutputTarget(o.getOutputTarget() != null ? o.getOutputTarget() : 0);
            a.setCompletedQuantity(o.getCompletedQuantity() != null ? o.getCompletedQuantity() : 0);
            a.setProgressPercent(o.getProgressPercent());
            a.setExpectedYield(o.getExpectedYield() != null ? o.getExpectedYield() : 0);
            a.setActualYield(a.getOutputTarget() > 0 && a.getCompletedQuantity() > 0
                    ? Math.round(a.getCompletedQuantity() / a.getOutputTarget() * 10000.0) / 10000.0 : 0);
            a.setStatus(o.getStatus());
            a.setRiskLevel(calcRiskLevel(o));

            if (o.getProductionStartDate() != null && o.getCustomerDeliveryDate() != null) {
                a.setLeadTimeDays((int) ChronoUnit.DAYS.between(o.getProductionStartDate(), o.getCustomerDeliveryDate()));
            }

            return a;
        }).collect(Collectors.toList());
    }

    private List<DailyTarget> getTargetsInRange(UUID teamId, LocalDate start, LocalDate end) {
        List<ProductionOrder> orders = orderRepo.findByTeamIdOrderByCreatedAtDesc(teamId);
        List<DailyTarget> all = new ArrayList<>();
        for (ProductionOrder o : orders) {
            all.addAll(targetRepo.findByOrderIdAndDateRange(o.getId(), start, end));
        }
        return all;
    }

    private String stageKey(Attendance.ProductionStage stage) {
        if (stage == null) return "OTHER";
        return switch (stage) {
            case RANH_VA_CHON, RANG -> "RANG";
            case QA, XAY -> "QC";
            case DONG_GOI -> "DONG_GOI";
        };
    }

    private String calcRiskLevel(ProductionOrder o) {
        if (o.getInternalDeadline() == null) return "NONE";
        if ("COMPLETED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) return "NONE";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), o.getInternalDeadline().toLocalDate());
        if (days <= 0) return "CRITICAL";
        if (days <= 2) return "HIGH";
        if (days <= 5) return "MEDIUM";
        return "LOW";
    }

    private double n(Double v) { return v != null ? v : 0; }
    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
