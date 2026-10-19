package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.SeatHoldSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldSessionRepository extends JpaRepository<SeatHoldSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SeatHoldSession s where s.userId = :userId and s.showtime.id = :showtimeId")
    Optional<SeatHoldSession> findByUserIdAndShowtimeIdForUpdate(@Param("userId") Long userId,
                                                                  @Param("showtimeId") Long showtimeId);
}
