package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Predicate;

@Service
public class SePayPaymentService {
    private static final String QR_IMAGE_ENDPOINT = "https://qr.sepay.vn/img";
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SePayProperties properties;
    private final SecureRandom random = new SecureRandom();

    public SePayPaymentService(SePayProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        return properties.isEnabled()
                && !blank(properties.getBankAccount())
                && !blank(properties.getBankCode());
    }

    public void requireAvailable() {
        if (!isAvailable()) throw new IllegalStateException("Thanh toán QR ngân hàng chưa được cấu hình");
    }

    public String newPaymentReference(Predicate<String> exists) {
        String prefix = properties.getPaymentPrefix();
        prefix = blank(prefix) ? "REX" : prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        prefix = prefix.substring(0, Math.min(prefix.length(), 10));
        for (int attempt = 0; attempt < 12; attempt++) {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) suffix.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
            String reference = prefix + suffix;
            if (!exists.test(reference)) return reference;
        }
        throw new IllegalStateException("Không thể tạo mã thanh toán QR duy nhất");
    }

    public String qrImageUrl(String paymentReference, BigDecimal amount) {
        requireAvailable();
        return UriComponentsBuilder.fromUriString(QR_IMAGE_ENDPOINT)
                .queryParam("bank", properties.getBankCode())
                .queryParam("acc", properties.getBankAccount())
                .queryParam("amount", amount.stripTrailingZeros().toPlainString())
                .queryParam("des", paymentReference)
                .queryParam("template", "compact")
                .build()
                .encode()
                .toUriString();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
