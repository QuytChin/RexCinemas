package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);
    Optional<Booking> findByBookingCode(String bookingCode);
    Optional<Booking> findByQrToken(String qrToken);
    boolean existsByShowtimeIdAndStatusNot(Long showtimeId, BookingStatus status);
    List<Booking> findTop20ByCheckedInAtIsNotNullOrderByCheckedInAtDesc();

    @Query("select coalesce(sum(b.totalAmount),0) from Booking b where b.status = :status")
    BigDecimal totalRevenue(@Param("status") BookingStatus status);

    @Query("select distinct b from Booking b join fetch b.user join fetch b.showtime st join fetch st.movie " +
           "join fetch st.auditorium a join fetch a.cinema left join fetch b.combos order by b.createdAt desc")
    List<Booking> findAllDetailed();

    /** Fix 1: Find PENDING bookings that have passed their expiry time. */
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime before);

    /** Fix 3: Pessimistic lock on booking — prevents duplicate payment webhook processing. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingCode = :code")
    Optional<Booking> findByBookingCodeForUpdate(@Param("code") String code);

    /** Fix 5: Atomic check-in — prevents double scan at two gates. */
    @Modifying
    @Query("UPDATE Booking b SET b.checkedInAt = :now, b.checkedInBy = :operator " +
           "WHERE b.id = :id AND b.checkedInAt IS NULL")
    int atomicCheckIn(@Param("id") Long id,
                      @Param("now") LocalDateTime now,
                      @Param("operator") String operator);
}
