package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CinemaRepository extends JpaRepository<Cinema, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cinema> {
    List<Cinema> findByActiveTrueOrderByCityAscNameAsc();
    boolean existsByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);
    boolean existsByNameIgnoreCaseAndAddressIgnoreCaseAndIdNot(String name, String address, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cinema c where c.id = :id")
    Optional<Cinema> findByIdForUpdate(@Param("id") Long id);
}
