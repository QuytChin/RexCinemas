package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.config.SePayProperties;
import com.rexchain.cinema.dto.PaymentSettingsUpdateRequest;
import com.rexchain.cinema.dto.ManualRefundConfirmation;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
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
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final AuditoriumRepository auditoriums;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showSeats;
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
    private final SePayPaymentService sepayPaymentService;
    private final SePayProperties sepayProperties;
    private final PaymentSettingsService paymentSettingsService;
    private final PaymentTransactionRepository paymentTransactions;
    private final LatePaymentCaseService latePaymentCases;
    private final MembershipService membershipService;
    private final AdminListSupport lists;

    public AdminController(MovieRepository movies, CinemaRepository cinemas, AuditoriumRepository auditoriums,
            ShowtimeRepository showtimes, ShowtimeSeatRepository showSeats, BookingRepository bookings, UserRepository users,
            CinemaSetupService setup, CloudinaryService cloudinary, ComboProductRepository combos,
            VoucherRepository vouchers, ReviewRepository reviews,
            DashboardService dashboardService, BookingService bookingService,
            ShowtimeService showtimeService, SePayPaymentService sepayPaymentService,
            SePayProperties sepayProperties, PaymentSettingsService paymentSettingsService,
            PaymentTransactionRepository paymentTransactions, LatePaymentCaseService latePaymentCases,
            MembershipService membershipService, AdminListSupport lists) {
        this.movies = movies;
        this.cinemas = cinemas;
        this.auditoriums = auditoriums;
        this.showtimes = showtimes;
        this.showSeats = showSeats;
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
        this.sepayPaymentService = sepayPaymentService;
        this.sepayProperties = sepayProperties;
        this.paymentSettingsService = paymentSettingsService;
        this.paymentTransactions = paymentTransactions;
        this.latePaymentCases = latePaymentCases;
        this.membershipService = membershipService;
        this.lists = lists;
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

    @GetMapping("/payments")
    @Transactional(readOnly = true)
    String paymentSettings(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var savedSettings = paymentSettingsService.current();
        boolean accountConfigured = hasText(savedSettings.getBankCode()) && hasText(savedSettings.getBankAccount());
        boolean bankQrAvailable = savedSettings.isEnabled() && accountConfigured
                && (savedSettings.isSchemaAvailable() || sepayProperties.isEnabled());
        boolean webhookConfigured = sepayProperties.getApiKey() != null && !sepayProperties.getApiKey().isBlank();
        String bankCode = hasText(savedSettings.getBankCode()) ? savedSettings.getBankCode() : sepayProperties.getBankCode();
        String bankAccount = hasText(savedSettings.getBankAccount()) ? savedSettings.getBankAccount() : sepayProperties.getBankAccount();
        String paymentPrefix = hasText(savedSettings.getPaymentPrefix()) ? savedSettings.getPaymentPrefix() : sepayProperties.getPaymentPrefix();
        model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_QR));
        var options = new AdminListSupport.Options("bookingCode,user.email", "status",
                new String[]{"createdAt", "totalAmount", "status", "id"}, new String[]{"user", "showtime.movie", "showtime.auditorium.cinema"});
        Page<Booking> page = bookingPage(params, options);
        display(model, request, params, page, options, "transactions", BookingStatus.values());
        model.addAttribute("accountConfigured", accountConfigured);
        model.addAttribute("bankQrAvailable", bankQrAvailable);
        model.addAttribute("sepayEnabled", sepayProperties.isEnabled());
        model.addAttribute("webhookConfigured", webhookConfigured);
        model.addAttribute("paymentPrefix", blankOrFallback(paymentPrefix, "REX"));
        model.addAttribute("maskedBankAccount", maskAccount(bankAccount));
        model.addAttribute("bankCode", blankOrFallback(bankCode, "Chưa cấu hình"));
        model.addAttribute("qrPreviewUrl", hasText(bankCode) && hasText(bankAccount)
                ? sepayPaymentService.qrImageUrl(bankCode, bankAccount, "REXDEMO", BigDecimal.valueOf(100_000)) : null);
        model.addAttribute("paymentSettings", savedSettings);
        model.addAttribute("settingsSchemaAvailable", savedSettings.isSchemaAvailable());
        return "admin/payment-settings";
    }

    @PostMapping("/payments/save")
    String paymentSettingsSave(@ModelAttribute PaymentSettingsUpdateRequest request,
                               Authentication authentication, RedirectAttributes redirect) {
        try {
            paymentSettingsService.save(request, authentication.getName());
            redirect.addFlashAttribute("success", "Đã lưu cấu hình thanh toán không bí mật");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @GetMapping("/transactions")
    String transactions(@RequestParam(required = false) String status,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) String q, @RequestParam Map<String, String> params,
                        HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("", "status", "receivedAt", "amount", "status", "id");
        var page = paymentTransactions.search(status, from, to, q, lists.pageable(params, options.sorts()));
        display(model, request, params, page, options, "transactions", "RECEIVED", "VERIFIED", "SETTLED", "NEEDS_REVIEW", "REFUND_PENDING", "LATE_PAYMENT_REFUND_PENDING", "DUPLICATE");
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        model.addAttribute("filterQuery", q);
        model.addAttribute("transactionStatuses", List.of("RECEIVED", "VERIFIED", "SETTLED", "NEEDS_REVIEW", "REFUND_PENDING", "LATE_PAYMENT_REFUND_PENDING", "DUPLICATE"));
        model.addAttribute("transactionsSchemaAvailable", paymentTransactions.isSchemaAvailable());
        return "admin/transactions";
    }

    @GetMapping("/late-payments")
    String latePayments(@RequestParam(required = false) LatePaymentCaseStatus status,
                        @RequestParam(required = false, name = "q") String query,
                        @RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("", "status", "createdAt", "amount", "status", "id");
        var page = latePaymentCases.search(status, query, lists.pageable(params, options.sorts()));
        display(model, request, params, page, options, "cases", LatePaymentCaseStatus.values());
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterQuery", query);
        model.addAttribute("caseStatuses", LatePaymentCaseStatus.values());
        return "admin/late-payments";
    }

    @GetMapping("/late-payments/{id}")
    String latePaymentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("latePaymentCase", latePaymentCases.findDetailed(id));
        model.addAttribute("history", latePaymentCases.auditHistory(id));
        return "admin/late-payment-detail";
    }

    @PostMapping("/late-payments/{id}/review")
    String latePaymentReview(@PathVariable Long id, @RequestParam(required = false) String note,
                             Authentication auth, RedirectAttributes redirect) {
        try {
            User operator = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
            latePaymentCases.markNeedsReview(id, operator.getId(), operator.getEmail(), note);
            redirect.addFlashAttribute("success", "Đã đánh dấu hồ sơ đang xử lý");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/late-payments/" + id;
    }

    @PostMapping("/late-payments/{id}/refund-failed")
    String latePaymentRefundFailed(@PathVariable Long id, @RequestParam String reason,
                                   Authentication auth, RedirectAttributes redirect) {
        try {
            User operator = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
            latePaymentCases.markRefundFailed(id, operator.getId(), operator.getEmail(), reason);
            redirect.addFlashAttribute("success", "Đã lưu lần hoàn tiền thất bại; có thể thử lại sau đối soát");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/late-payments/" + id;
    }

    @PostMapping("/late-payments/{id}/refund")
    String latePaymentRefund(@PathVariable Long id, @RequestParam String refundReference,
                             @RequestParam BigDecimal refundAmount, @RequestParam(required = false) String note,
                             @RequestParam(required = false) String refundEvidence,
                             @RequestParam(defaultValue = "false") boolean manualTransferConfirmed,
                             Authentication auth, RedirectAttributes redirect) {
        try {
            if (!manualTransferConfirmed) throw new IllegalArgumentException("Chỉ xác nhận sau khi đã hoàn tiền thủ công");
            User operator = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
            latePaymentCases.confirmRefund(id, operator.getId(), operator.getEmail(),
                    new ManualRefundConfirmation(refundReference, refundAmount, note, refundEvidence));
            redirect.addFlashAttribute("success", "Đã lưu xác nhận hoàn tiền thủ công");
        } catch (Exception ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/late-payments/" + id;
    }

    @GetMapping("/transactions/{id}")
    String transactionDetail(@PathVariable Long id, Model model) {
        model.addAttribute("transaction", paymentTransactions.findById(id).orElseThrow());
        return "admin/transaction-detail";
    }

    private String blankOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) return "Chưa cấu hình";
        String value = account.trim();
        return value.length() <= 4 ? "••••" : "•••• " + value.substring(value.length() - 4);
    }

    @GetMapping("/movies")
    String movies(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("title,genre", "status", "createdAt", "title", "releaseDate", "id")
                .withLabels("Trạng thái", "Nhập tên phim");
        display(model, request, params, lists.query(movies, params, options), options, "movies", MovieStatus.values());
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
    String cinemas(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("name", "city", "id", "name", "city")
                .withLabels("Thành phố", "Nhập tên rạp")
                .withoutSortControls();
        String[] cities = cinemas.findAll().stream().map(Cinema::getCity).filter(java.util.Objects::nonNull)
                .distinct().sorted().toArray(String[]::new);
        String active = params.getOrDefault("active", "");
        org.springframework.data.jpa.domain.Specification<Cinema> activeFilter = (root, query, cb) -> {
            if (active.isBlank()) return cb.conjunction();
            if ("true".equals(active) || "false".equals(active))
                return cb.equal(root.get("active"), Boolean.valueOf(active));
            return cb.disjunction();
        };
        display(model, request, params, lists.query(cinemas, params, options, activeFilter), options, "cinemas", (Object[]) cities);
        model.addAttribute("adminShowActiveFilter", true);
        model.addAttribute("adminActive", active);
        model.addAttribute("adminToolbarVariant", "cinemas");
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
    String auditoriums(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("name,cinema.name", "cinema.name",
                new String[]{"name", "id"}, new String[]{"cinema"})
                .withLabels("Rạp", "Nhập tên phòng hoặc rạp");
        String[] cinemaNames = cinemas.findAll().stream().map(Cinema::getName)
                .filter(java.util.Objects::nonNull).distinct().sorted().toArray(String[]::new);
        display(model, request, params, lists.query(auditoriums, params, options), options, "auditoriums",
                (Object[]) cinemaNames);
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
        return "admin/auditoriums";
    }

    @PostMapping("/auditoriums/save")
    String auditoriumSave(@RequestParam Long cinemaId,
            @RequestParam String name,
            @RequestParam int totalRows,
            @RequestParam int seatsPerRow,
            RedirectAttributes redirect) {
        try {
            Cinema cinema = cinemas.findById(cinemaId).orElseThrow();
            if (!cinema.isActive())
            throw new IllegalArgumentException("Rạp đang bị ẩn");
            setup.createAuditorium(cinema, name, totalRows, seatsPerRow);
        redirect.addFlashAttribute("success", "Tạo phòng chiếu thành công.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/auditoriums";
    }

    @GetMapping("/showtimes")
    String showtimes(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                     HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("movie.title,auditorium.name", "active",
                new String[]{"startTime", "basePrice", "id"}, new String[]{"movie", "auditorium.cinema"});
        display(model, request, params, lists.query(showtimes, params, options), options, "showtimes", "true", "false");
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
    String showtimeDelete(@PathVariable Long id, RedirectAttributes redirect) {
        Showtime showtime = showtimes.findById(id).orElseThrow();
        if (!canHideShowtime(showtime)) {
            redirect.addFlashAttribute("error", "Không thể ẩn suất chiếu đang có vé hợp lệ, booking chờ thanh toán hoặc ghế đang được giữ.");
            return "redirect:/admin/showtimes";
        }
        showtime.setActive(false);
        showtimes.save(showtime);
        return "redirect:/admin/showtimes";
    }

    @PostMapping("/showtimes/{id}/toggle")
    String showtimeToggle(@PathVariable Long id, RedirectAttributes redirect) {
        Showtime showtime = showtimes.findById(id).orElseThrow();
        if (showtime.isActive() && !canHideShowtime(showtime)) {
            redirect.addFlashAttribute("error", "Không thể ẩn suất chiếu đang có vé hợp lệ, booking chờ thanh toán hoặc ghế đang được giữ.");
            return "redirect:/admin/showtimes";
        }
        showtime.setActive(!showtime.isActive());
        showtimes.save(showtime);
        return "redirect:/admin/showtimes";
    }

    private boolean canHideShowtime(Showtime showtime) {
        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) return true;
        boolean protectedBooking = bookings.existsByShowtimeIdAndStatusIn(showtime.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.REFUND_PENDING));
        boolean activeHold = showSeats.existsByShowtimeIdAndStatusAndHoldUntilAfter(
                showtime.getId(), SeatStatus.HELD, LocalDateTime.now());
        return !protectedBooking && !activeHold;
    }

    @GetMapping("/bookings")
    @Transactional(readOnly = true)
    String bookings(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("bookingCode,user.email,user.fullName,showtime.movie.title", "status",
                new String[]{"createdAt", "totalAmount", "status", "id"}, new String[]{"user", "showtime.movie", "showtime.auditorium.cinema"});
        display(model, request, params, bookingPage(params, options), options, "bookings", BookingStatus.values());
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
    String refundBooking(@PathVariable Long id, @RequestParam String refundReference,
                         @RequestParam BigDecimal refundAmount, @RequestParam(required = false) String note,
                         @RequestParam(required = false) String refundEvidence,
                         @RequestParam(defaultValue = "false") boolean manualTransferConfirmed,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            if (!manualTransferConfirmed)
                throw new IllegalArgumentException("Chỉ xác nhận sau khi đã chuyển khoản hoàn tiền thủ công");
            User operator = users.findByEmailIgnoreCase(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Không xác định được tài khoản quản trị viên"));
            bookingService.processRefund(id, operator.getId(), operator.getEmail(),
                    new ManualRefundConfirmation(refundReference, refundAmount, note, refundEvidence));
            redirect.addFlashAttribute("success", "Đã ghi nhận xác nhận hoàn tiền thủ công cho booking #" + id);
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    String users(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("fullName,email,username,phone", "role", "createdAt", "email", "fullName", "id");
        Page<User> page = lists.query(users, params, options);
        display(model, request, params, page, options, "users", Role.values());
        java.util.Map<Long, MembershipTier> memberTiers = new java.util.HashMap<>();
        var policies = membershipService.tierPolicies();
        page.forEach(user -> memberTiers.put(user.getId(), policies.stream()
                .filter(policy -> policy.isActive() && policy.getQualifyingThreshold() <= user.getTierQualifyingPoints())
                .max(java.util.Comparator.comparingInt(policy -> policy.getTier().ordinal()))
                .orElseThrow().getTier()));
        model.addAttribute("memberTiers", memberTiers);
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
    String combos(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                  HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("name", "active", "createdAt", "name", "price", "id");
        display(model, request, params, lists.query(combos, params, options), options, "combos", "true", "false");
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
    String vouchers(@RequestParam(required = false) Long edit, @RequestParam Map<String, String> params,
                    HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("code,name", "active", "id", "code", "name", "validUntil");
        display(model, request, params, lists.query(vouchers, params, options), options, "vouchers", "true", "false");
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
    String reviews(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("movie.title,user.email", null,
                new String[]{"createdAt", "rating", "id"}, new String[]{"movie", "user"});
        display(model, request, params, lists.query(reviews, params, options), options, "reviews");
        return "admin/reviews";
    }

    private Page<Booking> bookingPage(Map<String, String> params, AdminListSupport.Options options) {
        Page<Booking> page = lists.query(bookings, params, options);
        if (page.hasContent()) bookings.loadSeatsForPage(page.getContent().stream().map(Booking::getId).toList());
        return page;
    }

    private void display(Model model, HttpServletRequest request, Map<String, String> params,
                         Page<?> page, AdminListSupport.Options options, String attribute, Object... statuses) {
        model.addAttribute(attribute, page.getContent());
        lists.present(model, page, params, options, request,
                java.util.Arrays.stream(statuses).map(Object::toString).toArray(String[]::new));
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
