package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime before);
    Optional<Booking> findByBookingCode(String bookingCode);
    Optional<Booking> findByQrToken(String qrToken);
    boolean existsByShowtimeIdAndStatusNot(Long showtimeId, BookingStatus status);
    List<Booking> findTop20ByCheckedInAtIsNotNullOrderByCheckedInAtDesc();

    @Query("select coalesce(sum(b.totalAmount),0) from Booking b where b.status = :status")
    BigDecimal totalRevenue(@Param("status") BookingStatus status);

    @Query("select distinct b from Booking b join fetch b.user join fetch b.showtime st join fetch st.movie " +
           "join fetch st.auditorium a join fetch a.cinema left join fetch b.combos order by b.createdAt desc")
    List<Booking> findAllDetailed();
}
