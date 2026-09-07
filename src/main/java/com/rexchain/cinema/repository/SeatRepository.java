package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.Seat; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface SeatRepository extends JpaRepository<Seat,Long>{List<Seat> findByAuditoriumIdOrderBySeatRowAscSeatNumberAsc(Long auditoriumId);}
