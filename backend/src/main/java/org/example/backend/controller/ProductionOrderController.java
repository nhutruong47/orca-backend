package org.example.backend.controller;

import org.example.backend.dto.ProductionOrderDTO;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.ProductionOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/production")
@CrossOrigin("*")
public class ProductionOrderController {

    private final ProductionOrderService orderService;
    private final AccessControlService accessControlService;

    public ProductionOrderController(ProductionOrderService orderService, AccessControlService accessControlService) {
        this.orderService = orderService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/teams/{teamId}/orders")
    public ResponseEntity<?> getOrders(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {
        accessControlService.requireTeamMember(user, teamId);
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
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProductionOrderDTO dto) {
        accessControlService.requireTeamMember(user, teamId);
        ProductionOrder raw = mapToOrder(dto);
        ProductionOrder created = orderService.createOrder(teamId, raw);
        return ResponseEntity.ok(orderService.toDTO(created));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireProductionOrderAccess(user, orderId);
        ProductionOrder order = orderService.getById(orderId);
        return ResponseEntity.ok(orderService.toDTO(order));
    }

    @PatchMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User user,
            @RequestBody ProductionOrderDTO dto) {
        accessControlService.requireProductionOrderAccess(user, orderId);
        ProductionOrder raw = mapToOrder(dto);
        raw.setStatus(dto.getStatus());
        ProductionOrder updated = orderService.updateOrder(orderId, raw);
        return ResponseEntity.ok(orderService.toDTO(updated));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        accessControlService.requireProductionOrderAccess(user, orderId);
        ProductionOrder updated = orderService.updateStatus(orderId, body.get("status"));
        return ResponseEntity.ok(orderService.toDTO(updated));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        accessControlService.requireProductionOrderAccess(user, orderId);
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{teamId}/orders/active")
    public ResponseEntity<?> getActiveOrders(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        accessControlService.requireTeamMember(user, teamId);
        List<ProductionOrder> orders = orderService.getActiveOrders(teamId);
        List<ProductionOrderDTO> dtos = orders.stream()
                .map(orderService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ProductionOrder mapToOrder(ProductionOrderDTO body) {
        ProductionOrder o = new ProductionOrder();
        if (body.getTitle() != null) o.setTitle(body.getTitle());
        if (body.getDescription() != null) o.setDescription(body.getDescription());
        if (body.getCustomerName() != null) o.setCustomerName(body.getCustomerName());
        if (body.getProductType() != null) o.setProductType(body.getProductType());
        if (body.getProcessType() != null) o.setProcessType(body.getProcessType());
        if (body.getRoastLevel() != null) o.setRoastLevel(body.getRoastLevel());
        if (body.getPackageSize() != null) o.setPackageSize(body.getPackageSize());
        if (body.getTotalPackages() != null) o.setTotalPackages(body.getTotalPackages());
        if (body.getOutputTarget() != null) o.setOutputTarget(body.getOutputTarget());
        if (body.getExpectedYield() != null) o.setExpectedYield(body.getExpectedYield());
        if (body.getExpectedLoss() != null) o.setExpectedLoss(body.getExpectedLoss());
        if (body.getUnit() != null) o.setUnit(body.getUnit());
        if (body.getOrderDate() != null) o.setOrderDate(body.getOrderDate());
        if (body.getConfirmDate() != null) o.setConfirmDate(body.getConfirmDate());
        if (body.getProductionStartDate() != null) o.setProductionStartDate(body.getProductionStartDate());
        if (body.getCustomerDeliveryDate() != null) o.setCustomerDeliveryDate(body.getCustomerDeliveryDate());
        if (body.getSafetyBufferDays() != null) o.setSafetyBufferDays(body.getSafetyBufferDays());
        if (body.getRecipientName() != null) o.setRecipientName(body.getRecipientName());
        if (body.getRecipientPhone() != null) o.setRecipientPhone(body.getRecipientPhone());
        if (body.getShippingNote() != null) o.setShippingNote(body.getShippingNote());
        if (body.getContactPhoneAlt() != null) o.setContactPhoneAlt(body.getContactPhoneAlt());
        if (body.getDeliveryAddress() != null) o.setDeliveryAddress(body.getDeliveryAddress());
        if (body.getPreferredDeliveryFrom() != null) o.setPreferredDeliveryFrom(body.getPreferredDeliveryFrom());
        if (body.getPreferredDeliveryTo() != null) o.setPreferredDeliveryTo(body.getPreferredDeliveryTo());
        if (body.getDeliveryFailureAction() != null) o.setDeliveryFailureAction(body.getDeliveryFailureAction());
        if (body.getDeliveryNote() != null) o.setDeliveryNote(body.getDeliveryNote());
        if (body.getCancelRequested() != null) o.setCancelRequested(body.getCancelRequested());
        if (body.getBuyerViewed() != null) o.setBuyerViewed(body.getBuyerViewed());
        if (body.getSellerViewed() != null) o.setSellerViewed(body.getSellerViewed());
        return o;
    }
}
