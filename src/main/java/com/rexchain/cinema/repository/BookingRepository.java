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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Booking> {
    interface DashboardSummary {
        Long getOrders();
        Long getConfirmed();
        Long getPending();
        Long getCancelled();
        Long getCheckedIn();
        BigDecimal getRevenue();
    }

    interface DashboardDailyRow {
        Integer getYearValue();
        Integer getMonthValue();
        Integer getDayValue();
        Long getOrders();
        Long getConfirmed();
        Long getPending();
        Long getCancelled();
        BigDecimal getRevenue();
    }

    interface DashboardTicketDay {
        Integer getYearValue();
        Integer getMonthValue();
        Integer getDayValue();
        Long getTickets();
    }

    interface DashboardPaidSummary {
        Long getBookings();
        BigDecimal getRevenue();
    }

    interface DashboardPaidDay {
        Integer getYearValue();
        Integer getMonthValue();
        Integer getDayValue();
        Long getBookings();
        BigDecimal getRevenue();
    }

    interface DashboardLeaderboardRow {
        String getName();
        BigDecimal getRevenue();
        Long getBookings();
    }

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema", "user"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema", "user"})
    org.springframework.data.domain.Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);
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

    @Query("select count(b) as orders, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED then 1 else 0 end), 0) as confirmed, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.PENDING then 1 else 0 end), 0) as pending, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CANCELLED then 1 else 0 end), 0) as cancelled, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED and b.checkedInAt is not null then 1 else 0 end), 0) as checkedIn, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED then b.totalAmount else 0 end), 0) as revenue " +
            "from Booking b " +
            "join b.showtime st " +
            "join st.movie m " +
            "join st.auditorium a " +
            "join a.cinema c " +
            "where b.createdAt >= :from and b.createdAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) " +
            "and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod)")
    DashboardSummary summarizeDashboardBookings(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("cinemaId") Long cinemaId,
                                                @Param("movieId") Long movieId,
                                                @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select count(b) as bookings, coalesce(sum(b.totalAmount), 0) as revenue " +
            "from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED " +
            "and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod)")
    DashboardPaidSummary summarizeDashboardPaid(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("cinemaId") Long cinemaId,
                                                @Param("movieId") Long movieId,
                                                @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select year(b.paidAt) as yearValue, month(b.paidAt) as monthValue, " +
            "day(b.paidAt) as dayValue, count(b) as bookings, " +
            "coalesce(sum(b.totalAmount), 0) as revenue " +
            "from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED " +
            "and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "group by year(b.paidAt), month(b.paidAt), day(b.paidAt)")
    List<DashboardPaidDay> summarizeDashboardPaidByDay(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to,
                                                       @Param("cinemaId") Long cinemaId,
                                                       @Param("movieId") Long movieId,
                                                       @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select year(b.createdAt) as yearValue, month(b.createdAt) as monthValue, day(b.createdAt) as dayValue, " +
            "count(b) as orders, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED then 1 else 0 end), 0) as confirmed, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.PENDING then 1 else 0 end), 0) as pending, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CANCELLED then 1 else 0 end), 0) as cancelled, " +
            "coalesce(sum(case when b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED then b.totalAmount else 0 end), 0) as revenue " +
            "from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.createdAt >= :from and b.createdAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "group by year(b.createdAt), month(b.createdAt), day(b.createdAt)")
    List<DashboardDailyRow> summarizeDashboardBookingsByDay(@Param("from") LocalDateTime from,
                                                            @Param("to") LocalDateTime to,
                                                            @Param("cinemaId") Long cinemaId,
                                                            @Param("movieId") Long movieId,
                                                            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select year(b.paidAt) as yearValue, month(b.paidAt) as monthValue, day(b.paidAt) as dayValue, count(bs) as tickets " +
            "from Booking b join b.seats bs join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "group by year(b.paidAt), month(b.paidAt), day(b.paidAt)")
    List<DashboardTicketDay> countDashboardTicketsByDay(@Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to,
                                                        @Param("cinemaId") Long cinemaId,
                                                        @Param("movieId") Long movieId,
                                                        @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select count(bs) from BookingSeat bs join bs.booking b join b.showtime st join st.movie m " +
            "join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod)")
    long countDashboardTickets(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("cinemaId") Long cinemaId,
                               @Param("movieId") Long movieId,
                               @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("select m.title as name, coalesce(sum(b.totalAmount), 0) as revenue, count(b) as bookings " +
            "from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "group by m.id, m.title order by sum(b.totalAmount) desc")
    List<DashboardLeaderboardRow> findDashboardTopMovies(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to,
                                                         @Param("cinemaId") Long cinemaId,
                                                         @Param("movieId") Long movieId,
                                                         @Param("paymentMethod") PaymentMethod paymentMethod,
                                                         Pageable pageable);

    @Query("select c.name as name, coalesce(sum(b.totalAmount), 0) as revenue, count(b) as bookings " +
            "from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED and b.paidAt >= :from and b.paidAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) " +
            "group by c.id, c.name order by sum(b.totalAmount) desc")
    List<DashboardLeaderboardRow> findDashboardTopCinemas(@Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to,
                                                          @Param("cinemaId") Long cinemaId,
                                                          @Param("movieId") Long movieId,
                                                          @Param("paymentMethod") PaymentMethod paymentMethod,
                                                          Pageable pageable);

    @EntityGraph(attributePaths = {"user", "showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema"})
    @Query("select b from Booking b join b.showtime st join st.movie m join st.auditorium a join a.cinema c " +
            "where b.createdAt >= :from and b.createdAt < :to " +
            "and (:cinemaId is null or c.id = :cinemaId) and (:movieId is null or m.id = :movieId) " +
            "and (:paymentMethod is null or b.paymentMethod = :paymentMethod) order by b.createdAt desc")
    List<Booking> findRecentDashboardBookings(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              @Param("cinemaId") Long cinemaId,
                                              @Param("movieId") Long movieId,
                                              @Param("paymentMethod") PaymentMethod paymentMethod,
                                              Pageable pageable);

    @EntityGraph(attributePaths = {
            "user", "showtime", "showtime.movie", "showtime.auditorium", "showtime.auditorium.cinema"
    })
    List<Booking> findByIdIn(Collection<Long> ids);


    @Query("select b.id from Booking b where b.status = :status and b.expiresAt < :before " +
            "and (:afterId is null or b.id > :afterId) order by b.id")
    List<Long> findExpiredBookingIds(@Param("status") BookingStatus status,
                                     @Param("before") LocalDateTime before,
                                     @Param("afterId") Long afterId,
                                     Pageable pageable);

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

    @Query("select distinct b from Booking b " +
           "join fetch b.showtime st " +
           "join fetch st.movie m " +
           "join fetch st.auditorium a " +
           "join fetch a.cinema c " +
           "left join fetch b.seats bs " +
           "where b.status = com.rexchain.cinema.entity.BookingStatus.CONFIRMED " +
           "and st.startTime >= :from and st.startTime < :to " +
           "and (:cinemaId is null or c.id = :cinemaId)")
    List<Booking> findConfirmedBookingsForOccupancy(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("cinemaId") Long cinemaId);
}
