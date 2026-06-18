package org.example.backend.service;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.Goal;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.ReviewRepository;
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
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepo;

    public InterGroupOrderService(InterGroupOrderRepository orderRepo, TeamRepository teamRepo,
            GoalRepository goalRepo, NotificationService notificationService,
            ReviewRepository reviewRepo) {
        this.orderRepo = orderRepo;
        this.teamRepo = teamRepo;
        this.goalRepo = goalRepo;
        this.notificationService = notificationService;
        this.reviewRepo = reviewRepo;
    }

    public List<InterGroupOrderDTO> getOutboundOrders(UUID buyerTeamId) {
        return orderRepo.findByBuyerTeamIdOrderByCreatedAtDesc(buyerTeamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InterGroupOrderDTO> getMyOutboundOrders(User currentUser) {
        return orderRepo.findByBuyerUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<InterGroupOrderDTO> getInboundOrders(UUID sellerTeamId) {
        return orderRepo.findBySellerTeamIdOrderByCreatedAtDesc(sellerTeamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public InterGroupOrderDTO createOrder(InterGroupOrderDTO dto, User currentUser) {
        if (dto.getBuyerTeamId() == null || dto.getBuyerTeamId().isBlank()) {
            Team sellerTeam = teamRepo.findById(UUID.fromString(dto.getSellerTeamId()))
                    .orElseThrow(() -> new RuntimeException("Seller team not found"));

            InterGroupOrder order = new InterGroupOrder();
            order.setBuyerUser(currentUser);
            order.setSellerTeam(sellerTeam);
            order.setTitle(dto.getTitle());
            order.setDescription(dto.getDescription());
            order.setQuantity(dto.getQuantity());
            order.setDeadline(dto.getDeadline());
            order.setStatus("PENDING");
            mapDeliveryFields(order, dto);

            InterGroupOrder saved = orderRepo.save(order);

            // Notify seller team owner about new order
            String buyerName = currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                    ? currentUser.getFullName() : currentUser.getUsername();
            notifyUser(sellerTeam.getOwner(),
                    "Đơn hàng mới",
                    "Bạn có đơn hàng mới từ " + buyerName + ": " + order.getTitle(),
                    "ORDER_CREATED", null);

            // Notify buyer (confirmation)
            notifyUser(currentUser,
                    "Đã gửi đơn hàng",
                    "Đơn hàng \"" + order.getTitle() + "\" đã được gửi đến " + sellerTeam.getName() + ". Chờ phản hồi.",
                    "ORDER_CREATED", null);

            return toDTO(saved);
        }

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
        mapDeliveryFields(order, dto);

        // Increment buyer total orders
        buyerTeam.setTotalOrders(buyerTeam.getTotalOrders() + 1);
        teamRepo.save(buyerTeam);

        InterGroupOrder saved = orderRepo.save(order);

        // Notify seller team owner about new order
        notifyUser(sellerTeam.getOwner(),
                "Đơn hàng mới",
                "Bạn có đơn hàng mới từ " + buyerTeam.getName() + ": " + order.getTitle(),
                "ORDER_CREATED", null);

        // Notify buyer (confirmation)
        notifyUser(currentUser,
                "Đã gửi đơn hàng",
                "Đơn hàng \"" + order.getTitle() + "\" đã được gửi đến " + sellerTeam.getName() + ". Chờ phản hồi.",
                "ORDER_CREATED", null);

        return toDTO(saved);
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
        order.setBuyerViewed(false);

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

        InterGroupOrder saved = orderRepo.save(order);

        // Notify buyer that order was accepted
        User buyerToNotify = resolveBuyerUser(order);
        if (buyerToNotify != null) {
            notifyUser(buyerToNotify,
                    "Đơn hàng được chấp nhận",
                    "Đơn hàng \"" + order.getTitle() + "\" đã được " + sellerTeam.getName() + " chấp nhận và bắt đầu gia công.",
                    "ORDER_ACCEPTED", null);
        }

        return toDTO(saved);
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
        order.setBuyerViewed(false);
        InterGroupOrder saved = orderRepo.save(order);

        // Notify buyer that order was rejected
        User buyerToNotify = resolveBuyerUser(order);
        if (buyerToNotify != null) {
            notifyUser(buyerToNotify,
                    "Đơn hàng bị từ chối",
                    "Đơn hàng \"" + order.getTitle() + "\" đã bị " + sellerTeam.getName() + " từ chối.",
                    "ORDER_REJECTED", null);
        }

        return toDTO(saved);
    }

    private InterGroupOrderDTO toDTO(InterGroupOrder order) {
        InterGroupOrderDTO dto = new InterGroupOrderDTO();
        dto.setId(order.getId().toString());
        dto.setBuyerTeamId(order.getBuyerTeam() != null ? order.getBuyerTeam().getId().toString() : null);
        dto.setBuyerTeamName(order.getBuyerTeam() != null ? order.getBuyerTeam().getName() : null);
        dto.setBuyerUserId(order.getBuyerUser() != null ? order.getBuyerUser().getId().toString() : null);
        dto.setBuyerUserName(order.getBuyerUser() != null
                ? (order.getBuyerUser().getFullName() != null && !order.getBuyerUser().getFullName().isBlank()
                        ? order.getBuyerUser().getFullName()
                        : order.getBuyerUser().getUsername())
                : null);
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
        dto.setCancelRequested(order.getCancelRequested());
        dto.setBuyerViewed(order.getBuyerViewed());
        dto.setSellerViewed(order.getSellerViewed());

        // Delivery profile
        dto.setContactPhone(order.getContactPhone());
        dto.setContactPhoneAlt(order.getContactPhoneAlt());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setPreferredDeliveryFrom(order.getPreferredDeliveryFrom());
        dto.setPreferredDeliveryTo(order.getPreferredDeliveryTo());
        dto.setDeliveryFailureAction(order.getDeliveryFailureAction());
        dto.setDeliveryNote(order.getDeliveryNote());

        // Delivery confirmation
        dto.setDeliveryConfirmed(order.getDeliveryConfirmed());
        dto.setDeliveryStatus(order.getDeliveryStatus());
        dto.setDeliveryConfirmedAt(order.getDeliveryConfirmedAt());

        // Buyer trust score — updated to include delivery performance
        Team buyer = order.getBuyerTeam();
        int trustScore = 100;
        if (buyer != null && buyer.getTotalOrders() > 0) {
            int completed = buyer.getCompletedOrders();
            int cancelled = buyer.getCancelledOrders();
            trustScore = (int) ((double) completed / (completed + cancelled) * 100);
        }
        dto.setBuyerTrustScore(trustScore);

        return dto;
    }

    /**
     * Khách hàng hoặc xưởng hủy đơn. Nếu người mua hủy đơn sau 24h, đơn sẽ chuyển sang trạng thái "Đang xin hủy".
     */
    @Transactional
    public InterGroupOrderDTO cancelOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PENDING".equals(order.getStatus()) && !"ACCEPTED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn PENDING hoặc ACCEPTED mới được hủy.");
        }

        boolean isBuyerOwner = (order.getBuyerTeam() != null && order.getBuyerTeam().getOwner().getId().equals(currentUser.getId()))
                || (order.getBuyerUser() != null && order.getBuyerUser().getId().equals(currentUser.getId()));
        boolean isSellerOwner = order.getSellerTeam().getOwner().getId().equals(currentUser.getId());

        if (!isBuyerOwner && !isSellerOwner) {
            throw new RuntimeException("Chỉ chủ xưởng mua hoặc bán mới được hủy đơn.");
        }

        // Logic 24h: Nếu người mua hủy và đơn đã quá 24h -> Chuyển thành Yêu cầu hủy
        if (isBuyerOwner && !isSellerOwner) {
            long hoursSinceCreation = java.time.temporal.ChronoUnit.HOURS.between(order.getCreatedAt(), LocalDateTime.now());
            if (hoursSinceCreation > 24) {
                order.setCancelRequested(true);
                order.setSellerViewed(false);
                InterGroupOrder saved = orderRepo.save(order);
                notifyUser(order.getSellerTeam().getOwner(),
                        "Yêu cầu hủy đơn hàng",
                        "Khách hàng đã yêu cầu hủy đơn \"" + order.getTitle() + "\". Hãy xem xét.",
                        "ORDER_CANCEL_REQUESTED", null);
                return toDTO(saved);
            }
        }

        // Thực hiện hủy ngay
        order.setStatus("CANCELED");
        order.setCancelledBy(isBuyerOwner ? "BUYER" : "SELLER");
        order.setCancelRequested(false);
        if (isBuyerOwner) {
            order.setSellerViewed(false);
        } else {
            order.setBuyerViewed(false);
        }

        // Penalty
        if (isBuyerOwner && order.getBuyerTeam() != null) {
            Team buyer = order.getBuyerTeam();
            buyer.setCancelledOrders(buyer.getCancelledOrders() + 1);
            teamRepo.save(buyer);
        } else if (isSellerOwner) {
            Team seller = order.getSellerTeam();
            seller.setCancelledOrders(seller.getCancelledOrders() + 1);
            teamRepo.save(seller);
        }

        InterGroupOrder saved = orderRepo.save(order);

        // Thông báo cho bên còn lại
        String cancellerName = isBuyerOwner ? "Bên mua" : "Bên bán";
        if (isBuyerOwner) {
            notifyUser(order.getSellerTeam().getOwner(),
                    "Đơn hàng đã bị hủy",
                    "Đơn hàng \"" + order.getTitle() + "\" đã bị hủy bởi " + cancellerName + " (chưa quá 24h).",
                    "ORDER_CANCELED", null);
        } else {
            User buyerToNotify = resolveBuyerUser(order);
            if (buyerToNotify != null) {
                notifyUser(buyerToNotify,
                        "Đơn hàng đã bị hủy",
                        "Đơn hàng \"" + order.getTitle() + "\" đã bị hủy bởi " + cancellerName + ".",
                        "ORDER_CANCELED", null);
            }
        }

        return toDTO(saved);
    }

    /**
     * Seller đồng ý yêu cầu hủy của Buyer
     */
    @Transactional
    public InterGroupOrderDTO approveCancelOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSellerTeam().getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ chủ xưởng bán mới có quyền duyệt hủy đơn.");
        }
        if (order.getCancelRequested() == null || !order.getCancelRequested()) {
            throw new RuntimeException("Đơn hàng không có yêu cầu hủy.");
        }

        order.setStatus("CANCELED");
        order.setCancelledBy("BUYER");
        order.setCancelRequested(false);
        order.setBuyerViewed(false);

        // Penalty cho buyer
        if (order.getBuyerTeam() != null) {
            Team buyer = order.getBuyerTeam();
            buyer.setCancelledOrders(buyer.getCancelledOrders() + 1);
            teamRepo.save(buyer);
        }

        InterGroupOrder saved = orderRepo.save(order);

        User buyerToNotify = resolveBuyerUser(order);
        if (buyerToNotify != null) {
            notifyUser(buyerToNotify,
                    "Yêu cầu hủy được chấp nhận",
                    "Xưởng " + order.getSellerTeam().getName() + " đã đồng ý hủy đơn \"" + order.getTitle() + "\".",
                    "ORDER_CANCELED", null);
        }

        return toDTO(saved);
    }

    /**
     * Seller từ chối yêu cầu hủy của Buyer (Chỉ hợp lệ nếu > 24h, mà code ở trên đã chặn việc tạo request nếu < 24h rồi)
     */
    @Transactional
    public InterGroupOrderDTO rejectCancelOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSellerTeam().getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ chủ xưởng bán mới có quyền từ chối hủy đơn.");
        }
        if (order.getCancelRequested() == null || !order.getCancelRequested()) {
            throw new RuntimeException("Đơn hàng không có yêu cầu hủy.");
        }

        // Tắt cờ yêu cầu hủy, đơn hàng trở lại bình thường
        order.setCancelRequested(false);
        order.setBuyerViewed(false);
        InterGroupOrder saved = orderRepo.save(order);

        User buyerToNotify = resolveBuyerUser(order);
        if (buyerToNotify != null) {
            notifyUser(buyerToNotify,
                    "Yêu cầu hủy bị từ chối",
                    "Xưởng " + order.getSellerTeam().getName() + " đã từ chối yêu cầu hủy đơn \"" + order.getTitle() + "\". Đơn hàng vẫn tiếp tục.",
                    "ORDER_CANCEL_REJECTED", null);
        }

        return toDTO(saved);
    }

    /**
     * Xưởng đánh dấu đã giao hàng — chuyển sang DELIVERED, chờ người mua xác nhận
     */
    @Transactional
    public InterGroupOrderDTO completeOrder(UUID orderId, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"ACCEPTED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn ACCEPTED mới đánh dấu đã giao.");
        }

        Team sellerTeam = order.getSellerTeam();
        if (!sellerTeam.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ chủ xưởng bán mới được đánh dấu đã giao.");
        }

        order.setStatus("DELIVERED");
        order.setBuyerViewed(false);

        InterGroupOrder saved = orderRepo.save(order);

        // Notify buyer that order has been delivered
        User buyerToNotify = resolveBuyerUser(order);
        if (buyerToNotify != null) {
            notifyUser(buyerToNotify,
                    "Đơn hàng đã giao — Chờ xác nhận",
                    "Đơn hàng \"" + order.getTitle() + "\" đã được " + sellerTeam.getName() + " giao. Vui lòng xác nhận đã nhận hàng đúng hẹn hay không.",
                    "ORDER_DELIVERED", null);
        }

        return toDTO(saved);
    }

    /**
     * Người mua xác nhận đã nhận hàng + đánh giá sao.
     * Trạng thái đơn: ON_TIME / LATE / NOT_DELIVERED
     * Trust score của xưởng được cập nhật theo đánh giá.
     */
    @Transactional
    public InterGroupOrderDTO buyerConfirmDelivery(UUID orderId, String deliveryStatus,
            int rating, String comment, User currentUser) {
        InterGroupOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn DELIVERED mới xác nhận được.");
        }

        // Verify buyer
        boolean isBuyer = (order.getBuyerTeam() != null && order.getBuyerTeam().getOwner().getId().equals(currentUser.getId()))
                || (order.getBuyerUser() != null && order.getBuyerUser().getId().equals(currentUser.getId()));
        if (!isBuyer) {
            throw new RuntimeException("Chỉ bên mua mới xác nhận được.");
        }

        if (order.getDeliveryConfirmed() != null && order.getDeliveryConfirmed()) {
            throw new RuntimeException("Đơn này đã được xác nhận trước đó.");
        }

        if (!"ON_TIME".equals(deliveryStatus) && !"LATE".equals(deliveryStatus) && !"NOT_DELIVERED".equals(deliveryStatus)) {
            throw new RuntimeException("Trạng thái giao hàng không hợp lệ.");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá phải từ 1 đến 5 sao.");
        }

        // Update order delivery info
        order.setDeliveryConfirmed(true);
        order.setDeliveryConfirmedAt(LocalDateTime.now());
        order.setDeliveryStatus(deliveryStatus);
        order.setStatus("COMPLETED");

        // Update seller trust stats
        Team sellerTeam = order.getSellerTeam();
        if ("ON_TIME".equals(deliveryStatus)) {
            sellerTeam.setOnTimeOrders(sellerTeam.getOnTimeOrders() + 1);
        } else if ("LATE".equals(deliveryStatus)) {
            sellerTeam.setLateOrders(sellerTeam.getLateOrders() + 1);
        }
        sellerTeam.setTotalRatings(sellerTeam.getTotalRatings() + 1);
        sellerTeam.setSumRatings(sellerTeam.getSumRatings() + rating);
        sellerTeam.setCompletedOrders(sellerTeam.getCompletedOrders() + 1);
        sellerTeam.setTotalOrders(sellerTeam.getTotalOrders() + 1);
        teamRepo.save(sellerTeam);

        // Update buyer team completed orders
        Team buyer = order.getBuyerTeam();
        if (buyer != null) {
            buyer.setCompletedOrders(buyer.getCompletedOrders() + 1);
            teamRepo.save(buyer);
        }

        // Save review
        org.example.backend.entity.Review review = new org.example.backend.entity.Review();
        review.setOrder(order);
        review.setBuyerTeam(order.getBuyerTeam());
        review.setBuyerUser(order.getBuyerUser());
        review.setSellerTeam(sellerTeam);
        review.setRating(rating);
        review.setComment(comment);
        review.setDeliveryResult(deliveryStatus);
        reviewRepo.save(review);

        InterGroupOrder saved = orderRepo.save(order);

        // Notify seller
        notifyUser(sellerTeam.getOwner(),
                "Người mua xác nhận giao hàng",
                "Đơn \"" + order.getTitle() + "\" đã được xác nhận: " + deliveryStatus + " | " + rating + " sao.",
                "ORDER_COMPLETED", null);

        return toDTO(saved);
    }

    @Transactional
    public void markOrdersAsViewed(List<UUID> orderIds, String role) {
        if (orderIds == null || orderIds.isEmpty()) return;
        List<InterGroupOrder> orders = orderRepo.findAllById(orderIds);
        for (InterGroupOrder order : orders) {
            if ("BUYER".equalsIgnoreCase(role)) {
                order.setBuyerViewed(true);
            } else if ("SELLER".equalsIgnoreCase(role)) {
                order.setSellerViewed(true);
            }
        }
        orderRepo.saveAll(orders);
    }

    // === Helper methods ===

    /** Map delivery fields from DTO to entity */
    private void mapDeliveryFields(InterGroupOrder order, InterGroupOrderDTO dto) {
        order.setContactPhone(dto.getContactPhone());
        order.setContactPhoneAlt(dto.getContactPhoneAlt());
        order.setDeliveryAddress(dto.getDeliveryAddress());
        order.setPreferredDeliveryFrom(dto.getPreferredDeliveryFrom());
        order.setPreferredDeliveryTo(dto.getPreferredDeliveryTo());
        order.setDeliveryFailureAction(dto.getDeliveryFailureAction());
        order.setDeliveryNote(dto.getDeliveryNote());
    }

    /** Resolve the buyer user: either buyerUser (personal) or buyerTeam owner */
    private User resolveBuyerUser(InterGroupOrder order) {
        if (order.getBuyerUser() != null) {
            return order.getBuyerUser();
        }
        if (order.getBuyerTeam() != null) {
            return order.getBuyerTeam().getOwner();
        }
        return null;
    }

    /** Send notification, silently ignore failures */
    private void notifyUser(User user, String title, String message, String type, UUID taskId) {
        try {
            notificationService.createAndSend(user, title, message, type, taskId);
        } catch (Exception e) {
            // Don't let notification failures break order operations
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }
}
