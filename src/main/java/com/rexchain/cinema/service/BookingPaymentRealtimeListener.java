package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.event.BookingConfirmedEvent;
import com.rexchain.cinema.repository.BookingRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Notifies only the booking owner after a bank QR payment has committed. */
@Component
public class BookingPaymentRealtimeListener {
    private static final Logger log = LoggerFactory.getLogger(BookingPaymentRealtimeListener.class);
    private static final String PAYMENT_STATUS_DESTINATION = "/queue/payment-status";

    private final BookingRepository bookings;
    private final SimpMessagingTemplate messaging;

    public BookingPaymentRealtimeListener(BookingRepository bookings, SimpMessagingTemplate messaging) {
        this.bookings = bookings;
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void notifyBookingOwner(BookingConfirmedEvent event) {
        try {
            Booking booking = bookings.findById(event.bookingId()).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED
                    || booking.getPaymentMethod() != PaymentMethod.BANK_QR
                    || booking.getUser() == null || booking.getUser().getEmail() == null
                    || booking.getUser().getEmail().isBlank()) return;

            messaging.convertAndSendToUser(booking.getUser().getEmail(), PAYMENT_STATUS_DESTINATION,
                    Map.of("bookingCode", booking.getBookingCode(),
                            "status", booking.getStatus().name(),
                            "ticketUrl", "/bookings/" + booking.getId()));
        } catch (RuntimeException exception) {
            // Settlement is already committed; websocket delivery must never affect it.
            log.warn("Could not publish realtime payment status for booking id {}", event.bookingId());
        }
    }
}
