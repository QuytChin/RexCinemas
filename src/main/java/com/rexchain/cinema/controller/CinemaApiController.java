package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CinemaApiController {
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final ComboProductRepository combos;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final BookingService bookingService;

    public CinemaApiController(MovieRepository movies, ShowtimeRepository showtimes,
                               ShowtimeSeatRepository showtimeSeats, ComboProductRepository combos,
                               ReviewRepository reviews, UserRepository users,
                               BookingRepository bookings, BookingService bookingService) {
        this.movies = movies;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.combos = combos;
        this.reviews = reviews;
        this.users = users;
        this.bookings = bookings;
        this.bookingService = bookingService;
    }

    @GetMapping("/movies/now-showing")
    public List<Map<String, Object>> nowShowing() {
        return movies.findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(LocalDate.now())
                .stream().map(this::movieJson).toList();
    }

    @GetMapping("/movies/coming-soon")
    public List<Map<String, Object>> comingSoon() {
        return movies.findByActiveTrueAndReleaseDateGreaterThanOrderByReleaseDateAsc(LocalDate.now())
                .stream().map(this::movieJson).toList();
    }

    @GetMapping("/movies/{id}")
    public Map<String, Object> movie(@PathVariable Long id) {
        return movieJson(movies.findById(id).orElseThrow());
    }

    @GetMapping("/movies/{id}/showtimes")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> movieShowtimes(@PathVariable Long id) {
        List<Showtime> result = showtimes.findPublicByMovieIdAndStartTimeAfterOrderByStartTimeAsc(
                id, LocalDateTime.now());
        Map<Long, long[]> availability = availabilityFor(result);
        return result.stream().map(showtime -> showtimeJson(showtime, availability.get(showtime.getId()))).toList();
    }

    @GetMapping("/showtimes/{id}/seats")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> seats(@PathVariable Long id) {
        Showtime showtime = showtimes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không tồn tại"));
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(LocalDateTime.now())
                || showtime.getAuditorium().getCinema() == null
                || !showtime.getAuditorium().getCinema().isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Suất chiếu không còn mở đặt vé");
        }
        return showtimeSeats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(id).stream().map(ss -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", ss.getId());
            x.put("label", ss.getSeat().getLabel());
            x.put("type", ss.getSeat().getType().name());
            x.put("status", ss.getStatus().name());
            x.put("price", ss.getPrice());
            x.put("holdUntil", ss.getHoldUntil());
            return x;
        }).toList();
    }

    @GetMapping("/snacks")
    public List<Map<String, Object>> snacks() {
        return combos.findByActiveTrueOrderByIdAsc().stream().map(c -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", c.getId());
            x.put("name", c.getName());
            x.put("description", c.getDescription());
            x.put("price", c.getPrice());
            x.put("imageUrl", c.getImageUrl());
            return x;
        }).toList();
    }

    @GetMapping("/movies/{id}/reviews")
    public List<Map<String, Object>> movieReviews(@PathVariable Long id) {
        return reviews.findByMovieIdOrderByCreatedAtDesc(id).stream().map(r -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", r.getId());
            x.put("user", r.getUser().getFullName());
            x.put("rating", r.getRating());
            x.put("comment", r.getComment());
            x.put("createdAt", r.getCreatedAt());
            x.put("updatedAt", r.getUpdatedAt());
            return x;
        }).toList();
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        User user = current(authentication);
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", user.getId());
        x.put("fullName", user.getFullName());
        x.put("email", user.getEmail());
        x.put("phone", user.getPhone());
        x.put("avatarUrl", user.getAvatarUrl());
        x.put("role", user.getRole().name());
        x.put("loyaltyPoints", user.getLoyaltyPoints());
        x.put("lifetimePoints", user.getLifetimePoints());
        x.put("membershipTier", user.getMembershipTier().name());
        return x;
    }

    @GetMapping("/me/bookings")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myBookings(Authentication authentication) {
        User user = current(authentication);
        return bookings.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::bookingJson).toList();
    }

    @GetMapping("/payment-status/{bookingCode}")
    @Transactional
    public ResponseEntity<?> paymentStatus(@PathVariable String bookingCode, Authentication authentication) {
        Booking booking;
        try { booking = bookingService.expireIfDue(bookingCode); }
        catch (IllegalArgumentException ex) { return ResponseEntity.notFound().build(); }
        User user = current(authentication);
        if (user.getRole() == Role.CUSTOMER && !booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền xem booking này"));
        }
        return ResponseEntity.ok(bookingJson(booking));
    }

    @PostMapping("/bookings/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest request, Authentication authentication) {
        User user = current(authentication);
        if (request.showtimeId() == null || request.seatIds() == null || request.seatIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu suất chiếu hoặc ghế"));
        }
        try {
            PaymentMethod method = request.paymentMethod() == null ? PaymentMethod.BANK_QR : request.paymentMethod();
            List<Long> comboIds = new ArrayList<>();
            List<Integer> comboQtys = new ArrayList<>();
            if (request.combos() != null) {
                for (ComboLine line : request.combos()) {
                    if (line != null && line.comboId() != null && line.quantity() != null) {
                        comboIds.add(line.comboId());
                        comboQtys.add(line.quantity());
                    }
                }
            }
            bookingService.holdSeats(user.getId(), request.showtimeId(), request.seatIds());
            Booking booking = bookingService.confirm(user.getId(), request.showtimeId(), request.seatIds(), method,
                    request.voucherCode(), comboIds, comboQtys);
            Map<String, Object> body = bookingJson(booking);
            if (booking.getStatus() == BookingStatus.PENDING) {
                body.put("paymentStartUrl", "/payment/" + booking.getId() + "/start");
            }
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            bookingService.releaseOwnHolds(user.getId(), request.showtimeId(), request.seatIds());
            String message = ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                    ? Objects.requireNonNullElse(ex.getMessage(), "Không thể tạo booking") : "Không thể tạo booking";
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }
    }

    private User current(Authentication authentication) {
        if (authentication == null) throw new IllegalStateException("Chưa đăng nhập");
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }

    private Map<String, Object> movieJson(Movie m) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", m.getId());
        x.put("title", m.getTitle());
        x.put("description", m.getDescription());
        x.put("durationMinutes", m.getDurationMinutes());
        x.put("ageRating", m.getAgeRating() != null ? m.getAgeRating().getCode() : "P");
        x.put("status", m.getStatus() != null ? m.getStatus().name() : "NOW_SHOWING");
        x.put("genre", m.getGenre());
        x.put("language", m.getLanguage());
        x.put("releaseDate", m.getReleaseDate());
        x.put("posterUrl", m.getPosterUrl());
        x.put("trailerUrl", m.getTrailerUrl());
        return x;
    }

    private Map<String, Object> showtimeJson(Showtime s) {
        return showtimeJson(s, null);
    }

    private Map<String, Object> showtimeJson(Showtime s, long[] availability) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", s.getId());
        x.put("startTime", s.getStartTime());
        x.put("basePrice", s.getBasePrice());
        x.put("movieId", s.getMovie().getId());
        x.put("movie", s.getMovie().getTitle());
        x.put("cinemaId", s.getAuditorium().getCinema().getId());
        x.put("cinema", s.getAuditorium().getCinema().getName());
        x.put("auditorium", s.getAuditorium().getName());
        x.put("availableSeats", availability == null ? showtimeSeats.countByShowtimeIdAndStatus(s.getId(), SeatStatus.AVAILABLE) : availability[0]);
        x.put("totalSeats", availability == null ? showtimeSeats.countByShowtimeId(s.getId()) : availability[1]);
        x.put("bookingUrl", "/booking/" + s.getId());
        return x;
    }

    private Map<Long, long[]> availabilityFor(List<Showtime> list) {
        Map<Long, long[]> result = new HashMap<>();
        List<Long> ids = list.stream().map(Showtime::getId).distinct().toList();
        if (!ids.isEmpty()) {
            for (Object[] row : showtimeSeats.summarizeAvailability(ids)) {
                result.put((Long) row[0], new long[]{((Number) row[2]).longValue(), ((Number) row[1]).longValue()});
            }
        }
        ids.forEach(id -> result.putIfAbsent(id, new long[]{0, 0}));
        return result;
    }

    private Map<String, Object> bookingJson(Booking b) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", b.getId());
        x.put("bookingCode", b.getBookingCode());
        x.put("status", b.getStatus().name());
        x.put("paymentMethod", b.getPaymentMethod().name());
        x.put("totalAmount", b.getTotalAmount());
        x.put("movie", b.getShowtime().getMovie().getTitle());
        x.put("cinema", b.getShowtime().getAuditorium().getCinema().getName());
        x.put("showtime", b.getShowtime().getStartTime());
        x.put("seats", b.getSeats().stream().map(BookingSeat::getSeatLabel).toList());
        x.put("checkedIn", b.isCheckedIn());
        x.put("checkedInAt", b.getCheckedInAt());
        x.put("ticketUrl", "/bookings/" + b.getId());
        x.put("qrImageUrl", b.getStatus() == BookingStatus.CONFIRMED ? "/bookings/" + b.getId() + "/qr" : null);
        x.put("createdAt", b.getCreatedAt());
        return x;
    }

    public record ComboLine(
        @NotNull(message = "Mã combo không được để trống") Long comboId,
        @NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng combo tối thiểu là 1") Integer quantity
    ) {}

    public record CheckoutRequest(
        @NotNull(message = "Suất chiếu không được để trống") Long showtimeId,
        @NotEmpty(message = "Danh sách ghế không được để trống") List<Long> seatIds,
        PaymentMethod paymentMethod,
        String voucherCode,
        List<ComboLine> combos
    ) {}
}
