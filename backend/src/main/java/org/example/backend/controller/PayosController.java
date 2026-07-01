package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.PayosPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payos")
public class PayosController {

    private final PayosPaymentService payosPaymentService;

    public PayosController(PayosPaymentService payosPaymentService) {
        this.payosPaymentService = payosPaymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPaymentLink(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String planId = body.get("planId");
        return ResponseEntity.ok(payosPaymentService.createPaymentLink(user, planId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> body) {
        // PayOS webhook verification should happen inside the service
        Map<String, Object> result = payosPaymentService.handleWebhook(body);
        return ResponseEntity.ok(result);
    }
}
