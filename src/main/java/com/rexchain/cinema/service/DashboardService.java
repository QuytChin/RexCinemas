package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {
    private final BookingRepository bookings;
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final UserRepository users;

    public DashboardService(BookingRepository bookings, MovieRepository movies,
                            CinemaRepository cinemas, UserRepository users) {
        this.bookings = bookings;
        this.movies = movies;
        this.cinemas = cinemas;
        this.users = users;
    }

    public record LeaderboardItem(String name, BigDecimal revenue, long bookings) {}
    public record DailyMetric(String label, long orders, long confirmed, long pending, long cancelled,
                              long tickets, BigDecimal revenue, BigDecimal aov) {}

    @Transactional(readOnly = true)
    public void populate(Model model, LocalDate fromDate, LocalDate toDate,
                         Long cinemaId, Long movieId, PaymentMethod paymentMethod) {
        LocalDate today = LocalDate.now();
        LocalDate from = fromDate == null ? today.minusDays(29) : fromDate;
        LocalDate to = toDate == null ? today : toDate;
        if (to.isBefore(from)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<Booking> filtered = bookings.findAllDetailed().stream()
                .filter(b -> !b.getCreatedAt().isBefore(fromTime) && b.getCreatedAt().isBefore(toExclusive))
                .filter(b -> cinemaId == null || Objects.equals(b.getShowtime().getAuditorium().getCinema().getId(), cinemaId))
                .filter(b -> movieId == null || Objects.equals(b.getShowtime().getMovie().getId(), movieId))
                .filter(b -> paymentMethod == null || b.getPaymentMethod() == paymentMethod)
                .toList();

        long confirmed = filtered.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long pending = filtered.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long cancelled = filtered.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long checkedIn = filtered.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.isCheckedIn()).count();
        long tickets = filtered.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .mapToLong(b -> b.getSeats().size()).sum();
        BigDecimal revenue = filtered.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal aov = confirmed == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(confirmed), 0, RoundingMode.HALF_UP);

        Map<LocalDate, List<Booking>> byDay = new LinkedHashMap<>();
        long dayCount = Math.min(62, Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).toDays());
        LocalDate chartStart = dayCount >= 62 ? to.minusDays(61) : from;
        for (LocalDate d = chartStart; !d.isAfter(to); d = d.plusDays(1)) byDay.put(d, new ArrayList<>());
        for (Booking b : filtered) {
            LocalDate d = b.getCreatedAt().toLocalDate();
            if (byDay.containsKey(d)) byDay.get(d).add(b);
        }

        List<DailyMetric> daily = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM");
        for (Map.Entry<LocalDate, List<Booking>> entry : byDay.entrySet()) {
            List<Booking> day = entry.getValue();
            long dc = day.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
            long dp = day.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
            long dx = day.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
            long dt = day.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).mapToLong(b -> b.getSeats().size()).sum();
            BigDecimal dr = day.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal daov = dc == 0 ? BigDecimal.ZERO : dr.divide(BigDecimal.valueOf(dc), 0, RoundingMode.HALF_UP);
            daily.add(new DailyMetric(entry.getKey().format(df), day.size(), dc, dp, dx, dt, dr, daov));
        }

        Map<String, BigDecimal> movieRevenue = new HashMap<>();
        Map<String, Long> movieBookings = new HashMap<>();
        Map<String, BigDecimal> cinemaRevenue = new HashMap<>();
        Map<String, Long> cinemaBookings = new HashMap<>();
        filtered.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).forEach(b -> {
            String movieName = b.getShowtime().getMovie().getTitle();
            String cinemaName = b.getShowtime().getAuditorium().getCinema().getName();
            movieRevenue.merge(movieName, b.getTotalAmount(), BigDecimal::add);
            movieBookings.merge(movieName, 1L, Long::sum);
            cinemaRevenue.merge(cinemaName, b.getTotalAmount(), BigDecimal::add);
            cinemaBookings.merge(cinemaName, 1L, Long::sum);
        });

        List<LeaderboardItem> topMovies = toLeaderboard(movieRevenue, movieBookings);
        List<LeaderboardItem> topCinemas = toLeaderboard(cinemaRevenue, cinemaBookings);

        model.addAttribute("movieCount", movies.count());
        model.addAttribute("cinemaCount", cinemas.count());
        model.addAttribute("bookingCount", filtered.size());
        model.addAttribute("userCount", users.count());
        model.addAttribute("revenue", revenue);
        model.addAttribute("confirmedCount", confirmed);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("checkedInCount", checkedIn);
        model.addAttribute("ticketCount", tickets);
        model.addAttribute("aov", aov);
        model.addAttribute("dailyMetrics", daily);
        model.addAttribute("revenueLabels", daily.stream().map(DailyMetric::label).toList());
        model.addAttribute("revenueData", daily.stream().map(DailyMetric::revenue).toList());
        model.addAttribute("bookingData", daily.stream().map(DailyMetric::confirmed).toList());
        model.addAttribute("topMovies", topMovies.stream().limit(5).toList());
        model.addAttribute("topCinemas", topCinemas.stream().limit(5).toList());
        model.addAttribute("recentBookings", filtered.stream().sorted(Comparator.comparing(Booking::getCreatedAt).reversed()).limit(8).toList());
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        model.addAttribute("movies", movies.findByActiveTrueOrderByReleaseDateDesc());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        model.addAttribute("filterCinemaId", cinemaId);
        model.addAttribute("filterMovieId", movieId);
        model.addAttribute("filterPaymentMethod", paymentMethod);
    }

    private List<LeaderboardItem> toLeaderboard(Map<String, BigDecimal> revenue, Map<String, Long> count) {
        return revenue.entrySet().stream()
                .map(e -> new LeaderboardItem(e.getKey(), e.getValue(), count.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparing(LeaderboardItem::revenue).reversed())
                .toList();
    }
}
