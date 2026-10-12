package com.rexchain.cinema.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class RepositoryMappingTest {
    @Autowired ShowtimeSeatRepository showtimeSeats;
    @Autowired BookingRepository bookings;

    @Test
    void customQueriesAreParsedAndExecutable() {
        assertThat(showtimeSeats.summarizeAvailability(List.of(-1L))).isEmpty();
        assertThat(bookings.findByBookingCodeForUpdate("missing")).isEmpty();
    }
}
