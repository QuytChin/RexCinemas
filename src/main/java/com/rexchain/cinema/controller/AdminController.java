package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final AuditoriumRepository auditoriums;
    private final ShowtimeRepository showtimes;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final CinemaSetupService setup;
    private final CloudinaryService cloudinary;
    private final ComboProductRepository combos;
    private final VoucherRepository vouchers;
    private final ReviewRepository reviews;
    private final DashboardService dashboardService;
    private final BookingService bookingService;

    public AdminController(MovieRepository movies, CinemaRepository cinemas, AuditoriumRepository auditoriums,
                           ShowtimeRepository showtimes, BookingRepository bookings, UserRepository users,
                           CinemaSetupService setup, CloudinaryService cloudinary, ComboProductRepository combos,
                           VoucherRepository vouchers, ReviewRepository reviews,
                           DashboardService dashboardService, BookingService bookingService) {
        this.movies = movies;
        this.cinemas = cinemas;
        this.auditoriums = auditoriums;
        this.showtimes = showtimes;
        this.bookings = bookings;
        this.users = users;
        this.setup = setup;
        this.cloudinary = cloudinary;
        this.combos = combos;
        this.vouchers = vouchers;
        this.reviews = reviews;
        this.dashboardService = dashboardService;
        this.bookingService = bookingService;
    }

    @GetMapping
    String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                     @RequestParam(required = false) Long cinemaId,
                     @RequestParam(required = false) Long movieId,
                     @RequestParam(required = false) PaymentMethod paymentMethod,
                     Model model) {
        dashboardService.populate(model, from, to, cinemaId, movieId, paymentMethod);
        return "admin/dashboard";
    }

    @GetMapping("/movies")
    String movies(Model model) {
        model.addAttribute("movies", movies.findAll());
        return "admin/movies";
    }

    @GetMapping("/movies/new")
    String movieNew(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movie-form";
    }

    @GetMapping("/movies/{id}/edit")
    String movieEdit(@PathVariable Long id, Model model) {
        model.addAttribute("movie", movies.findById(id).orElseThrow());
        return "admin/movie-form";
    }

    @PostMapping("/movies/save")
    String movieSave(@RequestParam(required = false) Long id,
                     @RequestParam String title,
                     @RequestParam String description,
                     @RequestParam int durationMinutes,
                     @RequestParam(required = false) String ageRating,
                     @RequestParam(required = false) String genre,
                     @RequestParam(required = false) String language,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
                     @RequestParam(required = false) String posterUrl,
                     @RequestParam(required = false) String trailerUrl,
                     @RequestParam(required = false) MultipartFile posterFile) throws Exception {
        Movie movie = id == null ? new Movie() : movies.findById(id).orElseThrow();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationMinutes(durationMinutes);
        movie.setAgeRating(ageRating);
        movie.setGenre(genre);
        movie.setLanguage(language);
        movie.setReleaseDate(releaseDate);
        movie.setTrailerUrl(trailerUrl);
        if (posterFile != null && !posterFile.isEmpty()) movie.setPosterUrl(cloudinary.uploadMoviePoster(posterFile));
        else if (posterUrl != null && !posterUrl.isBlank()) movie.setPosterUrl(posterUrl);
        movies.save(movie);
        return "redirect:/admin/movies";
    }

    @PostMapping("/movies/{id}/toggle")
    String movieToggle(@PathVariable Long id) {
        Movie movie = movies.findById(id).orElseThrow();
        movie.setActive(!movie.isActive());
        movies.save(movie);
        return "redirect:/admin/movies";
    }

    @PostMapping("/movies/{id}/delete")
    String movieDelete(@PathVariable Long id) {
        Movie movie = movies.findById(id).orElseThrow();
        movie.setActive(false);
        movies.save(movie);
        return "redirect:/admin/movies";
    }

    @GetMapping("/cinemas")
    String cinemas(Model model) {
        model.addAttribute("cinemas", cinemas.findAll());
        return "admin/cinemas";
    }

    @GetMapping("/cinemas/new")
    String cinemaNew(Model model) {
        model.addAttribute("cinema", new Cinema());
        return "admin/cinema-form";
    }

    @GetMapping("/cinemas/{id}/edit")
    String cinemaEdit(@PathVariable Long id, Model model) {
        model.addAttribute("cinema", cinemas.findById(id).orElseThrow());
        return "admin/cinema-form";
    }

    @PostMapping("/cinemas/save")
    String cinemaSave(@RequestParam(required = false) Long id,
                      @RequestParam String name,
                      @RequestParam String city,
                      @RequestParam String address,
                      @RequestParam(required = false) String phone) {
        Cinema cinema = id == null ? new Cinema() : cinemas.findById(id).orElseThrow();
        cinema.setName(name);
        cinema.setCity(city);
        cinema.setAddress(address);
        cinema.setPhone(phone);
        cinemas.save(cinema);
        return "redirect:/admin/cinemas";
    }

    @PostMapping("/cinemas/{id}/delete")
    String cinemaDelete(@PathVariable Long id) {
        Cinema cinema = cinemas.findById(id).orElseThrow();
        cinema.setActive(false);
        cinemas.save(cinema);
        return "redirect:/admin/cinemas";
    }

    @PostMapping("/cinemas/{id}/toggle")
    String cinemaToggle(@PathVariable Long id) {
        Cinema cinema = cinemas.findById(id).orElseThrow();
        cinema.setActive(!cinema.isActive());
        cinemas.save(cinema);
        return "redirect:/admin/cinemas";
    }

    @GetMapping("/auditoriums")
    String auditoriums(Model model) {
        model.addAttribute("auditoriums", auditoriums.findAll());
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        return "admin/auditoriums";
    }

    @PostMapping("/auditoriums/save")
    String auditoriumSave(@RequestParam Long cinemaId,
                          @RequestParam String name,
                          @RequestParam int totalRows,
                          @RequestParam int seatsPerRow) {
        Cinema cinema = cinemas.findById(cinemaId).orElseThrow();
        if (!cinema.isActive()) throw new IllegalArgumentException("Rạp đang bị ẩn");
        setup.createAuditorium(cinema, name, totalRows, seatsPerRow);
        return "redirect:/admin/auditoriums";
    }

    @GetMapping("/showtimes")
    String showtimes(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("showtimes", showtimes.findAll());
        model.addAttribute("movies", movies.findAll());
        model.addAttribute("auditoriums", auditoriums.findAll());
        model.addAttribute("editShowtime", edit == null ? new Showtime() : showtimes.findById(edit).orElse(new Showtime()));
        model.addAttribute("now", LocalDateTime.now());
        return "admin/showtimes";
    }

    @PostMapping("/showtimes/save")
    String showtimeSave(@RequestParam(required = false) Long id,
                        @RequestParam Long movieId,
                        @RequestParam Long auditoriumId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                        @RequestParam BigDecimal basePrice,
                        RedirectAttributes redirect) {
        try {
            boolean create = id == null;
            Showtime showtime = create ? new Showtime() : showtimes.findById(id).orElseThrow();
            if (!create && bookings.existsByShowtimeIdAndStatusNot(id, BookingStatus.CANCELLED))
                throw new IllegalStateException("Suất chiếu đã có vé bán ra nên không thể đổi giờ hoặc giá. Bạn có thể ẩn suất để ngừng bán thêm.");
            if (create) {
                Movie movie = movies.findById(movieId).orElseThrow();
                Auditorium auditorium = auditoriums.findById(auditoriumId).orElseThrow();
                if (!movie.isActive()) throw new IllegalArgumentException("Phim đang bị ẩn");
                if (!auditorium.getCinema().isActive()) throw new IllegalArgumentException("Rạp đang bị ẩn");
                showtime.setMovie(movie);
                showtime.setAuditorium(auditorium);
            }
            if (startTime == null || startTime.isBefore(LocalDateTime.now().minusMinutes(1)))
                throw new IllegalArgumentException("Suất chiếu phải ở thời điểm hiện tại hoặc tương lai");
            if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Giá vé phải lớn hơn 0");

            showtime.setStartTime(startTime);
            showtime.setBasePrice(basePrice);
            validateShowtimeSlot(showtime, id);
            showtime = showtimes.save(showtime);
            if (create) setup.createShowtimeSeats(showtime);
            else setup.repriceShowtimeSeats(showtime);
            redirect.addFlashAttribute("success", create ? "Đã tạo suất chiếu và sơ đồ ghế" : "Đã cập nhật suất chiếu");
            return "redirect:/admin/showtimes";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/showtimes" + (id == null ? "" : "?edit=" + id);
        }
    }

    @PostMapping("/showtimes/{id}/delete")
    String showtimeDelete(@PathVariable Long id) {
        Showtime showtime = showtimes.findById(id).orElseThrow();
        showtime.setActive(false);
        showtimes.save(showtime);
        return "redirect:/admin/showtimes";
    }

    @PostMapping("/showtimes/{id}/toggle")
    String showtimeToggle(@PathVariable Long id) {
        Showtime showtime = showtimes.findById(id).orElseThrow();
        showtime.setActive(!showtime.isActive());
        showtimes.save(showtime);
        return "redirect:/admin/showtimes";
    }

    @GetMapping("/bookings")
    String bookings(Model model) {
        model.addAttribute("bookings", bookings.findAllDetailed());
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    String cancelBooking(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            bookingService.cancelByAdmin(id);
            redirect.addFlashAttribute("success", "Đã hủy booking và trả ghế về hệ thống");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/users")
    String users(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    String toggleUser(@PathVariable Long id) {
        User user = users.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        users.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    String roleUser(@PathVariable Long id, @RequestParam Role role) {
        User user = users.findById(id).orElseThrow();
        user.setRole(role);
        users.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/combos")
    String combos(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("combos", combos.findAll());
        model.addAttribute("editCombo", edit == null ? new ComboProduct() : combos.findById(edit).orElse(new ComboProduct()));
        return "admin/combos";
    }

    @PostMapping("/combos/save")
    String saveCombo(@RequestParam(required = false) Long id,
                     @RequestParam String name,
                     @RequestParam(required = false) String description,
                     @RequestParam BigDecimal price,
                     @RequestParam(required = false) String imageUrl,
                     @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        ComboProduct combo = id == null ? new ComboProduct() : combos.findById(id).orElseThrow();
        combo.setName(name);
        combo.setDescription(description);
        combo.setPrice(price);
        if (imageFile != null && !imageFile.isEmpty()) combo.setImageUrl(cloudinary.uploadComboImage(imageFile));
        else if (imageUrl != null && !imageUrl.isBlank()) combo.setImageUrl(imageUrl);
        combos.save(combo);
        return "redirect:/admin/combos";
    }

    @PostMapping("/combos/{id}/toggle")
    String toggleCombo(@PathVariable Long id) {
        ComboProduct combo = combos.findById(id).orElseThrow();
        combo.setActive(!combo.isActive());
        combos.save(combo);
        return "redirect:/admin/combos";
    }

    @GetMapping("/vouchers")
    String vouchers(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("vouchers", vouchers.findAll());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("editVoucher", edit == null ? new Voucher() : vouchers.findById(edit).orElse(new Voucher()));
        return "admin/vouchers";
    }

    @PostMapping("/vouchers/save")
    String saveVoucher(@RequestParam(required = false) Long id,
                       @RequestParam String code,
                       @RequestParam String name,
                       @RequestParam DiscountType discountType,
                       @RequestParam BigDecimal discountValue,
                       @RequestParam(required = false) BigDecimal minOrderAmount,
                       @RequestParam(required = false) BigDecimal maxDiscountAmount,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validUntil,
                       @RequestParam(required = false) Integer usageLimit) {
        Voucher voucher = id == null ? new Voucher() : vouchers.findById(id).orElseThrow();
        voucher.setCode(code.trim().toUpperCase());
        voucher.setName(name);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMinOrderAmount(minOrderAmount == null ? BigDecimal.ZERO : minOrderAmount);
        voucher.setMaxDiscountAmount(maxDiscountAmount);
        voucher.setValidFrom(validFrom);
        voucher.setValidUntil(validUntil);
        voucher.setUsageLimit(usageLimit);
        vouchers.save(voucher);
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/{id}/toggle")
    String toggleVoucher(@PathVariable Long id) {
        Voucher voucher = vouchers.findById(id).orElseThrow();
        voucher.setActive(!voucher.isActive());
        vouchers.save(voucher);
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/reviews")
    String reviews(Model model) {
        model.addAttribute("reviews", reviews.findAll());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    String deleteReview(@PathVariable Long id) {
        reviews.deleteById(id);
        return "redirect:/admin/reviews";
    }
    private void validateShowtimeSlot(Showtime candidate, Long currentId) {
        LocalDateTime start = candidate.getStartTime();
        LocalDateTime end = start.plusMinutes(candidate.getMovie().getDurationMinutes() + 15L);
        List<Showtime> nearby = showtimes.findByAuditoriumIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
                candidate.getAuditorium().getId(), start.minusHours(6), end.plusHours(1));
        for (Showtime other : nearby) {
            if (currentId != null && other.getId().equals(currentId)) continue;
            LocalDateTime otherEnd = other.getStartTime().plusMinutes(other.getMovie().getDurationMinutes() + 15L);
            if (start.isBefore(otherEnd) && end.isAfter(other.getStartTime())) {
                throw new IllegalArgumentException("Phòng đã có suất '" + other.getMovie().getTitle() + "' lúc "
                        + other.getStartTime().toLocalTime() + ". Cần cách nhau đủ thời lượng phim + 15 phút dọn phòng.");
            }
        }
    }

}
