package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.ManualRefundConfirmation;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.LatePaymentCaseAuditRepository;
import com.rexchain.cinema.repository.LatePaymentCaseRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LatePaymentCaseServiceTest {
    @Mock LatePaymentCaseRepository cases;
    @Mock LatePaymentCaseAuditRepository audits;
    @Mock NotificationService notifications;
    private LatePaymentCaseService service;

    @BeforeEach void setUp() { service = new LatePaymentCaseService(cases, audits, notifications); }

    @Test
    void failedRefundAttemptsAreAppendedBeforeSuccessfulRetry() {
        LatePaymentCase value = new LatePaymentCase();
        Booking booking = new Booking(); booking.setBookingCode("REX_TEST_LATE");
        value.setBooking(booking); value.setAmount(BigDecimal.valueOf(100_000));
        value.setStatus(LatePaymentCaseStatus.PENDING_REFUND);
        when(cases.findByIdForUpdate(9L)).thenReturn(Optional.of(value));
        when(cases.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markRefundFailed(9L, 1L, "admin@example.invalid", "Không nhận được xác nhận ngân hàng");
        service.markRefundFailed(9L, 1L, "admin@example.invalid", "Sai thông tin người nhận");
        LatePaymentCase refunded = service.confirmRefund(9L, 1L, "admin@example.invalid",
                new ManualRefundConfirmation("REF-9", BigDecimal.valueOf(100_000), null, "test receipt"));

        assertThat(refunded.getStatus()).isEqualTo(LatePaymentCaseStatus.REFUNDED);
        ArgumentCaptor<LatePaymentCaseAudit> captor = ArgumentCaptor.forClass(LatePaymentCaseAudit.class);
        verify(audits, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(LatePaymentCaseAudit::getAction)
                .containsExactly("REFUND_FAILED", "REFUND_FAILED", "REFUND_CONFIRMED");
        verify(notifications).latePaymentRefunded(booking);
    }

    @Test
    void confirmedLateRefundIsIdempotent() {
        LatePaymentCase value = new LatePaymentCase();
        value.setStatus(LatePaymentCaseStatus.REFUNDED);
        when(cases.findByIdForUpdate(10L)).thenReturn(Optional.of(value));

        assertThat(service.confirmRefund(10L, 1L, "admin@example.invalid",
                new ManualRefundConfirmation("ignored", BigDecimal.ONE, null, null))).isSameAs(value);
        verifyNoInteractions(audits, notifications);
        verify(cases, never()).save(any());
    }
}
