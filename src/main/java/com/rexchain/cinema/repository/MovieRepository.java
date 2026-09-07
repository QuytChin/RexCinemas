package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByActiveTrueOrderByReleaseDateDesc();
    Optional<Movie> findByTitleIgnoreCase(String title);
    List<Movie> findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(LocalDate date);
    List<Movie> findByActiveTrueAndReleaseDateGreaterThanOrderByReleaseDateAsc(LocalDate date);

    @Query("select m from Movie m where m.active = true and " +
           "(lower(m.title) like lower(concat('%', :q, '%')) or lower(coalesce(m.genre,'')) like lower(concat('%', :q, '%'))) " +
           "order by m.releaseDate desc")
    List<Movie> search(@Param("q") String q);
}
