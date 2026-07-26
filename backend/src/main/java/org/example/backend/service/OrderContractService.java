package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.OrderContractDTO;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.OrderContract;
import org.example.backend.entity.User;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.OrderContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderContractService {

    private final OrderContractRepository contractRepo;
    private final InterGroupOrderRepository orderRepo;
    private final NotificationService notificationService;

    @Transactional
    public OrderContractDTO createContract(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        validateAccess(order, currentUser);

        Optional<OrderContract> existing = contractRepo.findByOrderId(orderId);
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }

        // Generate default terms based on order details
        String terms = String.format("HỢP ĐỒNG MUA BÁN\n\nBên Bán: %s\nBên Mua: %s\nĐơn hàng: %s\nTổng tiền: %s VND\n\nCác điều khoản chung áp dụng theo chính sách của hệ thống ORCA.",
                order.getSellerTeam().getName(),
                order.getBuyerTeam() != null ? order.getBuyerTeam().getName() : order.getBuyerUser().getFullName(),
                order.getTitle(),
                order.getQuotedPrice() != null ? order.getQuotedPrice() : 0.0);

        OrderContract contract = OrderContract.builder()
                .order(order)
                .terms(terms)
                .status("DRAFT")
                .build();

        OrderContract saved = contractRepo.save(contract);
        return toDTO(saved);
    }

    public OrderContractDTO getContractByOrderId(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        validateAccess(order, currentUser);

        OrderContract contract = contractRepo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Contract not found for this order"));

        return toDTO(contract);
    }

    @Transactional
    public OrderContractDTO signContract(UUID contractId, String signatureUrl, User currentUser) {
        OrderContract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        InterGroupOrder order = contract.getOrder();
        validateAccess(order, currentUser);

        if ("SIGNED".equals(contract.getStatus())) {
            throw new RuntimeException("Hợp đồng đã được ký bởi 2 bên.");
        }

        boolean isBuyer = order.getBuyerTeam() != null ? order.getBuyerTeam().getOwner().getId().equals(currentUser.getId()) : (order.getBuyerUser() != null && order.getBuyerUser().getId().equals(currentUser.getId()));
        boolean isSeller = order.getSellerTeam().getOwner().getId().equals(currentUser.getId());

        if (isBuyer) {
            contract.setBuyerSignatureUrl(signatureUrl);
        } else if (isSeller) {
            contract.setSellerSignatureUrl(signatureUrl);
        }

        // Check if both signed
        if (contract.getBuyerSignatureUrl() != null && contract.getSellerSignatureUrl() != null) {
            contract.setStatus("SIGNED");
            contract.setSignedAt(LocalDateTime.now());
            
            // Notify both parties
            if (order.getBuyerTeam() != null) {
                notifyUser(order.getBuyerTeam().getOwner(), "Hợp đồng đã hoàn tất", "Hợp đồng cho đơn hàng " + order.getTitle() + " đã được cả 2 bên ký.", "CONTRACT_SIGNED");
            } else if (order.getBuyerUser() != null) {
                notifyUser(order.getBuyerUser(), "Hợp đồng đã hoàn tất", "Hợp đồng cho đơn hàng " + order.getTitle() + " đã được cả 2 bên ký.", "CONTRACT_SIGNED");
            }
            notifyUser(order.getSellerTeam().getOwner(), "Hợp đồng đã hoàn tất", "Hợp đồng cho đơn hàng " + order.getTitle() + " đã được cả 2 bên ký.", "CONTRACT_SIGNED");
        }

        return toDTO(contractRepo.save(contract));
    }

    private void validateAccess(InterGroupOrder order, User currentUser) {
        boolean isBuyerTeamOwner = order.getBuyerTeam() != null && order.getBuyerTeam().getOwner().getId().equals(currentUser.getId());
        boolean isBuyerUser = order.getBuyerUser() != null && order.getBuyerUser().getId().equals(currentUser.getId());
        boolean isSeller = order.getSellerTeam().getOwner().getId().equals(currentUser.getId());

        if (!isBuyerTeamOwner && !isBuyerUser && !isSeller) {
            throw new RuntimeException("Bạn không có quyền truy cập hợp đồng này");
        }
    }

    private void notifyUser(User user, String title, String message, String type) {
        try {
            notificationService.createAndSend(user, title, message, type, null);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    private OrderContractDTO toDTO(OrderContract contract) {
        return OrderContractDTO.builder()
                .id(contract.getId())
                .orderId(contract.getOrder().getId())
                .terms(contract.getTerms())
                .buyerSignatureUrl(contract.getBuyerSignatureUrl())
                .sellerSignatureUrl(contract.getSellerSignatureUrl())
                .signedAt(contract.getSignedAt())
                .status(contract.getStatus())
                .fileUrl(contract.getFileUrl())
                .createdAt(contract.getCreatedAt())
                .build();
    }
}
