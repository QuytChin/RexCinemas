package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.MovieOccupancyDto;
import com.rexchain.cinema.entity.Auditorium;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.MovieRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MovieOccupancyService {

    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final BookingRepository bookings;

    public MovieOccupancyService(MovieRepository movies,
                                 ShowtimeRepository showtimes,
                                 BookingRepository bookings) {
        this.movies = movies;
        this.showtimes = showtimes;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<MovieOccupancyDto> getMovieOccupancyReport(Long cinemaId, int days) {
        int windowDays = days > 0 ? days : 7;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = now.minusDays(windowDays);
        LocalDateTime toTime = now;

        List<Movie> activeMovies = movies.findByActiveTrueOrderByReleaseDateDesc();
        if (activeMovies.isEmpty()) {
            activeMovies = movies.findAll();
        }

        List<Showtime> periodShowtimes = showtimes.findShowtimesForOccupancy(fromTime, toTime, cinemaId);
        List<Booking> periodBookings = bookings.findConfirmedBookingsForOccupancy(fromTime, toTime, cinemaId);

        // Map showtimes by movie ID
        Map<Long, List<Showtime>> showtimesByMovie = new HashMap<>();
        for (Showtime st : periodShowtimes) {
            if (st.getMovie() != null && st.getMovie().getId() != null) {
                showtimesByMovie.computeIfAbsent(st.getMovie().getId(), k -> new ArrayList<>()).add(st);
            }
        }

        // Map bookings by movie ID
        Map<Long, List<Booking>> bookingsByMovie = new HashMap<>();
        for (Booking b : periodBookings) {
            if (b.getShowtime() != null && b.getShowtime().getMovie() != null) {
                bookingsByMovie.computeIfAbsent(b.getShowtime().getMovie().getId(), k -> new ArrayList<>()).add(b);
            }
        }

        List<MovieOccupancyDto> result = new ArrayList<>();

        for (Movie m : activeMovies) {
            Long movieId = m.getId();
            List<Showtime> mShowtimes = showtimesByMovie.getOrDefault(movieId, Collections.emptyList());
            List<Booking> mBookings = bookingsByMovie.getOrDefault(movieId, Collections.emptyList());

            long showtimeCount = mShowtimes.size();
            long totalCapacity = 0;
            for (Showtime st : mShowtimes) {
                Auditorium a = st.getAuditorium();
                if (a != null) {
                    totalCapacity += (long) a.getTotalRows() * a.getSeatsPerRow();
                }
            }

            long soldTickets = 0;
            BigDecimal revenue = BigDecimal.ZERO;

            Map<String, Long> slotTicketMap = new LinkedHashMap<>();
            slotTicketMap.put("S\u00e1ng (08h-12h)", 0L);
            slotTicketMap.put("Chi\u1ec1u (12h-17h)", 0L);
            slotTicketMap.put("T\u1ed1i (17h-21h)", 0L);
            slotTicketMap.put("Khuya (sau 21h)", 0L);

            for (Booking b : mBookings) {
                int ticketCount = (b.getSeats() != null && !b.getSeats().isEmpty())
                        ? b.getSeats().size() : 1;
                soldTickets += ticketCount;
                if (b.getTotalAmount() != null) {
                    revenue = revenue.add(b.getTotalAmount());
                }

                if (b.getShowtime() != null && b.getShowtime().getStartTime() != null) {
                    int hour = b.getShowtime().getStartTime().getHour();
                    String slotKey;
                    if (hour < 12) slotKey = "S\u00e1ng (08h-12h)";
                    else if (hour < 17) slotKey = "Chi\u1ec1u (12h-17h)";
                    else if (hour < 21) slotKey = "T\u1ed1i (17h-21h)";
                    else slotKey = "Khuya (sau 21h)";
                    slotTicketMap.put(slotKey, slotTicketMap.get(slotKey) + ticketCount);
                }
            }

            Double occupancyRate = null;
            if (totalCapacity > 0) {
                occupancyRate = Math.min(100.0, Math.round((soldTickets * 1000.0) / totalCapacity) / 10.0);
            }

            String peakHourSlot = "Ch\u01b0a c\u00f3";
            long maxSlotTickets = 0;
            for (Map.Entry<String, Long> entry : slotTicketMap.entrySet()) {
                if (entry.getValue() > maxSlotTickets) {
                    maxSlotTickets = entry.getValue();
                    peakHourSlot = entry.getKey();
                }
            }
            if (maxSlotTickets == 0) {
                peakHourSlot = "—";
            }

            String recommendation;
            String badgeClass;
            if (showtimeCount == 0 || totalCapacity == 0) {
                recommendation = "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u su\u1ea5t \u0111\u00e3 di\u1ec5n ra trong 7 ng\u00e0y";
                badgeClass = "bg-secondary";
            } else if (showtimeCount < 3) {
                recommendation = "Ch\u01b0a \u0111\u1ee7 m\u1eabu: " + showtimeCount + "/3 su\u1ea5t \u0111\u00e3 di\u1ec5n ra";
                badgeClass = "bg-secondary";
            } else if (occupancyRate >= 70.0) {
                recommendation = "Nhu c\u1ea7u quan s\u00e1t \u0111\u01b0\u1ee3c cao; c\u00e2n nh\u1eafc th\u00eam ph\u00f2ng l\u1edbn cho khung " + peakHourSlot;
                badgeClass = "bg-success";
            } else if (occupancyRate >= 40.0) {
                recommendation = "Nhu c\u1ea7u quan s\u00e1t \u0111\u01b0\u1ee3c \u1edf m\u1ee9c trung b\u00ecnh; duy tr\u00ec quy m\u00f4 ph\u00f2ng hi\u1ec7n t\u1ea1i";
                badgeClass = "bg-warning text-dark";
            } else {
                recommendation = "T\u1ef7 l\u1ec7 b\u00e1n th\u1ea5p trong m\u1eabu \u0111\u00e3 di\u1ec5n ra; c\u00e2n nh\u1eafc r\u00e0 so\u00e1t s\u1ed1 su\u1ea5t v\u00e0 quy m\u00f4 ph\u00f2ng";
                badgeClass = "bg-secondary";
            }

            result.add(new MovieOccupancyDto(
                    movieId,
                    m.getTitle(),
                    m.getPosterUrl(),
                    m.getDurationMinutes(),
                    m.getAgeRating() != null ? m.getAgeRating().name() : "P",
                    showtimeCount,
                    totalCapacity,
                    soldTickets,
                    revenue,
                    occupancyRate,
                    peakHourSlot,
                    recommendation,
                    badgeClass
            ));
        }

        result.sort((a, b) -> {
            int comp = Double.compare(b.occupancyRate() == null ? -1 : b.occupancyRate(),
                    a.occupancyRate() == null ? -1 : a.occupancyRate());
            if (comp != 0) return comp;
            return b.revenue().compareTo(a.revenue());
        });

        return result;
    }

    @Transactional(readOnly = true)
    public MovieOccupancyDto getMovieOccupancyForMovie(Long movieId, Long cinemaId, int days) {
        return getMovieOccupancyReport(cinemaId, days).stream()
                .filter(dto -> dto.movieId().equals(movieId))
                .findFirst()
                .orElse(null);
    }
}
