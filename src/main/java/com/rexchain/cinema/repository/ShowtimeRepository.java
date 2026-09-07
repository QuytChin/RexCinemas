package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieIdAndActiveTrueAndStartTimeAfterOrderByStartTimeAsc(Long movieId, LocalDateTime after);
    List<Showtime> findByActiveTrueAndStartTimeAfterOrderByStartTimeAsc(LocalDateTime after);
    long countByActiveTrueAndStartTimeBetween(LocalDateTime from, LocalDateTime to);
    List<Showtime> findByAuditoriumIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
            Long auditoriumId, LocalDateTime from, LocalDateTime to);

    @Query("select s from Showtime s join fetch s.movie m join fetch s.auditorium a join fetch a.cinema c " +
           "where s.active=true and m.active=true and c.active=true " +
           "and s.startTime >= :from and s.startTime < :to " +
           "and (:cinemaId is null or c.id = :cinemaId) " +
           "and (:movieId is null or m.id = :movieId) " +
           "order by c.name, m.title, s.startTime")
    List<Showtime> findSchedule(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                @Param("cinemaId") Long cinemaId,
                                @Param("movieId") Long movieId);
}
