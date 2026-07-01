package org.example.backend.service;

import org.example.backend.entity.PaymentTransaction;
import org.example.backend.entity.User;
import org.example.backend.repository.PaymentTransactionRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayosPaymentService {

    private final PayOS payOS;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private static final List<Plan> PLANS = List.of(
            new Plan("professional", "Professional", 129000, 500000),
            new Plan("enterprise", "Enterprise", 249000, 1500000)
    );

    public PayosPaymentService(PayOS payOS, PaymentTransactionRepository paymentRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.payOS = payOS;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createPaymentLink(User user, String planId) {
        Plan plan = findPlan(planId);
        long orderCode = System.currentTimeMillis() / 1000 + (long)(Math.random() * 10000);
        String txnRef = String.valueOf(orderCode);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setStatus("PENDING");
        transaction.setBankCode("PAYOS");
        transaction.setPaymentMethod("PAYOS");
        paymentRepository.save(transaction);

        String returnUrl = frontendUrl + "/payment-result?txnRef=" + txnRef + "&planId=" + planId;
        String cancelUrl = frontendUrl + "/upgrade";

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(plan.monthlyPrice())
                .description("Goi " + plan.name())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        try {
            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("checkoutUrl", data.getCheckoutUrl());
            response.put("txnRef", txnRef);
            response.put("planId", plan.id());
            response.put("planName", plan.name());
            response.put("amount", plan.monthlyPrice());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Loi khi tao link thanh toan PayOS: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> handleWebhook(Map<String, Object> requestBody) {
        try {
            Webhook webhookBody = objectMapper.convertValue(requestBody, Webhook.class);
            WebhookData data = payOS.webhooks().verify(webhookBody);

            if ("00".equals(webhookBody.getCode())) {
                String orderCodeStr = String.valueOf(data.getOrderCode());
                PaymentTransaction transaction = paymentRepository.findByTxnRef(orderCodeStr).orElse(null);
                if (transaction != null && !"PAID".equals(transaction.getStatus())) {
                    transaction.setStatus("PAID");
                    transaction.setPaidAt(LocalDateTime.now());
                    transaction.setVnpResponseCode("00");
                    transaction.setVnpTransactionStatus("00");
                    paymentRepository.save(transaction);

                    activatePlan(transaction.getUser(), transaction.getPlanId());
                }
            }
            return Map.of("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private void activatePlan(User user, String planId) {
        if (user == null) return;
        user.setAiPlan(planId);
        if ("enterprise".equalsIgnoreCase(planId)) {
            user.setAiPlanExpiresAt(LocalDateTime.now().plusDays(30));
        } else if ("professional".equalsIgnoreCase(planId) || "plus".equalsIgnoreCase(planId)) {
            user.setAiUsageCount(0);
            user.setAiPlanExpiresAt(null);
        } else {
            user.setAiPlanExpiresAt(LocalDateTime.now().plusMonths(1));
        }
        userRepository.save(user);
    }

    private Plan findPlan(String planId) {
        return PLANS.stream()
                .filter(plan -> plan.id().equalsIgnoreCase(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Goi AI khong hop le"));
    }

    private record Plan(String id, String name, long monthlyPrice, long tokenLimit) {}
}
