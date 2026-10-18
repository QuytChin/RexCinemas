package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.BankTransferSnapshot;
import com.rexchain.cinema.dto.PaymentSettingsDto;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.repository.BankTransferSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class BankTransferPaymentService {
    private final PaymentSettingsService settings;
    private final BankTransferSnapshotRepository snapshots;
    private final SePayPaymentService qr;
    private final SePayProperties environment;

    public BankTransferPaymentService(PaymentSettingsService settings, BankTransferSnapshotRepository snapshots,
                                      SePayPaymentService qr, SePayProperties environment) {
        this.settings = settings; this.snapshots = snapshots; this.qr = qr; this.environment = environment;
    }

    public boolean isReady() {
        PaymentSettingsDto value = settings.current();
        return value.isEnabled() && !blank(value.getBankCode()) && !blank(value.getBankAccount())
                && (value.isSchemaAvailable() || environment.isEnabled());
    }

    public void requireReady() {
        if (!isReady()) throw new IllegalStateException("VietQR chưa có tài khoản ngân hàng Ready");
        if (!snapshots.isSchemaAvailable()) throw new IllegalStateException("Migration VietQR snapshot chưa được áp dụng");
    }

    public String newPaymentReference(Predicate<String> exists) {
        PaymentSettingsDto value = settings.current();
        String prefix = blank(value.getPaymentPrefix()) ? "REX" : value.getPaymentPrefix();
        prefix = prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return qr.newPaymentReference(prefix, exists);
    }

    public int paymentTimeoutMinutes() {
        int configured = settings.current().getPaymentTimeoutMinutes();
        return configured > 0 ? configured : 5;
    }

    public BankTransferSnapshot snapshot(Booking booking) {
        requireReady();
        PaymentSettingsDto value = settings.current();
        BankTransferSnapshot snapshot = new BankTransferSnapshot();
        snapshot.setBookingCode(booking.getBookingCode()); snapshot.setBankName(value.getBankName());
        snapshot.setBankCode(value.getBankCode()); snapshot.setBankAccount(value.getBankAccount());
        snapshot.setAccountHolder(value.getAccountHolder()); snapshot.setAmount(booking.getTotalAmount());
        snapshot.setPaymentReference(booking.getPaymentReference()); snapshot.setExpiresAt(booking.getExpiresAt());
        snapshots.save(snapshot);
        return snapshot;
    }

    public Optional<BankTransferSnapshot> findSnapshot(String bookingCode) { return snapshots.findByBookingCode(bookingCode); }

    public String qrImageUrl(BankTransferSnapshot snapshot) {
        return qr.qrImageUrl(snapshot.getBankCode(), snapshot.getBankAccount(), snapshot.getPaymentReference(), snapshot.getAmount());
    }

    public boolean snapshotSchemaAvailable() { return snapshots.isSchemaAvailable(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
