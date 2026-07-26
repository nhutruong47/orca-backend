package org.example.backend.controller;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.InterGroupOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.backend.dto.ConfirmDeliveryRequest;
import org.example.backend.dto.MarkViewedRequest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inter-group-orders")
public class InterGroupOrderController {

    @Autowired
    private InterGroupOrderService orderService;

    @Autowired
    private AccessControlService accessControlService;

    @GetMapping("/outbound/{buyerTeamId}")
    public ResponseEntity<?> getOutboundOrders(@PathVariable UUID buyerTeamId,
                                               @AuthenticationPrincipal User user) {
        accessControlService.requireTeamMember(user, buyerTeamId);
        return ResponseEntity.ok(orderService.getOutboundOrders(buyerTeamId));
    }

    @GetMapping("/outbound-personal")
    public ResponseEntity<List<InterGroupOrderDTO>> getMyOutboundOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getMyOutboundOrders(user));
    }

    @GetMapping("/inbound/{sellerTeamId}")
    public ResponseEntity<?> getInboundOrders(@PathVariable UUID sellerTeamId,
                                              @AuthenticationPrincipal User user) {
        accessControlService.requireTeamMember(user, sellerTeamId);
        return ResponseEntity.ok(orderService.getInboundOrders(sellerTeamId));
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody InterGroupOrderDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.createOrder(dto, user));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.getById(orderId));
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.acceptOrder(orderId, user));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.rejectOrder(orderId, user));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user));
    }

    @PostMapping("/{orderId}/approve-cancel")
    public ResponseEntity<?> approveCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.approveCancelOrder(orderId, user));
    }

    @PostMapping("/{orderId}/reject-cancel")
    public ResponseEntity<?> rejectCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.rejectCancelOrder(orderId, user));
    }

    /**
     * Seller transitions the order from CONFIRMED -> SHIPPING.
     */
    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        return ResponseEntity.ok(orderService.shipOrder(orderId, user));
    }

    /**
     * Seller transitions the order from CONFIRMED -> SHIPPING -> DELIVERED.
     * Validation: only the seller (receiving team owner) can mark delivered.
     */
    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable UUID orderId,
                                          @RequestBody(required = false) ConfirmDeliveryRequest payload,
                                          @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        String deliveryNote = payload != null ? payload.getDeliveryNote() : null;
        return ResponseEntity.ok(orderService.deliverOrder(orderId, deliveryNote, user));
    }

    /**
     * Buyer confirms the delivery, transitioning DELIVERED -> COMPLETED.
     * Validation: only the buyer (owning team / personal buyer) can confirm.
     */
    @PatchMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<?> confirmDelivery(@PathVariable UUID orderId,
                                             @Valid @RequestBody(required = false) ConfirmDeliveryRequest payload,
                                             @AuthenticationPrincipal User user) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        String deliveryStatus = (payload != null && payload.getDeliveryStatus() != null) ? payload.getDeliveryStatus() : "ON_TIME";
        Integer rating = (payload != null && payload.getRating() != null) ? payload.getRating() : 5;
        String comment = payload != null ? payload.getComment() : null;
        return ResponseEntity.ok(orderService.confirmDelivery(orderId, deliveryStatus, rating, comment, user));
    }

    @PostMapping("/{orderId}/buyer-confirm")
    public ResponseEntity<?> buyerConfirmDelivery(@PathVariable UUID orderId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ConfirmDeliveryRequest payload) {
        accessControlService.requireInterGroupOrderAccess(user, orderId);
        String deliveryStatus = payload.getDeliveryStatus();
        int rating = payload.getRating() != null ? payload.getRating() : 5;
        String comment = payload.getComment();
        return ResponseEntity.ok(orderService.buyerConfirmDelivery(orderId, deliveryStatus, rating, comment, payload.getProofImageUrls(), user));
    }

    @PostMapping("/mark-viewed")
    public ResponseEntity<?> markViewed(@Valid @RequestBody MarkViewedRequest payload,
                                        @AuthenticationPrincipal User user) {
        List<UUID> orderIds = payload.getOrderIds();
        String role = payload.getRole();
        // Ensure current user has access to every order being marked
        for (UUID orderId : orderIds) {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
        }
        orderService.markOrdersAsViewed(orderIds, role);
        return ResponseEntity.ok(Map.of("message", "Success"));
    }
}
