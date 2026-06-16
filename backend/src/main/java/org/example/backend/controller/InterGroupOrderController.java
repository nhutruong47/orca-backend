package org.example.backend.controller;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.User;
import org.example.backend.service.InterGroupOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inter-orders")
public class InterGroupOrderController {

    @Autowired
    private InterGroupOrderService orderService;

    @GetMapping("/outbound/{buyerTeamId}")
    public ResponseEntity<List<InterGroupOrderDTO>> getOutboundOrders(@PathVariable UUID buyerTeamId) {
        return ResponseEntity.ok(orderService.getOutboundOrders(buyerTeamId));
    }

    @GetMapping("/outbound-personal")
    public ResponseEntity<List<InterGroupOrderDTO>> getMyOutboundOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getMyOutboundOrders(user));
    }

    @GetMapping("/inbound/{sellerTeamId}")
    public ResponseEntity<List<InterGroupOrderDTO>> getInboundOrders(@PathVariable UUID sellerTeamId) {
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

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.acceptOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.rejectOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/approve-cancel")
    public ResponseEntity<?> approveCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.approveCancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/reject-cancel")
    public ResponseEntity<?> rejectCancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.rejectCancelOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(orderService.completeOrder(orderId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mark-viewed")
    public ResponseEntity<?> markViewed(@RequestBody Map<String, Object> payload) {
        try {
            List<String> orderIdsStr = (List<String>) payload.get("orderIds");
            List<UUID> orderIds = orderIdsStr.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toList());
            String role = (String) payload.get("role");
            orderService.markOrdersAsViewed(orderIds, role);
            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
