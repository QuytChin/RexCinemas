package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Event> {
    List<Event> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();
    List<Event> findTop4ByActiveTrueOrderBySortOrderAscCreatedAtDesc();
}
