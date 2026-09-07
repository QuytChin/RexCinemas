package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    List<Cinema> findByActiveTrueOrderByCityAscNameAsc();
}
