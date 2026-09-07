package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.Auditorium; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface AuditoriumRepository extends JpaRepository<Auditorium,Long>{List<Auditorium> findByCinemaIdOrderByNameAsc(Long cinemaId);}
