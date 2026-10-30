package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.MovieOccupancyDto;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.MovieRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieOccupancyServiceTest {

    @Mock
    private MovieRepository movies;

    @Mock
    private ShowtimeRepository showtimes;

    @Mock
    private BookingRepository bookings;

    private MovieOccupancyService service;

    @BeforeEach
    void setUp() {
        service = new MovieOccupancyService(movies, showtimes, bookings);
    }

    @Test
    void testOccupancyCalculationAndRecommendation() {
        // 1. Movie
        Movie m1 = new Movie();
        m1.setId(1L);
        m1.setTitle("Dune: Part Two");
        m1.setDurationMinutes(166);
        m1.setAgeRating(AgeRating.T16);

        when(movies.findByActiveTrueOrderByReleaseDateDesc()).thenReturn(List.of(m1));

        // 2. Auditorium (10 rows x 10 cols = 100 capacity)
        Auditorium room = new Auditorium();
        room.setId(10L);
        room.setName("Phòng 1");
        room.setTotalRows(10);
        room.setSeatsPerRow(10);

        // 3. Showtime at 19:30 (Evening / Prime slot)
        Showtime st = new Showtime();
        st.setId(100L);
        st.setMovie(m1);
        st.setAuditorium(room);
        st.setStartTime(LocalDateTime.now().minusDays(1).withHour(19).withMinute(30));

        when(showtimes.findShowtimesForOccupancy(any(), any(), eq(null))).thenReturn(List.of(st));

        // 4. Confirmed Booking with 85 tickets
        Booking b = new Booking();
        b.setId(500L);
        b.setShowtime(st);
        b.setStatus(BookingStatus.CONFIRMED);
        b.setTotalAmount(new BigDecimal("8500000"));

        for (int i = 0; i < 85; i++) {
            BookingSeat bs = new BookingSeat();
            bs.setId((long) (i + 1));
            bs.setSeatLabel("A" + (i + 1));
            bs.setPrice(new BigDecimal("100000"));
            b.getSeats().add(bs);
        }

        when(bookings.findConfirmedBookingsForOccupancy(any(), any(), eq(null))).thenReturn(List.of(b));

        // Execute
        List<MovieOccupancyDto> report = service.getMovieOccupancyReport(null, 7);

        // Verify
        assertNotNull(report);
        assertEquals(1, report.size());
        MovieOccupancyDto dto = report.get(0);

        assertEquals(1L, dto.movieId());
        assertEquals("Dune: Part Two", dto.movieTitle());
        assertEquals(1, dto.showtimeCount());
        assertEquals(100, dto.totalCapacity());
        assertEquals(85, dto.soldTickets());
        assertEquals(85.0, dto.occupancyRate(), 0.01);
        assertEquals(new BigDecimal("8500000"), dto.revenue());
        assertEquals("bg-secondary", dto.badgeClass());
        assertTrue(dto.recommendation().contains("/3"));
        assertTrue(dto.peakHourSlot().contains("17h-21h"));
    }

    @Test
    void testMovieWithZeroShowtimes() {
        Movie m2 = new Movie();
        m2.setId(2L);
        m2.setTitle("Phim Mới Chưa Chiếu");
        m2.setDurationMinutes(120);

        when(movies.findByActiveTrueOrderByReleaseDateDesc()).thenReturn(List.of(m2));
        when(showtimes.findShowtimesForOccupancy(any(), any(), eq(null))).thenReturn(List.of());
        when(bookings.findConfirmedBookingsForOccupancy(any(), any(), eq(null))).thenReturn(List.of());

        List<MovieOccupancyDto> report = service.getMovieOccupancyReport(null, 7);

        assertEquals(1, report.size());
        MovieOccupancyDto dto = report.get(0);
        assertEquals(0, dto.showtimeCount());
        assertEquals(0, dto.totalCapacity());
        assertNull(dto.occupancyRate());
        assertEquals("bg-secondary", dto.badgeClass());
        assertFalse(dto.recommendation().isBlank());
    }
}
