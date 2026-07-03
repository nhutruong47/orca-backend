package org.example.backend.controller;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.InterGroupOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> placeOrder(@RequestBody InterGroupOrderDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.createOrder(dto, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.getById(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.acceptOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.rejectOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.cancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/approve-cancel")
    public ResponseEntity<?> approveCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.approveCancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/reject-cancel")
    public ResponseEntity<?> rejectCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.rejectCancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Seller transitions the order from CONFIRMED -> SHIPPING.
     */
    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            return ResponseEntity.ok(orderService.shipOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Seller transitions the order from CONFIRMED -> SHIPPING -> DELIVERED.
     * Validation: only the seller (receiving team owner) can mark delivered.
     */
    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable UUID orderId,
                                          @RequestBody(required = false) Map<String, Object> payload,
                                          @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            String deliveryNote = payload != null && payload.get("deliveryNote") != null
                    ? payload.get("deliveryNote").toString() : null;
            return ResponseEntity.ok(orderService.deliverOrder(orderId, deliveryNote, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Buyer confirms the delivery, transitioning DELIVERED -> COMPLETED.
     * Validation: only the buyer (owning team / personal buyer) can confirm.
     */
    @PatchMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<?> confirmDelivery(@PathVariable UUID orderId,
                                             @RequestBody(required = false) Map<String, Object> payload,
                                             @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            String deliveryStatus = payload != null && payload.get("deliveryStatus") != null
                    ? payload.get("deliveryStatus").toString() : "ON_TIME";
            Integer rating = payload != null && payload.get("rating") instanceof Number
                    ? ((Number) payload.get("rating")).intValue() : 5;
            String comment = payload != null && payload.get("comment") != null
                    ? payload.get("comment").toString() : null;
            return ResponseEntity.ok(orderService.confirmDelivery(orderId, deliveryStatus, rating, comment, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/buyer-confirm")
    public ResponseEntity<?> buyerConfirmDelivery(@PathVariable UUID orderId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> payload) {
        try {
            accessControlService.requireInterGroupOrderAccess(user, orderId);
            String deliveryStatus = (String) payload.get("deliveryStatus");
            int rating = ((Number) payload.get("rating")).intValue();
            String comment = (String) payload.get("comment");
            return ResponseEntity.ok(orderService.buyerConfirmDelivery(orderId, deliveryStatus, rating, comment, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mark-viewed")
    public ResponseEntity<?> markViewed(@RequestBody Map<String, Object> payload,
                                        @AuthenticationPrincipal User user) {
        try {
            List<String> orderIdsStr = (List<String>) payload.get("orderIds");
            List<UUID> orderIds = orderIdsStr.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toList());
            String role = (String) payload.get("role");
            // Ensure current user has access to every order being marked
            for (UUID orderId : orderIds) {
                accessControlService.requireInterGroupOrderAccess(user, orderId);
            }
            orderService.markOrdersAsViewed(orderIds, role);
            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
