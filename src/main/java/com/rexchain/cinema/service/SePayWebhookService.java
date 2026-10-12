package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SePayWebhookService {
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final SePayProperties properties;

    public SePayWebhookService(BookingRepository bookings, BookingService bookingService,
                               SePayProperties properties) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.properties = properties;
    }

    @Transactional
    public void process(SePayWebhookRequest transaction) {
        if (transaction == null || blank(transaction.id()))
            throw new IllegalArgumentException("Thiếu mã giao dịch SePay");
        if (!"in".equalsIgnoreCase(trim(transaction.transferType()))) return;
        if (bookings.findBySepayTransactionId(trim(transaction.id())).isPresent()) return;

        String reference = trim(transaction.code());
        Booking booking = reference == null ? findByContent(transaction.content())
                : bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, reference).orElse(null);
        if (booking == null || booking.getPaymentMethod() != PaymentMethod.BANK_QR)
            throw new IllegalArgumentException("Không tìm thấy booking BANK_QR phù hợp");
        if (booking.getPaymentReference() == null || reference == null
                || !booking.getPaymentReference().equalsIgnoreCase(reference))
            throw new IllegalArgumentException("Mã thanh toán không khớp booking");
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.EXPIRED
                && booking.getStatus() != BookingStatus.CANCELLED)
            throw new IllegalStateException("Booking không còn chờ thanh toán");
        if (transaction.transferAmount() == null
                || transaction.transferAmount().compareTo(booking.getTotalAmount()) != 0)
            throw new IllegalArgumentException("Số tiền giao dịch không khớp booking");
        if (!blank(properties.getBankAccount())
                && !properties.getBankAccount().trim().equals(trim(transaction.accountNumber())))
            throw new IllegalArgumentException("Tài khoản nhận không khớp cấu hình");

        bookingService.completeOnlinePayment(booking.getBookingCode(), trim(transaction.id()));
    }

    private Booking findByContent(String content) {
        if (blank(content)) return null;
        String normalized = content.toUpperCase(Locale.ROOT);
        return bookings.findByPaymentMethodAndStatus(PaymentMethod.BANK_QR, BookingStatus.PENDING).stream()
                .filter(b -> b.getPaymentReference() != null
                        && normalized.contains(b.getPaymentReference().toUpperCase(Locale.ROOT)))
                .findFirst().orElse(null);
    }

    private String trim(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
