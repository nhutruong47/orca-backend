package org.example.backend.service;

import org.example.backend.dto.ReplanDTO;
import org.example.backend.dto.ReplanDTO.ReplanDay;
import org.example.backend.entity.*;
import org.example.backend.entity.DailyTarget.TargetStatus;
import org.example.backend.entity.ProductionPlan.PlanStatus;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AiReplanService {

    private final ProductionPlanRepository planRepo;
    private final ProductionOrderRepository orderRepo;
    private final DailyTargetRepository targetRepo;

    public AiReplanService(ProductionPlanRepository planRepo,
                           ProductionOrderRepository orderRepo,
                           DailyTargetRepository targetRepo) {
        this.planRepo = planRepo;
        this.orderRepo = orderRepo;
        this.targetRepo = targetRepo;
    }

    public ReplanDTO analyzeReplan(UUID orderId) {
        ProductionOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));

        ProductionPlan plan = planRepo.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException("Chua co ke hoach san xuat"));

        List<DailyTarget> allTargets = targetRepo.findByOrderIdOrderByTargetDateAsc(orderId);
        LocalDate today = LocalDate.now();

        double completedKg = allTargets.stream()
                .filter(t -> t.getTotalActualKg() != null)
                .mapToDouble(DailyTarget::getTotalActualKg)
                .sum();

        double outputTarget = order.getOutputTarget() != null ? order.getOutputTarget() : 0;
        double remainingKg = Math.max(0, outputTarget - completedKg);

        List<DailyTarget> pastTargets = allTargets.stream()
                .filter(t -> !t.getTargetDate().isAfter(today))
                .filter(t -> t.getIsHoliday() == null || !t.getIsHoliday())
                .toList();

        List<DailyTarget> futureTargets = allTargets.stream()
                .filter(t -> t.getTargetDate().isAfter(today))
                .filter(t -> t.getIsHoliday() == null || !t.getIsHoliday())
                .toList();

        int originalWorkingDays = (int) allTargets.stream()
                .filter(t -> t.getIsHoliday() == null || !t.getIsHoliday())
                .count();
        int remainingWorkingDays = futureTargets.size();

        double avgDailyActual = pastTargets.isEmpty() ? plan.getDailyTargetKg()
                : pastTargets.stream().filter(t -> t.getTotalActualKg() != null)
                        .mapToDouble(DailyTarget::getTotalActualKg)
                        .average().orElse(plan.getDailyTargetKg());

        boolean underperforming = avgDailyActual < plan.getDailyTargetKg() * 0.8;

        ReplanDTO dto = new ReplanDTO();
        dto.setPlanId(plan.getId());
        dto.setOrderId(orderId);
        dto.setOrderCode(order.getOrderCode());
        dto.setOriginalTargetKg(plan.getDailyTargetKg());
        dto.setCompletedKg(Math.round(completedKg * 100.0) / 100.0);
        dto.setRemainingKg(Math.round(remainingKg * 100.0) / 100.0);
        dto.setShortFall(Math.round((plan.getDailyTargetKg() - avgDailyActual) * 100.0) / 100.0);
        dto.setOriginalWorkingDays(originalWorkingDays);
        dto.setRemainingWorkingDays(remainingWorkingDays);

        int newDaysNeeded = remainingWorkingDays > 0
                ? (int) Math.ceil(remainingKg / avgDailyActual)
                : remainingWorkingDays;
        dto.setNewWorkingDaysNeeded(Math.max(0, newDaysNeeded - remainingWorkingDays));

        List<ReplanDay> revisedDays = buildRevisedDays(futureTargets, completedKg, remainingKg, avgDailyActual, plan.getDailyTargetKg());
        dto.setRevisedDays(revisedDays);

        List<String> recommendations = buildRecommendations(avgDailyActual, plan.getDailyTargetKg(), remainingKg,
                remainingWorkingDays, newDaysNeeded, order, underperforming);
        dto.setRecommendations(recommendations);

        String riskLevel = calcRiskLevel(order, remainingKg, remainingWorkingDays, avgDailyActual);
        dto.setRiskLevel(riskLevel);

        boolean needsReplan = underperforming
                || remainingKg > 0 && remainingWorkingDays == 0
                || (remainingWorkingDays > 0 && newDaysNeeded > remainingWorkingDays);
        dto.setNeedsReplan(needsReplan);

        if (needsReplan) {
            dto.setReplanStrategy(underperforming ? "TANG_NGAY_CONG"
                    : remainingWorkingDays == 0 ? "KE_DAIL"
                    : "CAN_CHINH_MUC_TIEU");
        } else {
            dto.setReplanStrategy("KHONG_CAN");
        }

        return dto;
    }

    @Transactional
    public void applyReplan(UUID orderId, List<Map<String, Object>> revisedTargets) {
        ProductionOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));

        ProductionPlan plan = planRepo.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException("Chua co ke hoach"));

        for (Map<String, Object> revised : revisedTargets) {
            String targetIdStr = (String) revised.get("targetId");
            if (targetIdStr == null) continue;
            UUID targetId = UUID.fromString(targetIdStr);
            Optional<DailyTarget> tOpt = targetRepo.findById(targetId);
            if (tOpt.isEmpty()) continue;
            DailyTarget t = tOpt.get();

            if (revised.get("targetQuantityKg") != null) {
                t.setTargetQuantityKg(((Number) revised.get("targetQuantityKg")).doubleValue());
            }
            if (revised.get("targetRoastKg") != null) {
                t.setTargetRoastKg(((Number) revised.get("targetRoastKg")).doubleValue());
            }
            if (revised.get("targetQcKg") != null) {
                t.setTargetQcKg(((Number) revised.get("targetQcKg")).doubleValue());
            }
            if (revised.get("targetPackagedKg") != null) {
                t.setTargetPackagedKg(((Number) revised.get("targetPackagedKg")).doubleValue());
            }
            targetRepo.save(t);
        }

        plan.setDailyTargetKg(plan.getDailyTargetKg());
        plan.setRiskFactors("Da duoc AI replan luc " + LocalDate.now());
        planRepo.save(plan);

        order.setStatus("IN_PRODUCTION");
        orderRepo.save(order);
    }

    private List<ReplanDay> buildRevisedDays(List<DailyTarget> futureTargets,
                                              double completedKg, double remainingKg,
                                              double avgDailyActual, double originalTarget) {
        List<ReplanDay> days = new ArrayList<>();
        double cumulativeActual = completedKg;

        for (int i = 0; i < futureTargets.size(); i++) {
            DailyTarget t = futureTargets.get(i);
            ReplanDay r = new ReplanDay();
            r.setDayIndex(i + 1);
            r.setDate(t.getTargetDate().toString());
            r.setOriginalTargetKg(t.getTargetQuantityKg() != null ? t.getTargetQuantityKg() : 0);
            r.setActualKg(t.getTotalActualKg() != null ? t.getTotalActualKg() : 0);
            r.setCumulativeActual(Math.round(cumulativeActual * 100.0) / 100.0);
            r.setCumulativeTarget(Math.round(r.getOriginalTargetKg() * (i + 1) * 100.0) / 100.0);

            double revised = Math.min(avgDailyActual * 1.2, remainingKg / Math.max(1, futureTargets.size()));
            r.setRevisedTargetKg(Math.round(revised * 100.0) / 100.0);

            if (t.getStatus() == TargetStatus.COMPLETED) {
                r.setStatus("DA_HOAN_THANH");
            } else if (t.getTotalActualKg() != null && t.getTotalActualKg() > 0) {
                r.setStatus("DANG_THUC_HIEN");
            } else {
                r.setStatus("CHUA_THUC_HIEN");
            }

            cumulativeActual += r.getActualKg();
            days.add(r);
        }
        return days;
    }

    private List<String> buildRecommendations(double avgActual, double originalTarget, double remainingKg,
                                               int remainingDays, int newDaysNeeded,
                                               ProductionOrder order, boolean underperforming) {
        List<String> recs = new ArrayList<>();

        if (underperforming) {
            recs.add("San luong thuc te (" + Math.round(avgActual * 100.0) / 100.0 + " kg/ngay) thap hon ke hoach (" + Math.round(originalTarget * 100.0) / 100.0 + " kg/ngay).");
        }

        if (remainingDays == 0 && remainingKg > 0) {
            recs.add("Da het han lam viec nhung con " + Math.round(remainingKg * 100.0) / 100.0 + " kg chua hoan thanh. Can gia han them " + newDaysNeeded + " ngay.");
        }

        if (remainingDays > 0 && newDaysNeeded > remainingDays) {
            int extraDays = newDaysNeeded - remainingDays;
            recs.add("Can them " + extraDays + " ngay lam viec de hoan thanh muc tieu. De xuat tang ca hoac thue them nhan su.");
        }

        if (avgActual > 0 && remainingKg / avgActual <= 2) {
            recs.add("Kha nang hoan thanh dung han: cao. Tien do hien tai on dinh.");
        }

        if (order.getInternalDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), order.getInternalDeadline().toLocalDate());
            if (daysLeft <= 2) {
                recs.add("Can CHAP CANH: han noi bo chi con " + daysLeft + " ngay. Kiem tra kha nang giao hang ngay.");
            }
        }

        double shortfallPercent = originalTarget > 0 ? (originalTarget - avgActual) / originalTarget * 100 : 0;
        if (shortfallPercent > 20) {
            recs.add("Thieu " + Math.round(shortfallPercent) + "% so voi ke hoach. Xem xet tang ca 2 gio/ngay de bup gap.");
        }

        return recs;
    }

    private String calcRiskLevel(ProductionOrder order, double remainingKg, int remainingDays, double avgDailyActual) {
        if (remainingKg <= 0) return "NONE";
        if (order.getInternalDeadline() == null) return avgDailyActual < 50 ? "HIGH" : "MEDIUM";

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), order.getInternalDeadline().toLocalDate());
        if (daysLeft <= 0) return "CRITICAL";
        if (daysLeft <= 2) return "HIGH";
        if (daysLeft <= 5) return "MEDIUM";

        double projectedCompletion = remainingDays * avgDailyActual;
        if (projectedCompletion < remainingKg) return "HIGH";
        return "LOW";
    }
}
