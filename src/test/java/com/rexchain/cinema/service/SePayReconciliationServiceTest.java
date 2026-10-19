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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SePayReconciliationServiceTest {
    @Mock BookingRepository bookings;
    @Mock BookingService bookingService;
    @Mock PaymentTransactionRepository transactions;
    @Mock BankTransferSnapshotRepository snapshots;
    private SePayWebhookService service;

    @BeforeEach
    void setUp() {
        SePayProperties properties = new SePayProperties();
        properties.setBankAccount("123456");
        service = new SePayWebhookService(bookings, bookingService, properties, transactions, snapshots);
    }

    @Test
    void validTransactionSettlesOnceAfterAmountReferenceAndRecipientChecks() {
        Booking booking = booking(BookingStatus.PENDING, BigDecimal.valueOf(100_000));
        PaymentTransactionDto ledger = ledger(11L);
        when(transactions.insertIfAbsent(any())).thenReturn(true);
        when(transactions.findByProviderId("tx-1")).thenReturn(Optional.of(ledger));
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));
        BankTransferSnapshot snapshot = new BankTransferSnapshot();
        snapshot.setBookingCode("REX-1"); snapshot.setBankAccount("123456");
        when(snapshots.findByBookingCode("REX-1")).thenReturn(Optional.of(snapshot));
        Booking confirmed = booking(BookingStatus.CONFIRMED, BigDecimal.valueOf(100_000));
        confirmed.setBookingCode("REX-1"); confirmed.setPaymentReference("REXABC");
        when(bookingService.completeOnlinePayment("REX-1", "tx-1", LocalDateTime.of(2026, 10, 20, 3, 59)))
                .thenReturn(confirmed);

        service.process(transaction("tx-1", "REXABC", "100000"));

        verify(bookingService).completeOnlinePayment("REX-1", "tx-1", LocalDateTime.of(2026, 10, 20, 3, 59));
        verify(transactions).updateStatus("tx-1", "SETTLED", "REX-1", "REXABC", "CONFIRMED");
    }

    @Test
    void wrongAmountIsRecordedForReviewAndDoesNotIssueTicket() {
        Booking booking = booking(BookingStatus.PENDING, BigDecimal.valueOf(100_000));
        when(transactions.insertIfAbsent(any())).thenReturn(true);
        when(transactions.findByProviderId("tx-2")).thenReturn(Optional.of(ledger(12L)));
        when(bookings.findByPaymentMethodAndPaymentReference(PaymentMethod.BANK_QR, "REXABC"))
                .thenReturn(Optional.of(booking));

        service.process(transaction("tx-2", "REXABC", "999"));

        verify(bookingService, never()).completeOnlinePayment(anyString(), anyString());
        verify(transactions).updateStatus(eq("tx-2"), eq("NEEDS_REVIEW"), eq("REX-1"), eq("REXABC"), contains("Số tiền"));
    }

    @Test
    void duplicateProviderTransactionIsOnlyAudited() {
        when(transactions.insertIfAbsent(any())).thenReturn(false);
        when(transactions.findByProviderId("tx-3")).thenReturn(Optional.of(ledger(13L)));

        service.process(transaction("tx-3", "REXABC", "100000"));

        verify(transactions).audit(13L, "DUPLICATE_RECEIVED", "SEPAY", "Webhook được gửi lại");
        verifyNoInteractions(bookings, bookingService, snapshots);
    }

    private Booking booking(BookingStatus status, BigDecimal amount) {
        Booking booking = new Booking(); booking.setBookingCode("REX-1");
        booking.setPaymentMethod(PaymentMethod.BANK_QR); booking.setPaymentReference("REXABC");
        booking.setStatus(status); booking.setTotalAmount(amount); return booking;
    }

    private PaymentTransactionDto ledger(Long id) {
        PaymentTransactionDto value = new PaymentTransactionDto(); value.setId(id); return value;
    }

    private SePayWebhookRequest transaction(String id, String code, String amount) {
        return new SePayWebhookRequest(id, "MBBank", "2026-10-20T03:59:00", "123456", null, code,
                code, "in", new BigDecimal(amount), "REF-" + id);
    }
}
