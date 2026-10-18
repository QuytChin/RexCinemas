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
        return newPaymentReference(properties.getPaymentPrefix(), exists);
    }

    public String newPaymentReference(String configuredPrefix, Predicate<String> exists) {
        String prefix = configuredPrefix;
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
        return qrImageUrl(properties.getBankCode(), properties.getBankAccount(), paymentReference, amount);
    }

    public String qrImageUrl(String bankCode, String bankAccount, String paymentReference, BigDecimal amount) {
        if (blank(bankCode) || blank(bankAccount) || amount == null || amount.signum() < 0)
            throw new IllegalStateException("Thông tin VietQR chưa hợp lệ");
        return UriComponentsBuilder.fromUriString(QR_IMAGE_ENDPOINT)
                .queryParam("bank", bankCode)
                .queryParam("acc", bankAccount)
                .queryParam("amount", amount.stripTrailingZeros().toPlainString())
                .queryParam("des", paymentReference)
                .queryParam("template", "compact")
                .build()
                .encode()
                .toUriString();
    }

    public String bankAccount() { return properties.getBankAccount(); }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
