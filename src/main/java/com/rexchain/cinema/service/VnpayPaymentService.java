package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnpayPaymentService {
    private final String paymentUrl;
    private final String tmnCode;
    private final String hashSecret;
    private final String baseUrl;

    public VnpayPaymentService(@Value("${payment.vnpay.url}") String paymentUrl,
                               @Value("${payment.vnpay.tmn-code:}") String tmnCode,
                               @Value("${payment.vnpay.hash-secret:}") String hashSecret,
                               @Value("${payment.base-url:http://localhost:8080}") String baseUrl) {
        this.paymentUrl = paymentUrl;
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.baseUrl = baseUrl;
    }

    public boolean configured() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    public String createPaymentUrl(Booking booking, HttpServletRequest request) {
        if (!configured()) throw new IllegalStateException("Chưa cấu hình VNPAY sandbox credentials");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", expectedAmount(booking));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", booking.getBookingCode());
        params.put("vnp_OrderInfo", "Thanh toan ve " + booking.getBookingCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", baseUrl + "/payment/callback/vnpay");
        params.put("vnp_IpAddr", clientIp(request));
        params.put("vnp_CreateDate", now.format(format));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(format));
        String encoded = encodedFields(params);
        return paymentUrl + "?" + encoded + "&vnp_SecureHash="
                + CryptoUtil.hmac("HmacSHA512", hashSecret, encoded);
    }

    public boolean verify(Map<String, String> input) {
        if (!configured() || !tmnCode.equals(input.get("vnp_TmnCode"))) return false;
        String supplied = input.get("vnp_SecureHash");
        if (supplied == null || supplied.isBlank()) return false;
        Map<String, String> signed = new TreeMap<>();
        input.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType") && value != null && !value.isBlank()) {
                signed.put(key, value);
            }
        });
        String expected = CryptoUtil.hmac("HmacSHA512", hashSecret, encodedFields(signed));
        return CryptoUtil.constantTimeEquals(expected, supplied);
    }

    public String expectedAmount(Booking booking) {
        return booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString();
    }

    private String encodedFields(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr() == null ? "127.0.0.1" : request.getRemoteAddr();
    }
}
