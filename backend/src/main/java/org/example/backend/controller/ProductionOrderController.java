package org.example.backend.controller;

import org.example.backend.dto.ProductionOrderDTO;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.service.ProductionOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/production")
@CrossOrigin("*")
public class ProductionOrderController {

    private final ProductionOrderService orderService;

    public ProductionOrderController(ProductionOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/teams/{teamId}/orders")
    public ResponseEntity<?> getOrders(
            @PathVariable UUID teamId,
            @RequestParam(required = false) String status) {
        List<ProductionOrder> orders;
        if (status != null && !status.isBlank()) {
            orders = orderService.getActiveOrders(teamId);
        } else {
            orders = orderService.getOrdersByTeam(teamId);
        }
        List<ProductionOrderDTO> dtos = orders.stream()
                .map(orderService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/teams/{teamId}/orders")
    public ResponseEntity<?> createOrder(
            @PathVariable UUID teamId,
            @RequestBody Map<String, Object> body) {
        try {
            ProductionOrder raw = mapToOrder(body);
            ProductionOrder created = orderService.createOrder(teamId, raw);
            return ResponseEntity.ok(orderService.toDTO(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId) {
        try {
            ProductionOrder order = orderService.getById(orderId);
            return ResponseEntity.ok(orderService.toDTO(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(
            @PathVariable UUID orderId,
            @RequestBody Map<String, Object> body) {
        try {
            ProductionOrder raw = mapToOrder(body);
            raw.setStatus(getString(body, "status"));
            ProductionOrder updated = orderService.updateOrder(orderId, raw);
            return ResponseEntity.ok(orderService.toDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> body) {
        try {
            ProductionOrder updated = orderService.updateStatus(orderId, body.get("status"));
            return ResponseEntity.ok(orderService.toDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable UUID orderId) {
        try {
            orderService.deleteOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/orders/active")
    public ResponseEntity<?> getActiveOrders(@PathVariable UUID teamId) {
        List<ProductionOrder> orders = orderService.getActiveOrders(teamId);
        List<ProductionOrderDTO> dtos = orders.stream()
                .map(orderService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ProductionOrder mapToOrder(Map<String, Object> body) {
        ProductionOrder o = new ProductionOrder();
        o.setTitle(getString(body, "title"));
        o.setDescription(getString(body, "description"));
        o.setCustomerName(getString(body, "customerName"));
        o.setProductType(getString(body, "productType"));
        o.setProcessType(getString(body, "processType"));
        o.setRoastLevel(getString(body, "roastLevel"));
        o.setPackageSize(getString(body, "packageSize"));
        o.setTotalPackages(getInt(body, "totalPackages"));
        o.setOutputTarget(getDouble(body, "outputTarget"));
        o.setExpectedYield(getDouble(body, "expectedYield"));
        o.setExpectedLoss(getDouble(body, "expectedLoss"));
        o.setUnit(getString(body, "unit"));
        o.setOrderDate(getLocalDate(body, "orderDate"));
        o.setConfirmDate(getLocalDate(body, "confirmDate"));
        o.setProductionStartDate(getLocalDate(body, "productionStartDate"));
        o.setCustomerDeliveryDate(getLocalDate(body, "customerDeliveryDate"));
        o.setSafetyBufferDays(getInt(body, "safetyBufferDays"));
        o.setRecipientName(getString(body, "recipientName"));
        o.setRecipientPhone(getString(body, "recipientPhone"));
        o.setShippingNote(getString(body, "shippingNote"));
        return o;
    }

    private String getString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
    }

    private Double getDouble(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer getInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private java.time.LocalDate getLocalDate(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        try { return java.time.LocalDate.parse(v.toString().substring(0, 10)); } catch (Exception e) { return null; }
    }
}
