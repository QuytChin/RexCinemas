package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.BankTransferSnapshot;
import com.rexchain.cinema.dto.PaymentTransactionDto;
import com.rexchain.cinema.dto.SePayWebhookRequest;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.repository.BankTransferSnapshotRepository;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class SePayWebhookService {
    private static final List<BookingStatus> RECONCILABLE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.EXPIRED, BookingStatus.CANCELLED);
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final SePayProperties properties;
    private final PaymentTransactionRepository transactions;
    private final BankTransferSnapshotRepository snapshots;
    private final LatePaymentCaseService latePaymentCases;

    /** Compatibility constructor for focused tests from before the reconciliation ledger. */
    public SePayWebhookService(BookingRepository bookings, BookingService bookingService,
                               SePayProperties properties) {
        this(bookings, bookingService, properties, null, null, null);
    }

    /** Compatibility constructor for reconciliation tests that do not need late-case persistence. */
    public SePayWebhookService(BookingRepository bookings, BookingService bookingService,
                               SePayProperties properties, PaymentTransactionRepository transactions,
                               BankTransferSnapshotRepository snapshots) {
        this(bookings, bookingService, properties, transactions, snapshots, null);
    }

    @Autowired
    public SePayWebhookService(BookingRepository bookings, BookingService bookingService,
                               SePayProperties properties, PaymentTransactionRepository transactions,
                               BankTransferSnapshotRepository snapshots, LatePaymentCaseService latePaymentCases) {
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.properties = properties;
        this.transactions = transactions;
        this.snapshots = snapshots;
        this.latePaymentCases = latePaymentCases;
    }

    @Transactional
    public void process(SePayWebhookRequest transaction) {
        if (transactions == null) {
            processLegacy(transaction);
            return;
        }
        if (transaction == null || blank(transaction.id()))
            throw new IllegalArgumentException("Thiếu mã giao dịch SePay");

        PaymentTransactionDto ledger = toLedger(transaction);
        if (!transactions.insertIfAbsent(ledger)) {
            transactions.findByProviderId(transaction.id().trim())
                    .ifPresent(existing -> transactions.audit(existing.getId(), "DUPLICATE_RECEIVED", "SEPAY", "Webhook được gửi lại"));
            return;
        }
        Optional<PaymentTransactionDto> saved = transactions.findByProviderId(transaction.id().trim());
        Long ledgerId = saved.map(PaymentTransactionDto::getId).orElse(null);

        if (!"in".equalsIgnoreCase(trim(transaction.transferType()))) {
            review(transaction, ledgerId, null, "Chỉ xử lý giao dịch tiền vào");
            return;
        }
        if (transaction.transferAmount() == null || transaction.transferAmount().signum() < 0) {
            review(transaction, ledgerId, null, "Số tiền giao dịch không hợp lệ");
            return;
        }

        String reference = trim(transaction.code());
        Booking booking = reference == null ? null
                : bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, reference).orElse(null);
        if (booking == null) {
            booking = findByContent(transaction.content());
            reference = booking == null ? null : trim(booking.getPaymentReference());
        }
        if (booking == null) {
            review(transaction, ledgerId, null, "Không tìm thấy booking BANK_QR phù hợp");
            return;
        }
        if (booking.getPaymentReference() == null || reference == null
                || !booking.getPaymentReference().equalsIgnoreCase(reference)) {
            review(transaction, ledgerId, booking, "Mã tham chiếu không khớp booking");
            return;
        }
        if (transaction.transferAmount().compareTo(booking.getTotalAmount()) != 0) {
            review(transaction, ledgerId, booking, "Số tiền giao dịch không khớp booking");
            return;
        }
        if (!recipientMatches(booking, transaction.accountNumber())) {
            review(transaction, ledgerId, booking, "Tài khoản nhận không khớp snapshot booking");
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.EXPIRED
                && booking.getStatus() != BookingStatus.CANCELLED) {
            review(transaction, ledgerId, booking, "Booking không còn ở trạng thái có thể đối soát");
            return;
        }

        transactions.updateStatus(transaction.id().trim(), "VERIFIED", booking.getBookingCode(),
                booking.getPaymentReference(), "Đã vượt qua kiểm tra giao dịch");
        transactions.findByProviderId(transaction.id().trim())
                .ifPresent(value -> transactions.audit(value.getId(), "VERIFIED", "SEPAY", "Đủ điều kiện settlement"));

        LocalDateTime bankRecordedAt = bankRecordedAt(transaction.transactionDate());
        if (bankRecordedAt == null) {
            review(transaction, ledgerId, booking, "Không xác định được thời điểm ngân hàng ghi nhận giao dịch");
            return;
        }
        Booking settled = bookingService.completeOnlinePayment(booking.getBookingCode(), trim(transaction.id()), bankRecordedAt);
        String status = settled.getStatus() == BookingStatus.CONFIRMED ? "SETTLED" : "LATE_PAYMENT_REFUND_PENDING";
        if (settled.getStatus() != BookingStatus.CONFIRMED && latePaymentCases != null)
            latePaymentCases.open(settled, trim(transaction.id()), transaction.transferAmount(), bankRecordedAt,
                    "Giao dịch hợp lệ nhưng không thể xác nhận booking an toàn");
        transactions.updateStatus(transaction.id().trim(), status, settled.getBookingCode(),
                settled.getPaymentReference(), settled.getStatus().name());
        transactions.findByProviderId(transaction.id().trim())
                .ifPresent(value -> transactions.audit(value.getId(), status, "SYSTEM", "Kết quả settlement: " + settled.getStatus()));
    }

    private void processLegacy(SePayWebhookRequest transaction) {
        if (transaction == null || blank(transaction.id()))
            throw new IllegalArgumentException("Thiếu mã giao dịch SePay");
        if (!"in".equalsIgnoreCase(trim(transaction.transferType()))) return;
        if (bookings.findBySepayTransactionId(trim(transaction.id())).isPresent()) return;
        String reference = trim(transaction.code());
        Booking booking = reference == null ? null
                : bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, reference).orElse(null);
        if (booking == null) {
            booking = findByContent(transaction.content());
            reference = booking == null ? null : trim(booking.getPaymentReference());
        }
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

    private void review(SePayWebhookRequest transaction, Long ledgerId, Booking booking, String reason) {
        transactions.updateStatus(transaction.id().trim(), "NEEDS_REVIEW",
                booking == null ? null : booking.getBookingCode(),
                booking == null ? null : booking.getPaymentReference(), reason);
        transactions.audit(ledgerId, "NEEDS_REVIEW", "SYSTEM", reason);
    }

    private PaymentTransactionDto toLedger(SePayWebhookRequest tx) {
        PaymentTransactionDto value = new PaymentTransactionDto();
        value.setProviderTransactionId(trim(tx.id())); value.setGateway(trim(tx.gateway()));
        value.setTransactionDate(trim(tx.transactionDate())); value.setAccountNumberLast4(last4(tx.accountNumber()));
        value.setCode(trim(tx.code())); value.setContent(limit(tx.content(), 500));
        value.setTransferType(trim(tx.transferType())); value.setAmount(tx.transferAmount());
        value.setReferenceCode(trim(tx.referenceCode()));
        return value;
    }

    private boolean recipientMatches(Booking booking, String receivedAccount) {
        if (blank(receivedAccount)) return false;
        if (snapshots != null) {
            Optional<BankTransferSnapshot> snapshot = snapshots.findByBookingCode(booking.getBookingCode());
            if (snapshot.isPresent()) return snapshot.get().getBankAccount().equals(receivedAccount.trim());
        }
        return blank(properties.getBankAccount()) || properties.getBankAccount().trim().equals(receivedAccount.trim());
    }

    private Booking findByContent(String content) {
        if (blank(content)) return null;
        java.util.Set<String> tokens = new java.util.LinkedHashSet<>();
        for (String token : content.split("[^A-Za-z0-9]+")) {
            if (token.length() >= 6 && token.length() <= 40) tokens.add(token.toUpperCase(Locale.ROOT));
            if (tokens.size() >= 40) break;
        }
        for (String token : tokens) {
            Booking candidate = bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, token)
                    .orElse(null);
            if (candidate != null && RECONCILABLE_STATUSES.contains(candidate.getStatus())) return candidate;
        }
        return null;
    }

    private String last4(String value) {
        String clean = trim(value);
        return clean == null ? null : clean.length() <= 4 ? clean : clean.substring(clean.length() - 4);
    }

    private String limit(String value, int length) {
        String clean = trim(value);
        return clean == null ? null : clean.length() <= length ? clean : clean.substring(0, length);
    }

    private String trim(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private LocalDateTime bankRecordedAt(String raw) {
        String value = trim(raw);
        if (value == null) return null;
        // Offset-aware payloads must be converted to the server zone; parsing them as
        // wall-clock time shifted "paid on time" decisions by the provider offset.
        try {
            return java.time.OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))) {
            try { return LocalDateTime.parse(value, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        return null;
    }
}
