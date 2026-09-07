package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    List<MediaItem> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();
}
