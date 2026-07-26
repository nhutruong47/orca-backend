package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.CreateDisputeRequest;
import org.example.backend.dto.OrderDisputeDTO;
import org.example.backend.dto.RespondDisputeRequest;
import org.example.backend.dto.ResolveDisputeRequest;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.OrderDispute;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.OrderStatus;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.OrderDisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final OrderDisputeRepository disputeRepo;
    private final InterGroupOrderRepository orderRepo;
    private final OrderStateMachine stateMachine;
    private final NotificationService notificationService;

    @Transactional
    public OrderDisputeDTO openDispute(CreateDisputeRequest request, User currentUser) {
        InterGroupOrder order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().equals(OrderStatus.DELIVERED.name()) && 
            !order.getStatus().equals(OrderStatus.COMPLETED.name()) &&
            !order.getStatus().equals(OrderStatus.REVIEWED.name())) {
            throw new RuntimeException("Chỉ có thể khiếu nại đơn hàng đã giao, đã hoàn thành hoặc đã đánh giá.");
        }

        // Change order status to DISPUTED
        stateMachine.requireTransition(OrderStatus.fromLegacy(order.getStatus()), OrderStatus.DISPUTED);
        order.setStatus(OrderStatus.DISPUTED.name());
        orderRepo.save(order);

        String evidenceUrlsStr = request.getEvidenceUrls() != null ? String.join(",", request.getEvidenceUrls()) : null;

        OrderDispute dispute = OrderDispute.builder()
                .order(order)
                .openedByUser(currentUser)
                .reason(request.getReason())
                .evidenceUrls(evidenceUrlsStr)
                .compensationAmount(request.getCompensationAmount())
                .status("OPEN")
                .build();

        OrderDispute saved = disputeRepo.save(dispute);

        // Notify seller
        notifyUser(order.getSellerTeam().getOwner(), "Khiếu nại mới", 
            "Người mua đã mở khiếu nại cho đơn: " + order.getTitle(), "DISPUTE_OPENED", null);

        return toDTO(saved);
    }

    @Transactional
    public OrderDisputeDTO respondDispute(UUID disputeId, RespondDisputeRequest request, User currentUser) {
        OrderDispute dispute = disputeRepo.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));

        if (!dispute.getStatus().equals("OPEN")) {
            throw new RuntimeException("Chỉ có thể phản hồi khiếu nại đang MỞ.");
        }

        if (!dispute.getOrder().getSellerTeam().getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ chủ xưởng bán mới được phản hồi khiếu nại này.");
        }

        String evidenceUrlsStr = request.getEvidenceUrls() != null ? String.join(",", request.getEvidenceUrls()) : null;
        if (evidenceUrlsStr != null && !evidenceUrlsStr.isEmpty()) {
            String existing = dispute.getEvidenceUrls() != null ? dispute.getEvidenceUrls() + "," : "";
            dispute.setEvidenceUrls(existing + evidenceUrlsStr);
        }
        
        dispute.setResolutionNote(dispute.getResolutionNote() != null ? 
            dispute.getResolutionNote() + "\nPhản hồi từ seller: " + request.getNote() : "Phản hồi từ seller: " + request.getNote());
        dispute.setStatus("RESPONDED");

        OrderDispute saved = disputeRepo.save(dispute);

        // Notify buyer
        notifyUser(dispute.getOpenedByUser(), "Phản hồi khiếu nại", 
            "Người bán đã phản hồi khiếu nại đơn: " + dispute.getOrder().getTitle(), "DISPUTE_RESPONDED", null);

        return toDTO(saved);
    }

    @Transactional
    public OrderDisputeDTO resolveDispute(UUID disputeId, ResolveDisputeRequest request, User currentUser) {
        OrderDispute dispute = disputeRepo.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));

        if (!dispute.getStatus().equals("OPEN") && !dispute.getStatus().equals("RESPONDED")) {
            throw new RuntimeException("Chỉ có thể giải quyết khiếu nại đang MỞ hoặc ĐÃ PHẢN HỒI.");
        }

        dispute.setResolutionNote(dispute.getResolutionNote() != null ? 
            dispute.getResolutionNote() + "\nGiải quyết: " + request.getResolutionNote() : "Giải quyết: " + request.getResolutionNote());
        dispute.setStatus("RESOLVED");
        dispute.setResolvedByUser(currentUser);
        dispute.setResolvedAt(LocalDateTime.now());

        InterGroupOrder order = dispute.getOrder();
        stateMachine.requireTransition(OrderStatus.fromLegacy(order.getStatus()), OrderStatus.RESOLVED);
        order.setStatus(OrderStatus.RESOLVED.name());
        orderRepo.save(order);

        OrderDispute saved = disputeRepo.save(dispute);

        // Notify both parties
        notifyUser(order.getSellerTeam().getOwner(), "Khiếu nại đã giải quyết", 
            "Khiếu nại đơn: " + order.getTitle() + " đã được giải quyết.", "DISPUTE_RESOLVED", null);
        if (!currentUser.getId().equals(dispute.getOpenedByUser().getId())) {
            notifyUser(dispute.getOpenedByUser(), "Khiếu nại đã giải quyết", 
                "Khiếu nại đơn: " + order.getTitle() + " đã được giải quyết.", "DISPUTE_RESOLVED", null);
        }

        return toDTO(saved);
    }

    public List<OrderDisputeDTO> getDisputesByOrder(UUID orderId) {
        return disputeRepo.findByOrderId(orderId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private OrderDisputeDTO toDTO(OrderDispute dispute) {
        return OrderDisputeDTO.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrder().getId())
                .openedByUserId(dispute.getOpenedByUser().getId())
                .reason(dispute.getReason())
                .evidenceUrls(dispute.getEvidenceUrls())
                .compensationAmount(dispute.getCompensationAmount())
                .status(dispute.getStatus())
                .resolutionNote(dispute.getResolutionNote())
                .resolvedByUserId(dispute.getResolvedByUser() != null ? dispute.getResolvedByUser().getId() : null)
                .resolvedAt(dispute.getResolvedAt())
                .createdAt(dispute.getCreatedAt())
                .build();
    }

    private void notifyUser(User user, String title, String message, String type, UUID taskId) {
        try {
            notificationService.createAndSend(user, title, message, type, taskId);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }
}
