package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DashboardAggregationQueryTest {

    @Autowired
    private BookingRepository bookings;

    @Test
    void dashboardAggregationsReturnEmptyResultsWithoutLoadingBookingEntities() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);

        BookingRepository.DashboardSummary summary = bookings.summarizeDashboardBookings(
                from, to, null, null, null);

        assertThat(summary.getOrders()).isZero();
        assertThat(summary.getConfirmed()).isZero();
        assertThat(summary.getRevenue()).isZero();
        BookingRepository.DashboardPaidSummary paid = bookings.summarizeDashboardPaid(from, to, null, null, null);
        assertThat(paid.getBookings()).isZero();
        assertThat(paid.getRevenue()).isZero();
        assertThat(bookings.summarizeDashboardBookingsByDay(from, to, null, null, null)).isEmpty();
        assertThat(bookings.summarizeDashboardPaidByDay(from, to, null, null, null)).isEmpty();
        assertThat(bookings.countDashboardTicketsByDay(from, to, null, null, null)).isEmpty();
        assertThat(bookings.countDashboardTickets(from, to, null, null, null)).isZero();
        assertThat(bookings.findDashboardTopMovies(from, to, null, null, null, PageRequest.of(0, 5))).isEmpty();
        assertThat(bookings.findDashboardTopCinemas(from, to, null, null, null, PageRequest.of(0, 5))).isEmpty();
        assertThat(bookings.findRecentDashboardBookings(from, to, null, null, PaymentMethod.BANK_QR,
                PageRequest.of(0, 8))).isEmpty();
    }
}
