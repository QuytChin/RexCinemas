package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CinemaRepository extends JpaRepository<Cinema, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cinema> {
    List<Cinema> findByActiveTrueOrderByCityAscNameAsc();
    boolean existsByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);
    boolean existsByNameIgnoreCaseAndAddressIgnoreCaseAndIdNot(String name, String address, Long id);
}
