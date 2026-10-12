package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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
    private final ShowtimeService showtimeService; // <-- Thêm dòng này

    public AdminController(MovieRepository movies, CinemaRepository cinemas, AuditoriumRepository auditoriums,
            ShowtimeRepository showtimes, BookingRepository bookings, UserRepository users,
            CinemaSetupService setup, CloudinaryService cloudinary, ComboProductRepository combos,
            VoucherRepository vouchers, ReviewRepository reviews,
            DashboardService dashboardService, BookingService bookingService,
            ShowtimeService showtimeService) { // <-- 1. Thêm tham số này vào cuối
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
        this.showtimeService = showtimeService; // <-- 2. Gán giá trị ở đây
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
        model.addAttribute("ageRatings", AgeRating.values());
        model.addAttribute("movieStatuses", MovieStatus.values());
        return "admin/movie-form";
    }

    @GetMapping("/movies/{id}/edit")
    String movieEdit(@PathVariable Long id, Model model) {
        model.addAttribute("movie", movies.findById(id).orElseThrow());
        model.addAttribute("ageRatings", AgeRating.values());
        model.addAttribute("movieStatuses", MovieStatus.values());
        return "admin/movie-form";
    }

    @PostMapping("/movies/save")
    String movieSave(@RequestParam(required = false) Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int durationMinutes,
            @RequestParam(required = false) AgeRating ageRating,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam(required = false) String posterUrl,
            @RequestParam(required = false) String trailerUrl,
            @RequestParam(required = false) MultipartFile posterFile) throws Exception {
        title = requiredText(title, "Tên phim", 180);
        description = requiredText(description, "Mô tả phim", 10_000);
        if (durationMinutes < 1 || durationMinutes > 600)
            throw new IllegalArgumentException("Thời lượng phim phải từ 1 đến 600 phút");
        boolean duplicateMovie = id == null
                ? movies.existsByTitleIgnoreCaseAndReleaseDate(title, releaseDate)
                : movies.existsByTitleIgnoreCaseAndReleaseDateAndIdNot(title, releaseDate, id);
        if (duplicateMovie) throw new IllegalArgumentException("Phim và ngày phát hành đã tồn tại");
        Movie movie = id == null ? new Movie() : movies.findById(id).orElseThrow();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationMinutes(durationMinutes);
        if (ageRating != null) movie.setAgeRating(ageRating);
        if (status != null) movie.setStatus(status);
        movie.setGenre(genre);
        movie.setLanguage(language);
        movie.setReleaseDate(releaseDate);
        movie.setTrailerUrl(trailerUrl);
        if (posterFile != null && !posterFile.isEmpty())
            movie.setPosterUrl(cloudinary.uploadMoviePoster(posterFile));
        else if (posterUrl != null && !posterUrl.isBlank())
            movie.setPosterUrl(posterUrl);
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
        name = requiredText(name, "Tên rạp", 120);
        city = requiredText(city, "Thành phố", 100);
        address = requiredText(address, "Địa chỉ", 255);
        boolean duplicateCinema = id == null
                ? cinemas.existsByNameIgnoreCaseAndAddressIgnoreCase(name, address)
                : cinemas.existsByNameIgnoreCaseAndAddressIgnoreCaseAndIdNot(name, address, id);
        if (duplicateCinema) throw new IllegalArgumentException("Rạp với tên và địa chỉ này đã tồn tại");
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
        if (!cinema.isActive())
            throw new IllegalArgumentException("Rạp đang bị ẩn");
        setup.createAuditorium(cinema, name, totalRows, seatsPerRow);
        return "redirect:/admin/auditoriums";
    }

    @GetMapping("/showtimes")
    String showtimes(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("showtimes", showtimes.findAll());
        model.addAttribute("movies", movies.findAll());
        model.addAttribute("auditoriums", auditoriums.findAll());
        model.addAttribute("editShowtime",
                edit == null ? new Showtime() : showtimes.findById(edit).orElse(new Showtime()));
        model.addAttribute("now", LocalDateTime.now());
        return "admin/showtimes";
    }

    @PostMapping("/showtimes/save")
    @Transactional
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
                throw new IllegalStateException(
                        "Suất chiếu đã có vé bán ra nên không thể đổi giờ hoặc giá. Bạn có thể ẩn suất để ngừng bán thêm.");
            if (create) {
                Movie movie = movies.findById(movieId).orElseThrow();
                Auditorium auditorium = auditoriums.findById(auditoriumId).orElseThrow();
                if (!movie.isActive())
                    throw new IllegalArgumentException("Phim đang bị ẩn");
                if (!auditorium.getCinema().isActive())
                    throw new IllegalArgumentException("Rạp đang bị ẩn");
                showtime.setMovie(movie);
                showtime.setAuditorium(auditorium);
            }
            if (startTime == null || startTime.isBefore(LocalDateTime.now().minusMinutes(1)))
                throw new IllegalArgumentException("Suất chiếu phải ở thời điểm hiện tại hoặc tương lai");
            if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Giá vé phải lớn hơn 0");

            showtime.setStartTime(startTime);
            showtime.setBasePrice(basePrice);
            showtimeService.validateShowtimeSlot(showtime, id);
            showtime = showtimes.save(showtime);
            if (create)
                setup.createShowtimeSeats(showtime);
            else
                setup.repriceShowtimeSeats(showtime);
            redirect.addFlashAttribute("success", create ? "Đã tạo suất chiếu và sơ đồ ghế" : "Đã cập nhật suất chiếu");
            return "redirect:/admin/showtimes";
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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

    @PostMapping("/bookings/{id}/refund")
    String refundBooking(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            bookingService.processRefund(id, auth.getName(), note);
            redirect.addFlashAttribute("success", "Đã duyệt và xác nhận hoàn tiền cho booking #" + id);
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
        model.addAttribute("editCombo",
                edit == null ? new ComboProduct() : combos.findById(edit).orElse(new ComboProduct()));
        return "admin/combos";
    }

    @PostMapping("/combos/save")
    String saveCombo(@RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
             @RequestParam int stockQuantity,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        name = requiredText(name, "Tên combo", 120);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Giá combo phải lớn hơn 0");
        if (stockQuantity < 0 || stockQuantity > 1_000_000)
            throw new IllegalArgumentException("Tồn kho phải từ 0 đến 1.000.000");
        ComboProduct combo = id == null ? new ComboProduct() : combos.findById(id).orElseThrow();
        combo.setName(name);
        combo.setDescription(description);
        combo.setPrice(price);
        combo.setStockQuantity(stockQuantity);
        if (imageFile != null && !imageFile.isEmpty())
            combo.setImageUrl(cloudinary.uploadComboImage(imageFile));
        else if (imageUrl != null && !imageUrl.isBlank())
            combo.setImageUrl(imageUrl);
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
        code = requiredText(code, "Mã voucher", 40).toUpperCase();
        name = requiredText(name, "Tên voucher", 160);
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0
                || (discountType == DiscountType.PERCENT && discountValue.compareTo(BigDecimal.valueOf(100)) > 0))
            throw new IllegalArgumentException("Giá trị voucher không hợp lệ");
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0
                || maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Giá trị đơn tối thiểu/giảm tối đa không được âm");
        if (usageLimit != null && usageLimit <= 0)
            throw new IllegalArgumentException("Giới hạn lượt dùng phải lớn hơn 0");
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom))
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        boolean duplicateVoucher = id == null ? vouchers.existsByCodeIgnoreCase(code)
                : vouchers.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (duplicateVoucher) throw new IllegalArgumentException("Mã voucher đã tồn tại");
        Voucher voucher = id == null ? new Voucher() : vouchers.findById(id).orElseThrow();
        voucher.setCode(code);
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

    private String requiredText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " không được để trống");
        if (normalized.length() > maxLength)
            throw new IllegalArgumentException(field + " không được vượt quá " + maxLength + " ký tự");
        return normalized;
    }

}
