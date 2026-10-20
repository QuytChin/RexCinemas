package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Movie> {
    List<Movie> findByActiveTrueOrderByReleaseDateDesc();
    Optional<Movie> findByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndReleaseDate(String title, LocalDate releaseDate);
    boolean existsByTitleIgnoreCaseAndReleaseDateAndIdNot(String title, LocalDate releaseDate, Long id);
    List<Movie> findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(LocalDate date);
    List<Movie> findByActiveTrueAndReleaseDateGreaterThanOrderByReleaseDateAsc(LocalDate date);
    List<Movie> findByActiveTrueAndStatusOrderByReleaseDateDesc(MovieStatus status);
    List<Movie> findByActiveTrueAndStatusOrderByReleaseDateAsc(MovieStatus status);

    @Query("select m from Movie m where m.active = true and " +
           "(lower(m.title) like lower(concat('%', :q, '%')) or lower(coalesce(m.genre,'')) like lower(concat('%', :q, '%'))) " +
           "order by m.releaseDate desc")
    List<Movie> search(@Param("q") String q);
}
