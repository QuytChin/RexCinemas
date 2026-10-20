package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.BankTransferPaymentService;
import com.rexchain.cinema.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerExpiryTest {
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock BankTransferPaymentService transfer;
    @Mock BookingService service;
    @Mock Authentication authentication;

    @Test
    void expiredBookingNeverFetchesOrRendersVietQr() {
        User user = new User(); user.setId(7L);
        Booking booking = new Booking(); booking.setId(11L); booking.setBookingCode("REX_TEST_EXPIRED");
        booking.setUser(user); booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setStatus(BookingStatus.EXPIRED);
        when(authentication.getName()).thenReturn("test@example.invalid");
        when(users.findByEmailIgnoreCase("test@example.invalid")).thenReturn(Optional.of(user));
        when(bookings.findById(11L)).thenReturn(Optional.of(booking));
        when(service.expireIfDue("REX_TEST_EXPIRED")).thenReturn(booking);

        String view = new PaymentController(bookings, users, transfer, service)
                .bankQr(11L, authentication, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/bookings/11");
        verifyNoInteractions(transfer);
    }
}
