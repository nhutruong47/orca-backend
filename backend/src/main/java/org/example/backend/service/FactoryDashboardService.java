package org.example.backend.service;

import org.example.backend.dto.AttendanceDTO;
import org.example.backend.dto.DailyTargetDTO;
import org.example.backend.dto.ProductionOrderDTO;
import org.example.backend.entity.Attendance;
import org.example.backend.entity.DailyTarget;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FactoryDashboardService {

    private final ProductionOrderRepository orderRepo;
    private final AttendanceRepository attendanceRepo;
    private final DailyTargetRepository targetRepo;
    private final ProductionPlanRepository planRepo;
    private final ProductionOrderService orderService;

    public FactoryDashboardService(ProductionOrderRepository orderRepo,
                                   AttendanceRepository attendanceRepo,
                                   DailyTargetRepository targetRepo,
                                   ProductionPlanRepository planRepo,
                                   ProductionOrderService orderService) {
        this.orderRepo = orderRepo;
        this.attendanceRepo = attendanceRepo;
        this.targetRepo = targetRepo;
        this.planRepo = planRepo;
        this.orderService = orderService;
    }

    public Map<String, Object> getDashboard(UUID teamId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<ProductionOrder> activeOrders = orderRepo.findByTeamIdAndStatusInOrderByDeadline(teamId,
                List.of("CONFIRMED", "PLANNING", "IN_PRODUCTION"));
        List<ProductionOrder> overdueOrders = orderRepo.findOverdueOrders(teamId, now);

        List<Map<String, Object>> activeOrdersData = activeOrders.stream()
                .limit(10)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId().toString());
                    m.put("orderCode", o.getOrderCode());
                    m.put("title", o.getTitle());
                    m.put("customerName", o.getCustomerName());
                    m.put("outputTarget", o.getOutputTarget());
                    m.put("completedQuantity", o.getCompletedQuantity());
                    m.put("remainingQuantity", o.getRemainingQuantity());
                    m.put("progressPercent", o.getProgressPercent());
                    m.put("internalDeadline", o.getInternalDeadline());
                    m.put("customerDeliveryDate", o.getCustomerDeliveryDate());
                    m.put("status", o.getStatus());
                    m.put("productType", o.getProductType());
                    m.put("isAtRisk", isAtRisk(o));
                    return m;
                })
                .collect(Collectors.toList());

        List<Attendance> todayAttendances = attendanceRepo.findByTeamIdAndDate(teamId, today);
        List<Map<String, Object>> staffData = todayAttendances.stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", a.getUser().getId().toString());
                    m.put("userName", a.getUser().getFullName());
                    m.put("shiftType", a.getShiftType() != null ? a.getShiftType().name() : null);
                    m.put("stage", a.getStage() != null ? a.getStage().name() : null);
                    m.put("checkInTime", a.getCheckInTime());
                    m.put("checkOutTime", a.getCheckOutTime());
                    m.put("workHours", a.getActualWorkHours() != null ? a.getActualWorkHours()
                            : (a.getRegularHours() + a.getOvertimeHours()));
                    m.put("attendanceStatus", a.getAttendanceStatus() != null ? a.getAttendanceStatus().name() : null);
                    if (a.getProductionOrder() != null) {
                        m.put("orderTitle", a.getProductionOrder().getTitle());
                    }
                    return m;
                })
                .collect(Collectors.toList());

        double totalWorkerHours = todayAttendances.stream()
                .filter(a -> a.getCheckOutTime() != null)
                .mapToDouble(a -> a.getRegularHours() + a.getOvertimeHours())
                .sum();

        Optional<DailyTarget> todayTarget = targetRepo.findByTeamIdAndDate(teamId, today);

        List<Map<String, Object>> alerts = new ArrayList<>();

        for (ProductionOrder o : overdueOrders) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("type", "ORDER_AT_RISK");
            alert.put("level", "warning");
            alert.put("message", "Don \"" + o.getTitle() + "\" sap het han noi bo!");
            alert.put("relatedId", o.getId().toString());
            alerts.add(alert);
        }

        if (todayTarget.isPresent()) {
            DailyTarget t = todayTarget.get();
            if (t.getCompletionRate() != null && t.getCompletionRate() < 80) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("type", "TARGET_LOW");
                alert.put("level", "warning");
                alert.put("message", "Muc tieu hom nay moi dat " + t.getCompletionRate().intValue() + "%");
                alert.put("relatedId", t.getId().toString());
                alerts.add(alert);
            }
        }

        long notCheckedOut = attendanceRepo.countNotCheckedOut(teamId, today);
        if (notCheckedOut > 0) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("type", "MISSING_CHECKOUT");
            alert.put("level", "info");
            alert.put("message", notCheckedOut + " nhan vien chua check-out");
            alerts.add(alert);
        }

        if (todayAttendances.size() < 3) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("type", "STAFF_LOW");
            alert.put("level", "info");
            alert.put("message", "So nhan vien hom nay thap hon binh thuong");
            alerts.add(alert);
        }

        dashboard.put("activeOrdersCount", activeOrders.size());
        dashboard.put("activeOrders", activeOrdersData);
        dashboard.put("todayTarget", todayTarget.map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId().toString());
            m.put("targetQuantityKg", t.getTargetQuantityKg());
            m.put("targetRoastKg", t.getTargetRoastKg());
            m.put("targetQcKg", t.getTargetQcKg());
            m.put("targetPackagedKg", t.getTargetPackagedKg());
            m.put("actualRoastKg", t.getActualRoastKg());
            m.put("actualQcKg", t.getActualQcKg());
            m.put("actualPackagedKg", t.getActualPackagedKg());
            m.put("totalActualKg", t.getTotalActualKg());
            m.put("completionRate", t.getCompletionRate());
            m.put("status", t.getStatus().name());
            m.put("totalWorkerHours", t.getTotalWorkerHours());
            m.put("productivityKgPerHour", t.getProductivityKgPerHour());
            return m;
        }).orElse(null));

        dashboard.put("staffToday", todayAttendances.size());
        dashboard.put("staffDetails", staffData);
        dashboard.put("totalWorkerHoursToday", totalWorkerHours);
        dashboard.put("alerts", alerts);

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("completedOrders", orderRepo.countByTeamIdAndStatus(teamId, "COMPLETED"));
        stats.put("inProductionOrders", orderRepo.countByTeamIdAndStatus(teamId, "IN_PRODUCTION"));
        stats.put("pendingOrders", orderRepo.countByTeamIdAndStatus(teamId, "PENDING"));
        stats.put("totalOrders", (long) orderRepo.findByTeamIdOrderByCreatedAtDesc(teamId).size());
        dashboard.put("stats", stats);

        dashboard.put("upcomingDeadlines", activeOrders.stream()
                .filter(o -> o.getInternalDeadline() != null)
                .sorted(Comparator.comparing(ProductionOrder::getInternalDeadline))
                .limit(5)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId().toString());
                    m.put("title", o.getTitle());
                    m.put("internalDeadline", o.getInternalDeadline());
                    m.put("daysRemaining", ChronoUnit.DAYS.between(now, o.getInternalDeadline()));
                    return m;
                })
                .collect(Collectors.toList()));

        return dashboard;
    }

    public Map<String, Object> getProductivitySummary(UUID teamId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        double totalKg = 0;
        double totalHours = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Optional<DailyTarget> targetOpt = targetRepo.findByTeamIdAndDate(teamId, current);
            if (targetOpt.isPresent()) {
                DailyTarget t = targetOpt.get();
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("date", current.toString());
                record.put("targetKg", t.getTargetQuantityKg());
                record.put("actualKg", t.getTotalActualKg());
                record.put("completionRate", t.getCompletionRate());
                record.put("workerHours", t.getTotalWorkerHours());
                record.put("productivity", t.getProductivityKgPerHour());
                records.add(record);

                if (t.getTotalActualKg() != null) totalKg += t.getTotalActualKg();
                if (t.getTotalWorkerHours() != null) totalHours += t.getTotalWorkerHours();
            }
            current = current.plusDays(1);
        }

        summary.put("records", records);
        summary.put("totalKg", totalKg);
        summary.put("totalWorkerHours", totalHours);
        summary.put("averageProductivity", totalHours > 0 ? Math.round((totalKg / totalHours) * 100.0) / 100.0 : 0);
        summary.put("workingDays", records.size());

        return summary;
    }

    private boolean isAtRisk(ProductionOrder order) {
        if (order.getInternalDeadline() == null) return false;
        if ("COMPLETED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) return false;
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), order.getInternalDeadline());
        return daysLeft <= 3;
    }
}
