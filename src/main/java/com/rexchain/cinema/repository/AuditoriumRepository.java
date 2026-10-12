package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.Auditorium;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface AuditoriumRepository extends JpaRepository<Auditorium,Long>{
    List<Auditorium> findByCinemaIdOrderByNameAsc(Long cinemaId);
    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auditorium a where a.id = :id")
    Optional<Auditorium> findByIdForUpdate(@Param("id") Long id);
}
