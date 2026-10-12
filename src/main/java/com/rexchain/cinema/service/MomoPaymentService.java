package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class MomoPaymentService {
    private final RestClient client = RestClient.create();
    private final String endpoint;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String baseUrl;

    public MomoPaymentService(@Value("${payment.momo.endpoint}") String endpoint,
                              @Value("${payment.momo.partner-code:}") String partnerCode,
                              @Value("${payment.momo.access-key:}") String accessKey,
                              @Value("${payment.momo.secret-key:}") String secretKey,
                              @Value("${payment.base-url:http://localhost:8080}") String baseUrl) {
        this.endpoint = endpoint;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
    }

    public boolean configured() {
        return !partnerCode.isBlank() && !accessKey.isBlank() && !secretKey.isBlank();
    }

    public String createPaymentUrl(Booking booking) {
        if (!configured())
            throw new IllegalStateException("Chưa cấu hình MoMo sandbox credentials");

        BigInteger exactAmount = booking.getTotalAmount().toBigIntegerExact();
        if (exactAmount.signum() < 0)
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");

        String requestId = booking.getBookingCode() + System.currentTimeMillis();
        String orderId = booking.getBookingCode();
        String amount = exactAmount.toString();
        String orderInfo = "Thanh toan ve " + booking.getBookingCode();
        String redirectUrl = baseUrl + "/payment/callback/momo";
        String ipnUrl = baseUrl + "/payment/callback/momo/ipn";
        String extraData = "";
        String requestType = "captureWallet";
        String raw = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestType", requestType);
        body.put("ipnUrl", ipnUrl);
        body.put("redirectUrl", redirectUrl);
        body.put("orderId", orderId);
        body.put("amount", exactAmount.longValueExact());
        body.put("orderInfo", orderInfo);
        body.put("requestId", requestId);
        body.put("extraData", extraData);
        body.put("lang", "vi");
        body.put("autoCapture", true);
        body.put("signature", CryptoUtil.hmac("HmacSHA256", secretKey, raw));

        Map<?, ?> response = client.post().uri(endpoint).body(body).retrieve().body(Map.class);
        if (response == null || !Objects.equals(String.valueOf(response.get("resultCode")), "0")) {
            throw new IllegalStateException("MoMo không tạo được giao dịch: "
                    + (response == null ? "no response" : response.get("message")));
        }
        Object payUrl = response.get("payUrl");
        if (payUrl == null) throw new IllegalStateException("MoMo không trả payUrl");
        return String.valueOf(payUrl);
    }

    public boolean verifyResult(Map<String, ?> params) {
        try {
            if (!configured() || !partnerCode.equals(value(params, "partnerCode"))) return false;
            String raw = "accessKey=" + accessKey
                    + "&amount=" + value(params, "amount")
                    + "&extraData=" + value(params, "extraData")
                    + "&message=" + value(params, "message")
                    + "&orderId=" + value(params, "orderId")
                    + "&orderInfo=" + value(params, "orderInfo")
                    + "&orderType=" + value(params, "orderType")
                    + "&partnerCode=" + value(params, "partnerCode")
                    + "&payType=" + value(params, "payType")
                    + "&requestId=" + value(params, "requestId")
                    + "&responseTime=" + value(params, "responseTime")
                    + "&resultCode=" + value(params, "resultCode")
                    + "&transId=" + value(params, "transId");
            String expected = CryptoUtil.hmac("HmacSHA256", secretKey, raw);
            return CryptoUtil.constantTimeEquals(expected, value(params, "signature"));
        } catch (Exception ex) {
            return false;
        }
    }

    private String value(Map<String, ?> params, String key) {
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
