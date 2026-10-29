package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.SeatStatus;
import com.rexchain.cinema.entity.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(Long showtimeId);
    List<ShowtimeSeat> findByIdIn(List<Long> ids);
    List<ShowtimeSeat> findByStatusAndHoldUntilBefore(SeatStatus status, LocalDateTime now);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);
    long countByShowtimeId(Long showtimeId);
    boolean existsByShowtimeIdAndStatusAndHoldUntilAfter(Long showtimeId, SeatStatus status, LocalDateTime now);

    @Query("select ss.showtime.id, count(ss), " +
           "sum(case when ss.status = com.rexchain.cinema.entity.SeatStatus.AVAILABLE then 1 else 0 end) " +
           "from ShowtimeSeat ss where ss.showtime.id in :showtimeIds group by ss.showtime.id")
    List<Object[]> summarizeAvailability(@Param("showtimeIds") List<Long> showtimeIds);

    @Query("select ss.showtime.id, count(ss), " +
           "sum(case when ss.status = com.rexchain.cinema.entity.SeatStatus.BOOKED then 1 else 0 end) " +
           "from ShowtimeSeat ss where ss.showtime.id in :showtimeIds group by ss.showtime.id")
    List<Object[]> summarizeOccupancy(@Param("showtimeIds") Collection<Long> showtimeIds);

    @Query("select ss from ShowtimeSeat ss join fetch ss.showtime join fetch ss.seat")
    List<ShowtimeSeat> findAllWithShowtimeAndSeat();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.showtime.id = :showtimeId " +
           "ORDER BY ss.seat.seatRow, ss.seat.seatNumber")
    List<ShowtimeSeat> findByShowtimeIdForUpdate(@Param("showtimeId") Long showtimeId);

    /** Fix 2: Pessimistic lock — prevents two users from holding the same seat concurrently. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.id IN :ids ORDER BY ss.id")
    List<ShowtimeSeat> findByIdInForUpdate(@Param("ids") List<Long> ids);
}
