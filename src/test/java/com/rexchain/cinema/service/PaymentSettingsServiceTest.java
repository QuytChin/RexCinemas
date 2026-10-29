package com.rexchain.cinema.service;

import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.PaymentSettingsDto;
import com.rexchain.cinema.dto.PaymentSettingsUpdateRequest;
import com.rexchain.cinema.repository.PaymentSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The seat-hold window is a fixed platform rule, so payment settings cannot change it. */
@ExtendWith(MockitoExtension.class)
class PaymentSettingsServiceTest {
    @Mock PaymentSettingsRepository repository;
    PaymentSettingsService service;

    @BeforeEach
    void setUp() {
        service = new PaymentSettingsService(repository, new SePayProperties());
    }

    @Test
    void holdWindowIsPinnedToTheFixedCheckoutRule() {
        PaymentSettingsDto stored = new PaymentSettingsDto();
        stored.setBankName("Vietcombank");
        stored.setBankCode("970436");
        stored.setBankAccount("0000501454103");
        stored.setAccountHolder("REX CINEMAS");
        stored.setPaymentPrefix("REX");
        stored.setPaymentDescriptionTemplate("{BOOKING_CODE}");
        when(repository.findBankQr()).thenReturn(Optional.of(stored));
        when(repository.isSchemaAvailable()).thenReturn(true);
        when(repository.save(any(PaymentSettingsDto.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentSettingsDto saved = service.save(request(0), "admin@example.com");

        assertThat(saved.getPaymentTimeoutMinutes()).isEqualTo(SeatReservationService.CHECKOUT_HOLD_MINUTES);
        assertThat(SeatReservationService.CHECKOUT_HOLD_MINUTES).isEqualTo(5);
    }

    @Test
    void aDifferentHoldWindowIsRejectedInsteadOfSilentlyIgnored() {
        assertThatThrownBy(() -> service.save(request(45), "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cố định");

        verify(repository, never()).save(any(PaymentSettingsDto.class), anyString());
    }

    private PaymentSettingsUpdateRequest request(int timeoutMinutes) {
        PaymentSettingsUpdateRequest request = new PaymentSettingsUpdateRequest();
        request.setEnabled(true);
        request.setBankName("Vietcombank");
        request.setBankCode("970436");
        request.setBankAccount("0000501454103");
        request.setAccountHolder("REX CINEMAS");
        request.setDefaultAccount(true);
        request.setPaymentTimeoutMinutes(timeoutMinutes);
        request.setPaymentPrefix("REX");
        request.setPaymentDescriptionTemplate("{BOOKING_CODE}");
        return request;
    }
}
