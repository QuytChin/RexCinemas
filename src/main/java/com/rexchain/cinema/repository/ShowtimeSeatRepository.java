package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.SeatStatus;
import com.rexchain.cinema.entity.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(Long showtimeId);
    List<ShowtimeSeat> findByIdIn(List<Long> ids);
    List<ShowtimeSeat> findByStatusAndHoldUntilBefore(SeatStatus status, LocalDateTime now);
    long countByShowtimeIdAndStatus(Long showtimeId, SeatStatus status);
    long countByShowtimeId(Long showtimeId);

    @Query("select ss from ShowtimeSeat ss join fetch ss.showtime join fetch ss.seat")
    List<ShowtimeSeat> findAllWithShowtimeAndSeat();
}
