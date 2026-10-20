package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HomeContentQueryRegressionTest {
    @Autowired EventRepository events;

    @Test
    void homepageEventQueryLimitsRowsInDatabaseAndPreservesOrder() {
        for (int index = 0; index < 6; index++) {
            Event event = new Event();
            event.setTitle("REX_TEST_HOME_EVENT_" + index);
            event.setSortOrder(index / 2);
            event.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(index));
            events.save(event);
        }
        events.flush();

        var result = events.findTop4ByActiveTrueOrderBySortOrderAscCreatedAtDesc();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Event::getTitle).containsExactly(
                "REX_TEST_HOME_EVENT_1", "REX_TEST_HOME_EVENT_0",
                "REX_TEST_HOME_EVENT_3", "REX_TEST_HOME_EVENT_2");
    }
}
