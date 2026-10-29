package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.data.domain.PageRequest;

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

        BookingRepository.DashboardSummary summary = bookings.summarizeDashboardBookings(
                fromTime, toExclusive, cinemaId, movieId, paymentMethod);
        // Order counts belong to the booking creation day. Cash revenue and sold
        // tickets belong to the payment settlement day (Booking.paidAt).
        BookingRepository.DashboardPaidSummary paid = bookings.summarizeDashboardPaid(
                fromTime, toExclusive, cinemaId, movieId, paymentMethod);
        long bookingCount = summary.getOrders();
        long confirmed = summary.getConfirmed();
        long pending = summary.getPending();
        long cancelled = summary.getCancelled();
        long checkedIn = summary.getCheckedIn();
        BigDecimal revenue = paid.getRevenue() == null ? BigDecimal.ZERO : paid.getRevenue();
        long paidBookings = paid.getBookings();
        BigDecimal aov = paidBookings == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paidBookings), 0, RoundingMode.HALF_UP);

        long dayCount = Math.min(62, Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).toDays());
        LocalDate chartStart = dayCount >= 62 ? to.minusDays(61) : from;
        LocalDateTime chartFrom = chartStart.atStartOfDay();
        List<BookingRepository.DashboardDailyRow> dailyRows = bookings.summarizeDashboardBookingsByDay(
                chartFrom, toExclusive, cinemaId, movieId, paymentMethod);
        Map<LocalDate, BookingRepository.DashboardDailyRow> metricsByDay = new HashMap<>();
        for (BookingRepository.DashboardDailyRow row : dailyRows) {
            metricsByDay.put(LocalDate.of(row.getYearValue(), row.getMonthValue(), row.getDayValue()), row);
        }
        Map<LocalDate, BookingRepository.DashboardPaidDay> revenueByDay = new HashMap<>();
        for (BookingRepository.DashboardPaidDay row : bookings.summarizeDashboardPaidByDay(
                chartFrom, toExclusive, cinemaId, movieId, paymentMethod)) {
            revenueByDay.put(LocalDate.of(row.getYearValue(), row.getMonthValue(), row.getDayValue()), row);
        }
        Map<LocalDate, Long> ticketsByDay = new HashMap<>();
        for (BookingRepository.DashboardTicketDay row : bookings.countDashboardTicketsByDay(
                chartFrom, toExclusive, cinemaId, movieId, paymentMethod)) {
            ticketsByDay.put(LocalDate.of(row.getYearValue(), row.getMonthValue(), row.getDayValue()), row.getTickets());
        }

        List<DailyMetric> daily = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM");
        for (LocalDate date = chartStart; !date.isAfter(to); date = date.plusDays(1)) {
            BookingRepository.DashboardDailyRow row = metricsByDay.get(date);
            long orders = row == null ? 0 : row.getOrders();
            long dc = row == null ? 0 : row.getConfirmed();
            long dp = row == null ? 0 : row.getPending();
            long dx = row == null ? 0 : row.getCancelled();
            long dt = ticketsByDay.getOrDefault(date, 0L);
            BookingRepository.DashboardPaidDay paidDay = revenueByDay.get(date);
            BigDecimal dr = paidDay == null || paidDay.getRevenue() == null ? BigDecimal.ZERO : paidDay.getRevenue();
            long dayPayments = paidDay == null ? 0 : paidDay.getBookings();
            BigDecimal daov = dayPayments == 0 ? BigDecimal.ZERO
                    : dr.divide(BigDecimal.valueOf(dayPayments), 0, RoundingMode.HALF_UP);
            daily.add(new DailyMetric(date.format(df), orders, dc, dp, dx, dt, dr, daov));
        }

        PageRequest topFive = PageRequest.of(0, 5);
        List<LeaderboardItem> topMovies = bookings.findDashboardTopMovies(
                        fromTime, toExclusive, cinemaId, movieId, paymentMethod, topFive)
                .stream().map(row -> new LeaderboardItem(row.getName(), row.getRevenue(), row.getBookings())).toList();
        List<LeaderboardItem> topCinemas = bookings.findDashboardTopCinemas(
                        fromTime, toExclusive, cinemaId, movieId, paymentMethod, topFive)
                .stream().map(row -> new LeaderboardItem(row.getName(), row.getRevenue(), row.getBookings())).toList();
        List<Booking> recentBookings = bookings.findRecentDashboardBookings(
                fromTime, toExclusive, cinemaId, movieId, paymentMethod, PageRequest.of(0, 8));

        model.addAttribute("movieCount", movies.count());
        model.addAttribute("cinemaCount", cinemas.count());
        model.addAttribute("bookingCount", bookingCount);
        model.addAttribute("userCount", users.count());
        model.addAttribute("revenue", revenue);
        model.addAttribute("confirmedCount", confirmed);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("checkedInCount", checkedIn);
        model.addAttribute("ticketCount", bookings.countDashboardTickets(
                fromTime, toExclusive, cinemaId, movieId, paymentMethod));
        model.addAttribute("aov", aov);
        model.addAttribute("dailyMetrics", daily);
        model.addAttribute("revenueLabels", daily.stream().map(DailyMetric::label).toList());
        model.addAttribute("revenueData", daily.stream().map(DailyMetric::revenue).toList());
        model.addAttribute("bookingData", daily.stream().map(DailyMetric::confirmed).toList());
        model.addAttribute("topMovies", topMovies.stream().limit(5).toList());
        model.addAttribute("topCinemas", topCinemas.stream().limit(5).toList());
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        model.addAttribute("movies", movies.findByActiveTrueOrderByReleaseDateDesc());
        model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_QR));
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        model.addAttribute("filterCinemaId", cinemaId);
        model.addAttribute("filterMovieId", movieId);
        model.addAttribute("filterPaymentMethod", paymentMethod);
    }

}
