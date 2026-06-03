package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.entity.User;
import org.example.backend.service.VnpayPaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final VnpayPaymentService vnpayPaymentService;

    public PaymentController(VnpayPaymentService vnpayPaymentService) {
        this.vnpayPaymentService = vnpayPaymentService;
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<Map<String, Object>> createVnpayPayment(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        return ResponseEntity.ok(vnpayPaymentService.createPayment(
                user,
                body.get("planId"),
                body.get("bankCode"),
                request
        ));
    }

    @PostMapping("/mock/transfer")
    public ResponseEntity<Map<String, Object>> createMockTransfer(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(vnpayPaymentService.createMockTransfer(user, body.get("planId")));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> handleVnpayReturn(@RequestParam Map<String, String> params) {
        Map<String, Object> result = vnpayPaymentService.handleReturn(params);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(vnpayPaymentService.buildFrontendRedirect(result)));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, Object>> handleVnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, Object> result = vnpayPaymentService.handleIpn(params);
        return ResponseEntity.ok(Map.of(
                "RspCode", result.getOrDefault("RspCode", "99"),
                "Message", result.getOrDefault("Message", "Unknown error")
        ));
    }
}
