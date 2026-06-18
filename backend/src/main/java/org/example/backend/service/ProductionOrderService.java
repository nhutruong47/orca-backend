package org.example.backend.service;

import org.example.backend.dto.ProductionOrderDTO;
import org.example.backend.entity.Attendance;
import org.example.backend.entity.ProductionBatch;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.ProductionPlan;
import org.example.backend.entity.Team;
import org.example.backend.repository.AttendanceRepository;
import org.example.backend.repository.DailyTargetRepository;
import org.example.backend.repository.ProductionBatchRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.ProductionPlanRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ProductionOrderService {

    private final ProductionOrderRepository orderRepo;
    private final TeamRepository teamRepo;
    private final ProductionPlanRepository planRepo;
    private final DailyTargetRepository dailyTargetRepo;
    private final ProductionBatchRepository batchRepo;
    private final AttendanceRepository attendanceRepo;

    public ProductionOrderService(
            ProductionOrderRepository orderRepo,
            TeamRepository teamRepo,
            ProductionPlanRepository planRepo,
            DailyTargetRepository dailyTargetRepo,
            ProductionBatchRepository batchRepo,
            AttendanceRepository attendanceRepo) {
        this.orderRepo = orderRepo;
        this.teamRepo = teamRepo;
        this.planRepo = planRepo;
        this.dailyTargetRepo = dailyTargetRepo;
        this.batchRepo = batchRepo;
        this.attendanceRepo = attendanceRepo;
    }

    @Transactional
    public ProductionOrder createOrder(UUID teamId, ProductionOrder raw) {
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        ProductionOrder order = new ProductionOrder();
        order.setTeam(team);
        applyEditableFields(order, raw);
        order.setStatus("PENDING");

        validateOrder(order);
        order.calculateInputRequired();

        return orderRepo.save(order);
    }

    @Transactional
    public ProductionOrder updateOrder(UUID orderId, ProductionOrder raw) {
        ProductionOrder order = getById(orderId);
        applyEditableFields(order, raw);
        if (raw.getStatus() != null && !raw.getStatus().isBlank()) {
            order.setStatus(raw.getStatus());
        }

        validateOrder(order);
        order.calculateInputRequired();

        return orderRepo.save(order);
    }

    @Transactional
    public void deleteOrder(UUID orderId) {
        ProductionOrder order = getById(orderId);

        List<Attendance> attendances = attendanceRepo.findByOrderId(orderId);
        attendances.forEach(attendance -> attendance.setProductionOrder(null));
        attendanceRepo.saveAll(attendances);

        List<ProductionBatch> batches = batchRepo.findByOrderIdOrderByCreatedAtDesc(orderId);
        batches.forEach(batch -> batch.setOrder(null));
        batchRepo.saveAll(batches);

        dailyTargetRepo.deleteAll(dailyTargetRepo.findByOrderIdOrderByTargetDateAsc(orderId));
        List<ProductionPlan> plans = planRepo.findByOrderIdOrderByCreatedAtDesc(orderId);
        planRepo.deleteAll(plans);

        orderRepo.delete(order);
    }

    private void applyEditableFields(ProductionOrder order, ProductionOrder raw) {
        order.setTitle(raw.getTitle());
        order.setDescription(raw.getDescription());
        order.setCustomerName(raw.getCustomerName());
        order.setProductType(raw.getProductType());
        order.setProcessType(raw.getProcessType());
        order.setRoastLevel(raw.getRoastLevel());
        order.setPackageSize(raw.getPackageSize());
        order.setTotalPackages(raw.getTotalPackages());
        order.setOutputTarget(raw.getOutputTarget());
        order.setExpectedYield(raw.getExpectedYield());
        order.setExpectedLoss(raw.getExpectedLoss());
        order.setUnit(raw.getUnit());
        order.setOrderDate(raw.getOrderDate() != null ? raw.getOrderDate() : LocalDate.now());
        order.setConfirmDate(raw.getConfirmDate());
        order.setProductionStartDate(raw.getProductionStartDate());
        order.setCustomerDeliveryDate(raw.getCustomerDeliveryDate());
        order.setSafetyBufferDays(raw.getSafetyBufferDays() != null ? raw.getSafetyBufferDays() : 2);
        order.setRecipientName(raw.getRecipientName());
        order.setRecipientPhone(raw.getRecipientPhone());
        order.setShippingNote(raw.getShippingNote());
    }

    private void validateOrder(ProductionOrder order) {
        if (order.getTitle() == null || order.getTitle().isBlank()) {
            throw new RuntimeException("Tieu de don hang khong duoc de trong");
        }
        if (order.getOutputTarget() == null || order.getOutputTarget() <= 0) {
            throw new RuntimeException("San luong muc tieu phai lon hon 0");
        }
        if (order.getCustomerDeliveryDate() != null && order.getProductionStartDate() != null
                && !order.getCustomerDeliveryDate().isAfter(order.getProductionStartDate())) {
            throw new RuntimeException("Ngay giao hang phai sau ngay bat dau san xuat");
        }
    }

    public List<ProductionOrder> getOrdersByTeam(UUID teamId) {
        return orderRepo.findByTeamIdOrderByCreatedAtDesc(teamId);
    }

    public List<ProductionOrder> getActiveOrders(UUID teamId) {
        return orderRepo.findByTeamIdAndStatusInOrderByDeadline(teamId,
                List.of("CONFIRMED", "PLANNING", "IN_PRODUCTION"));
    }

    public ProductionOrder getById(UUID orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang"));
    }

    public ProductionOrder updateStatus(UUID orderId, String status) {
        ProductionOrder order = getById(orderId);
        order.setStatus(status);
        return orderRepo.save(order);
    }

    public ProductionOrder updateCompletedQuantity(UUID orderId, Double quantity) {
        ProductionOrder order = getById(orderId);
        order.setCompletedQuantity(quantity);
        return orderRepo.save(order);
    }

    public ProductionOrderDTO toDTO(ProductionOrder o) {
        ProductionOrderDTO dto = new ProductionOrderDTO();
        dto.setId(o.getId().toString());
        dto.setTeamId(o.getTeam().getId().toString());
        dto.setOrderCode(o.getOrderCode());
        dto.setTitle(o.getTitle());
        dto.setDescription(o.getDescription());
        dto.setCustomerName(o.getCustomerName());
        dto.setProductType(o.getProductType());
        dto.setProcessType(o.getProcessType());
        dto.setRoastLevel(o.getRoastLevel());
        dto.setPackageSize(o.getPackageSize());
        dto.setTotalPackages(o.getTotalPackages());
        dto.setOutputTarget(o.getOutputTarget());
        dto.setExpectedYield(o.getExpectedYield());
        dto.setExpectedLoss(o.getExpectedLoss());
        dto.setInputRequired(o.getInputRequired());
        dto.setUnit(o.getUnit());
        dto.setOrderDate(o.getOrderDate());
        dto.setConfirmDate(o.getConfirmDate());
        dto.setProductionStartDate(o.getProductionStartDate());
        dto.setInternalDeadline(o.getInternalDeadline());
        dto.setCustomerDeliveryDate(o.getCustomerDeliveryDate());
        dto.setSafetyBufferDays(o.getSafetyBufferDays());
        dto.setRecipientName(o.getRecipientName());
        dto.setRecipientPhone(o.getRecipientPhone());
        dto.setShippingNote(o.getShippingNote());
        dto.setStatus(o.getStatus());
        dto.setCompletedQuantity(o.getCompletedQuantity());
        dto.setProgressPercent(o.getProgressPercent());
        dto.setRemainingQuantity(o.getRemainingQuantity());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());
        return dto;
    }
}
