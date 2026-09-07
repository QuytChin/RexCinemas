package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();
}
