package org.example.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.entity.PaymentTransaction;
import org.example.backend.entity.User;
import org.example.backend.repository.PaymentTransactionRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnpayPaymentService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<Plan> PLANS = List.of(
            new Plan("plus", "AI Plus", 129000, 500000),
            new Plan("pro", "AI Pro", 249000, 1500000)
    );

    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.tmn-code:DEMOV210}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.return-url:http://localhost:8080/api/payments/vnpay/return}")
    private String returnUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public VnpayPaymentService(PaymentTransactionRepository paymentRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> createPayment(User user, String planId, String bankCode, HttpServletRequest request) {
        Plan plan = findPlan(planId);
        String txnRef = "ORCA" + System.currentTimeMillis();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setBankCode(bankCode);
        paymentRepository.save(transaction);

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime expireAt = now.plusMinutes(15);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(plan.monthlyPrice() * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan goi " + plan.name() + " cho ORCA");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp(request));
        params.put("vnp_CreateDate", now.format(VNPAY_TIME_FORMAT));
        params.put("vnp_ExpireDate", expireAt.format(VNPAY_TIME_FORMAT));
        if (bankCode != null && !bankCode.isBlank()) {
            params.put("vnp_BankCode", bankCode.trim());
        }

        String hashData = buildQuery(params, true);
        String query = buildQuery(params, true);
        String secureHash = hmacSha512(hashSecret, hashData);
        String paymentUrl = payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("txnRef", txnRef);
        response.put("planId", plan.id());
        response.put("planName", plan.name());
        response.put("amount", plan.monthlyPrice());
        return response;
    }

    @Transactional
    public Map<String, Object> createMockTransfer(User user, String planId) {
        Plan plan = findPlan(planId);
        String txnRef = "MOCK" + System.currentTimeMillis();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setStatus("PAID");
        transaction.setVnpResponseCode("00");
        transaction.setVnpTransactionStatus("00");
        transaction.setVnpTransactionNo("MOCK-" + txnRef);
        transaction.setBankCode("VIRTUAL");
        transaction.setPaidAt(LocalDateTime.now());
        paymentRepository.save(transaction);

        user.setAiPlan(plan.id());
        user.setAiPlanExpiresAt(LocalDateTime.now().plusMonths(1));
        userRepository.save(user);

        Map<String, Object> response = result(txnRef, plan.id(), "SUCCESS", "Chuyen khoan ao thanh cong", null);
        response.put("planName", plan.name());
        response.put("amount", plan.monthlyPrice());
        return response;
    }

    @Transactional
    public Map<String, Object> handleReturn(Map<String, String> params) {
        return processVnpayResult(params, false);
    }

    @Transactional
    public Map<String, Object> handleIpn(Map<String, String> params) {
        return processVnpayResult(params, true);
    }

    public String buildFrontendRedirect(Map<String, Object> result) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/payment-result")
                .queryParam("status", result.get("status"))
                .queryParam("txnRef", result.get("txnRef"))
                .queryParam("planId", result.get("planId"))
                .queryParam("message", result.get("message"))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private Map<String, Object> processVnpayResult(Map<String, String> params, boolean ipn) {
        String txnRef = params.get("vnp_TxnRef");
        PaymentTransaction transaction = paymentRepository.findByTxnRef(txnRef)
                .orElse(null);

        if (!verifySecureHash(params)) {
            return result(txnRef, null, "INVALID_SIGNATURE", "Chu ky VNPAY khong hop le", ipn ? "97" : null);
        }
        if (transaction == null) {
            return result(txnRef, null, "NOT_FOUND", "Khong tim thay giao dich", ipn ? "01" : null);
        }

        long vnpAmount = parseLong(params.get("vnp_Amount"));
        if (vnpAmount != transaction.getAmount() * 100) {
            return result(txnRef, transaction.getPlanId(), "INVALID_AMOUNT", "So tien khong khop", ipn ? "04" : null);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean paid = "00".equals(responseCode) && "00".equals(transactionStatus);

        transaction.setVnpResponseCode(responseCode);
        transaction.setVnpTransactionStatus(transactionStatus);
        transaction.setVnpTransactionNo(params.get("vnp_TransactionNo"));

        if (paid) {
            transaction.setStatus("PAID");
            transaction.setPaidAt(LocalDateTime.now());
            User user = transaction.getUser();
            user.setAiPlan(transaction.getPlanId());
            user.setAiPlanExpiresAt(LocalDateTime.now().plusMonths(1));
            userRepository.save(user);
        } else {
            transaction.setStatus("FAILED");
        }
        paymentRepository.save(transaction);

        return result(
                txnRef,
                transaction.getPlanId(),
                paid ? "SUCCESS" : "FAILED",
                paid ? "Thanh toan thanh cong" : "Thanh toan khong thanh cong",
                ipn ? "00" : null
        );
    }

    private boolean verifySecureHash(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> signedParams = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null
                    && value != null
                    && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
                signedParams.put(key, value);
            }
        });

        String hashData = buildQuery(signedParams, true);
        String expectedHash = hmacSha512(hashSecret, hashData);
        return expectedHash.equalsIgnoreCase(receivedHash);
    }

    private String buildQuery(Map<String, String> params, boolean encode) {
        StringBuilder builder = new StringBuilder();
        params.forEach((key, value) -> {
            if (value == null || value.isBlank()) return;
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            if (encode) {
                builder.append(urlEncode(key)).append('=').append(urlEncode(value));
            } else {
                builder.append(key).append('=').append(value);
            }
        });
        return builder.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String key, String data) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing VNPAY hash secret");
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte item : bytes) {
                hash.append(String.format("%02x", item));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign VNPAY request", ex);
        }
    }

    private Plan findPlan(String planId) {
        return PLANS.stream()
                .filter(plan -> plan.id().equalsIgnoreCase(planId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Goi AI khong hop le"));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    private Map<String, Object> result(String txnRef, String planId, String status, String message, String rspCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("txnRef", txnRef);
        result.put("planId", planId);
        result.put("status", status);
        result.put("message", message);
        if (rspCode != null) {
            result.put("RspCode", rspCode);
            result.put("Message", message);
        }
        return result;
    }

    private record Plan(String id, String name, long monthlyPrice, long tokenLimit) {
    }
}
