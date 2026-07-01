package org.example.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.entity.PaymentTransaction;
import org.example.backend.entity.User;
import org.example.backend.repository.PaymentTransactionRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
            new Plan("professional", "Professional", 129000, 500000),
            new Plan("enterprise", "Enterprise", 249000, 1500000)
    );

    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

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

    @Value("${mbbank.account-number:0999999999}")
    private String mbAccountNumber;

    @Value("${mbbank.account-name:NGUYEN VAN A}")
    private String mbAccountName;

    @Value("${mbbank.allow-manual-confirm:true}")
    private boolean mbAllowManualConfirm;

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
        transaction.setPaymentMethod("VNPAY");
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
    public Map<String, Object> createMockTransfer(User user, String planId, String method) {
        Plan plan = findPlan(planId);
        String paymentMethod = normalizePaymentMethod(method);
        String txnRef = paymentMethod + System.currentTimeMillis();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setStatus("PAID");
        transaction.setVnpResponseCode("00");
        transaction.setVnpTransactionStatus("00");
        transaction.setVnpTransactionNo(paymentMethod + "-" + txnRef);
        transaction.setBankCode(paymentMethod + "_QR");
        transaction.setPaymentMethod(paymentMethod);
        transaction.setQrPayload(buildVirtualQrPayload(paymentMethod, txnRef, plan));
        transaction.setPaidAt(LocalDateTime.now());
        paymentRepository.save(transaction);

        activatePlan(user, plan.id());

        Map<String, Object> response = result(txnRef, plan.id(), "SUCCESS", paymentMethodLabel(paymentMethod) + " QR ao thanh cong", null);
        response.put("planName", plan.name());
        response.put("amount", plan.monthlyPrice());
        response.put("paymentMethod", paymentMethod);
        return response;
    }

    @Transactional
    public Map<String, Object> createVirtualQrPayment(User user, String planId, String method) {
        Plan plan = findPlan(planId);
        String paymentMethod = normalizePaymentMethod(method);
        if ("MB_BANK".equals(paymentMethod)) {
            if (mbAllowManualConfirm) {
                return createMbBankQrPayment(user, plan);
            }
            return createLocalVirtualQrPayment(user, plan, paymentMethod);
        }

        return createLocalVirtualQrPayment(user, plan, paymentMethod);
    }

    private Map<String, Object> createLocalVirtualQrPayment(User user, Plan plan, String paymentMethod) {
        String txnRef = paymentMethod + System.currentTimeMillis();
        String qrPayload = buildVirtualQrPayload(paymentMethod, txnRef, plan);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setStatus("PENDING");
        transaction.setBankCode(paymentMethod + "_QR");
        transaction.setPaymentMethod(paymentMethod);
        transaction.setQrPayload(qrPayload);
        paymentRepository.save(transaction);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("txnRef", txnRef);
        response.put("planId", plan.id());
        response.put("planName", plan.name());
        response.put("amount", plan.monthlyPrice());
        response.put("paymentMethod", paymentMethod);
        response.put("paymentMethodName", paymentMethodLabel(paymentMethod));
        response.put("qrPayload", qrPayload);
        response.put("status", "PENDING");
        response.put("message", "Quet ma QR ao de thanh toan");
        response.put("expiresAt", LocalDateTime.now(VIETNAM_ZONE).plusMinutes(15).toString());
        return response;
    }

    private Map<String, Object> createMbBankQrPayment(User user, Plan plan) {
        String txnRef = "ORCAMB" + System.currentTimeMillis();
        String orderInfo = "Thanh toan goi " + plan.name();
        
        // VietQR format
        String qrCodeUrl = "https://img.vietqr.io/image/970422-" + mbAccountNumber + "-compact2.png?amount=" 
                + plan.monthlyPrice() + "&addInfo=" + urlEncode(txnRef) + "&accountName=" + urlEncode(mbAccountName);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTxnRef(txnRef);
        transaction.setUser(user);
        transaction.setPlanId(plan.id());
        transaction.setAmount(plan.monthlyPrice());
        transaction.setStatus("PENDING");
        transaction.setBankCode("MB_BANK_QR");
        transaction.setPaymentMethod("MB_BANK");
        transaction.setQrPayload(qrCodeUrl);
        transaction.setVnpResponseCode("PENDING");
        transaction.setVnpTransactionStatus("PENDING");
        paymentRepository.save(transaction);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("txnRef", txnRef);
        response.put("planId", plan.id());
        response.put("planName", plan.name());
        response.put("amount", plan.monthlyPrice());
        response.put("paymentMethod", "MB_BANK");
        response.put("paymentMethodName", "Chuyển khoản MB Bank");
        response.put("qrPayload", qrCodeUrl);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("status", "PENDING");
        response.put("message", "Vui lòng dùng ứng dụng ngân hàng quét mã QR để chuyển khoản. Nhập đúng nội dung chuyển khoản là: " + txnRef);
        response.put("expiresAt", LocalDateTime.now(VIETNAM_ZONE).plusMinutes(15).toString());
        return response;
    }

    @Transactional
    public Map<String, Object> confirmVirtualQrPayment(User user, String txnRef) {
        if (txnRef == null || txnRef.isBlank()) {
            throw new RuntimeException("Ma giao dich khong hop le");
        }

        PaymentTransaction transaction = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Khong tim thay giao dich"));

        if (transaction.getUser() == null || !transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ban khong co quyen xac nhan giao dich nay");
        }

        String paymentMethod = normalizePaymentMethod(transaction.getPaymentMethod());
        if ("MB_BANK".equals(paymentMethod) && !mbAllowManualConfirm) {
            throw new RuntimeException("Giao dich MB Bank can duoc xac nhan qua webhook");
        }

        if ("PAID".equalsIgnoreCase(transaction.getStatus())) {
            Map<String, Object> alreadyPaid = result(txnRef, transaction.getPlanId(), "SUCCESS", "Giao dich da duoc thanh toan", null);
            alreadyPaid.put("planName", findPlan(transaction.getPlanId()).name());
            alreadyPaid.put("amount", transaction.getAmount());
            alreadyPaid.put("paymentMethod", paymentMethod);
            return alreadyPaid;
        }

        transaction.setStatus("PAID");
        transaction.setVnpResponseCode("00");
        transaction.setVnpTransactionStatus("00");
        transaction.setVnpTransactionNo(paymentMethod + "-" + txnRef);
        transaction.setBankCode(paymentMethod + "_QR");
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaidAt(LocalDateTime.now());
        paymentRepository.save(transaction);

        activatePlan(user, transaction.getPlanId());

        Map<String, Object> response = result(txnRef, transaction.getPlanId(), "SUCCESS", paymentMethodLabel(paymentMethod) + " QR ao thanh cong", null);
        response.put("planName", findPlan(transaction.getPlanId()).name());
        response.put("amount", transaction.getAmount());
        response.put("paymentMethod", paymentMethod);
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

    @Transactional
    public Map<String, Object> handleMbBankReturn(Map<String, String> params) {
        return new HashMap<>(); // Thường không dùng return trực tiếp cho chuyển khoản ngân hàng
    }

    @Transactional
    public Map<String, Object> handleMbBankIpn(Map<String, Object> body) {
        // Có thể tích hợp webhook SePay/Casso ở đây
        return new HashMap<>();
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
            activatePlan(transaction.getUser(), transaction.getPlanId());
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



    private String normalizePaymentMethod(String method) {
        String normalized = method == null ? "VNPAY" : method.trim().toUpperCase(Locale.ROOT);
        if (!List.of("MB_BANK", "VNPAY").contains(normalized)) {
            throw new RuntimeException("Phuong thuc thanh toan khong hop le");
        }
        return normalized;
    }

    private String paymentMethodLabel(String method) {
        return "MB_BANK".equalsIgnoreCase(method) ? "MB Bank" : "VNPay";
    }

    private String buildVirtualQrPayload(String method, String txnRef, Plan plan) {
        return String.join("|",
                "ORCA",
                normalizePaymentMethod(method),
                txnRef,
                plan.id(),
                plan.name(),
                String.valueOf(plan.monthlyPrice()),
                "Thanh toan goi " + plan.name());
    }

    private void activatePlan(User user, String planId) {
        user.setAiPlan(planId);
        
        if ("enterprise".equalsIgnoreCase(planId)) {
            user.setAiPlanExpiresAt(LocalDateTime.now().plusDays(30));
        } else if ("professional".equalsIgnoreCase(planId) || "plus".equalsIgnoreCase(planId)) {
            user.setAiUsageCount(0); // Reset usages to 0 to give 100 new uses
            user.setAiPlanExpiresAt(null); // Optional: if Plus doesn't expire by time
        } else {
            user.setAiPlanExpiresAt(LocalDateTime.now().plusMonths(1));
        }
        
        userRepository.save(user);
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

    private int parseInt(Object value) {
        try {
            return Integer.parseInt(safeString(value));
        } catch (Exception ex) {
            return -1;
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
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
