package org.example.backend.service;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.Goal;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterGroupOrderService {

    private final InterGroupOrderRepository orderRepo;
    private final TeamRepository teamRepo;
    private final GoalRepository goalRepo;

    public InterGroupOrderService(InterGroupOrderRepository orderRepo, TeamRepository teamRepo,
            GoalRepository goalRepo) {
        this.orderRepo = orderRepo;
        this.teamRepo = teamRepo;
        this.goalRepo = goalRepo;
    }

    public List<InterGroupOrderDTO> getOutboundOrders(UUID buyerTeamId) {
        return orderRepo.findByBuyerTeamIdOrderByCreatedAtDesc(buyerTeamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InterGroupOrderDTO> getInboundOrders(UUID sellerTeamId) {
        return orderRepo.findBySellerTeamIdOrderByCreatedAtDesc(sellerTeamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public InterGroupOrderDTO createOrder(InterGroupOrderDTO dto, User currentUser) {
        Team buyerTeam = teamRepo.findById(UUID.fromString(dto.getBuyerTeamId()))
                .orElseThrow(() -> new RuntimeException("Buyer team not found"));

        Team sellerTeam = teamRepo.findById(UUID.fromString(dto.getSellerTeamId()))
                .orElseThrow(() -> new RuntimeException("Seller team not found"));

        if (!buyerTeam.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the team owner can place inter-group orders.");
        }

        // Trust check: block if trust score < 30% and has >= 3 orders
        if (buyerTeam.getTotalOrders() >= 3) {
            int trustScore = (int) ((double) buyerTeam.getCompletedOrders() / buyerTeam.getTotalOrders() * 100);
            if (trustScore < 30) {
                throw new RuntimeException("Uy tín quá thấp (" + trustScore + "%). Không thể đặt hàng.");
            }
        }

        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setQuantity(dto.getQuantity());
        order.setDeadline(dto.getDeadline());
        order.setStatus("PENDING");

        // Increment buyer total orders
        buyerTeam.setTotalOrders(buyerTeam.getTotalOrders() + 1);
        teamRepo.save(buyerTeam);

        return toDTO(orderRepo.save(order));
    }

    public InterGroupOrderDTO acceptOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Team sellerTeam = order.getSellerTeam();
        if (!sellerTeam.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the receiving team owner can accept orders.");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order is not in PENDING state.");
        }

        // 1. Change Order Status
        order.setStatus("ACCEPTED");

        // 2. Automatically generate a Goal in the seller's Team
        Goal autoGoal = new Goal();
        autoGoal.setTeam(sellerTeam);
        autoGoal.setOwner(currentUser);
        autoGoal.setTitle("[Đơn Hàng] " + order.getTitle());
        autoGoal.setOutputTarget("SL: " + order.getQuantity() + " | " + order.getDescription());
        autoGoal.setPriority(2); // Normal priority
        autoGoal.setDeadline(order.getDeadline());
        autoGoal.setStatus("PUBLISHED");
        autoGoal.setTotalTasks(0);
        autoGoal.setCompletedTasks(0);

        Goal savedGoal = goalRepo.save(autoGoal);

        // 3. Link the goal to the order
        order.setLinkedGoalId(savedGoal.getId());

        return toDTO(orderRepo.save(order));
    }

    public InterGroupOrderDTO rejectOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Team sellerTeam = order.getSellerTeam();
        if (!sellerTeam.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the receiving team owner can reject orders.");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order is not in PENDING state.");
        }

        order.setStatus("REJECTED");
        return toDTO(orderRepo.save(order));
    }

    private InterGroupOrderDTO toDTO(InterGroupOrder order) {
        InterGroupOrderDTO dto = new InterGroupOrderDTO();
        dto.setId(order.getId().toString());
        dto.setBuyerTeamId(order.getBuyerTeam().getId().toString());
        dto.setBuyerTeamName(order.getBuyerTeam().getName());
        dto.setSellerTeamId(order.getSellerTeam().getId().toString());
        dto.setSellerTeamName(order.getSellerTeam().getName());
        dto.setTitle(order.getTitle());
        dto.setDescription(order.getDescription());
        dto.setQuantity(order.getQuantity());
        dto.setDeadline(order.getDeadline());
        dto.setStatus(order.getStatus());
        dto.setLinkedGoalId(order.getLinkedGoalId() != null ? order.getLinkedGoalId().toString() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCancelledBy(order.getCancelledBy());

        // Buyer trust score
        Team buyer = order.getBuyerTeam();
        int trustScore = buyer.getTotalOrders() > 0
                ? (int) ((double) buyer.getCompletedOrders() / buyer.getTotalOrders() * 100)
                : 100; // New team = 100% trust
        dto.setBuyerTrustScore(trustScore);

        return dto;
    }

    /**
     * Hủy đơn hàng — chỉ Buyer Owner hoặc Seller Owner mới được hủy
     */
    @Transactional
    public InterGroupOrderDTO cancelOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PENDING".equals(order.getStatus()) && !"ACCEPTED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn PENDING hoặc ACCEPTED mới được hủy.");
        }

        boolean isBuyerOwner = order.getBuyerTeam().getOwner().getId().equals(currentUser.getId());
        boolean isSellerOwner = order.getSellerTeam().getOwner().getId().equals(currentUser.getId());

        if (!isBuyerOwner && !isSellerOwner) {
            throw new RuntimeException("Chỉ chủ xưởng mua hoặc bán mới được hủy đơn.");
        }

        order.setStatus("CANCELED");
        order.setCancelledBy(isBuyerOwner ? "BUYER" : "SELLER");

        // Penalty: increment cancelled orders for the canceller's team
        if (isBuyerOwner) {
            Team buyer = order.getBuyerTeam();
            buyer.setCancelledOrders(buyer.getCancelledOrders() + 1);
            teamRepo.save(buyer);
        } else {
            Team seller = order.getSellerTeam();
            seller.setCancelledOrders(seller.getCancelledOrders() + 1);
            teamRepo.save(seller);
        }

        return toDTO(orderRepo.save(order));
    }

    /**
     * Hoàn thành đơn hàng — chỉ Seller Owner đánh dấu hoàn thành
     */
    @Transactional
    public InterGroupOrderDTO completeOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"ACCEPTED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn ACCEPTED mới đánh dấu hoàn thành.");
        }

        Team sellerTeam = order.getSellerTeam();
        if (!sellerTeam.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ chủ xưởng bán mới được đánh dấu hoàn thành.");
        }

        order.setStatus("COMPLETED");

        // Trust: +1 completed for both buyer and seller
        Team buyer = order.getBuyerTeam();
        buyer.setCompletedOrders(buyer.getCompletedOrders() + 1);
        teamRepo.save(buyer);

        sellerTeam.setCompletedOrders(sellerTeam.getCompletedOrders() + 1);
        sellerTeam.setTotalOrders(sellerTeam.getTotalOrders() + 1);
        teamRepo.save(sellerTeam);

        return toDTO(orderRepo.save(order));
    }
}
