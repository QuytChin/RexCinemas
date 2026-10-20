package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Booking> {
    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema", "user"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);
    Optional<Booking> findByBookingCode(String bookingCode);
    Optional<Booking> findByQrToken(String qrToken);
    boolean existsByPaymentReference(String paymentReference);
    Optional<Booking> findByPaymentMethodAndPaymentReference(PaymentMethod paymentMethod, String paymentReference);
    List<Booking> findByPaymentMethodAndStatus(PaymentMethod paymentMethod, BookingStatus status);
    Optional<Booking> findBySepayTransactionId(String sepayTransactionId);
    boolean existsByUserIdAndShowtimeIdAndStatusAndExpiresAtBetween(Long userId, Long showtimeId,
            BookingStatus status, LocalDateTime from, LocalDateTime to);
    boolean existsByUserIdAndShowtimeIdAndStatusInAndCreatedAtBetween(Long userId, Long showtimeId,
            Collection<BookingStatus> statuses, LocalDateTime from, LocalDateTime to);
    boolean existsByShowtimeIdAndStatusNot(Long showtimeId, BookingStatus status);
    boolean existsByShowtimeIdAndStatusIn(Long showtimeId, Collection<BookingStatus> statuses);
    List<Booking> findTop20ByCheckedInAtIsNotNullOrderByCheckedInAtDesc();

    @Query("select coalesce(sum(b.totalAmount),0) from Booking b where b.status = :status")
    BigDecimal totalRevenue(@Param("status") BookingStatus status);


    @Query("select b from Booking b " +
            "join fetch b.user " +
            "join fetch b.showtime st " +
            "join fetch st.movie " +
            "join fetch st.auditorium a " +
            "join fetch a.cinema " +
            "left join fetch b.seats " +
            "order by b.createdAt desc")
    List<Booking> findAllDetailed();

    @Query("select distinct b from Booking b left join fetch b.seats where b.id in :ids")
    List<Booking> loadSeatsForPage(@Param("ids") Collection<Long> ids);

    @Query("select b.id from Booking b " +
            "join b.showtime st " +
            "join st.movie m " +
            "join st.auditorium a " +
            "join a.cinema c " +
            "where b.createdAt >= :from and b.createdAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) " +
            "and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "order by b.createdAt desc")
    List<Long> findDashboardBookingIds(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("cinemaId") Long cinemaId,
                                       @Param("movieId") Long movieId,
                                       @Param("paymentMethod") PaymentMethod paymentMethod);

    @EntityGraph(attributePaths = {
            "user", "showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema"
    })
    List<Booking> findByIdIn(Collection<Long> ids);


    /** Fix 1: Find PENDING bookings that have passed their expiry time. */
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime before);

    /** Fix 3: Pessimistic lock on booking — prevents duplicate payment webhook processing. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingCode = :code")
    Optional<Booking> findByBookingCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Fix 5: Atomic check-in — prevents double scan at two gates. */
    @Modifying
    @Query("UPDATE Booking b SET b.checkedInAt = :now, b.checkedInBy = :operator " +
           "WHERE b.id = :id AND b.checkedInAt IS NULL " +
           "AND b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED")
    int atomicCheckIn(@Param("id") Long id,
                      @Param("now") LocalDateTime now,
                      @Param("operator") String operator);
}
