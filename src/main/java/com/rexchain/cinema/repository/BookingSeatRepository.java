package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.BookingSeat;
import com.rexchain.cinema.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    Optional<BookingSeat> findByQrToken(String qrToken);
    Optional<BookingSeat> findByTicketCode(String ticketCode);
    List<BookingSeat> findByBookingId(Long bookingId);
    long countByBookingIdAndCheckedInFalse(Long bookingId);
    boolean existsByShowtimeSeatIdAndBookingStatusAndBookingIdNot(
            Long showtimeSeatId, BookingStatus status, Long bookingId);

    /** Atomic check-in for an individual seat/ticket. */
    @Modifying
    @Query("UPDATE BookingSeat bs SET bs.checkedIn = true, bs.checkedInAt = :now, bs.checkedInBy = :operator " +
           "WHERE bs.id = :id AND bs.checkedIn = false AND bs.checkedInAt IS NULL " +
           "AND bs.booking.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED")
    int atomicCheckInSeat(@Param("id") Long id,
                          @Param("now") LocalDateTime now,
                          @Param("operator") String operator);

    /** Atomic check-in for all seats in a booking. */
    @Modifying
    @Query(value = "UPDATE booking_seats bs " +
           "JOIN bookings b ON b.id = bs.booking_id " +
           "SET bs.checked_in = 1, bs.checked_in_at = :now, bs.checked_in_by = :operator " +
           "WHERE bs.booking_id = :bookingId " +
           "AND (bs.checked_in = 0 OR bs.checked_in_at IS NULL) " +
           "AND b.status = 'CONFIRMED'", nativeQuery = true)
    int atomicCheckInAllSeats(@Param("bookingId") Long bookingId,
                              @Param("now") LocalDateTime now,
                              @Param("operator") String operator);
}
