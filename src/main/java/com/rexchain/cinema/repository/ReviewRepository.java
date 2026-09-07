package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.Review; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface ReviewRepository extends JpaRepository<Review,Long>{
 List<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId);
 Optional<Review> findByMovieIdAndUserId(Long movieId,Long userId);
 @Query("select coalesce(avg(r.rating),0) from Review r where r.movie.id=:movieId") Double averageRating(@Param("movieId") Long movieId);
 long countByMovieId(Long movieId);
}
