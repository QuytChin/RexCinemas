package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.HoldRequest;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.BookingService;
import com.rexchain.cinema.service.QrCodeService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
public class BookingController {
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showSeats;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final BookingService service;
    private final ComboProductRepository combos;
    private final QrCodeService qrCodes;

    public BookingController(ShowtimeRepository showtimes, ShowtimeSeatRepository showSeats,
                             UserRepository users, BookingRepository bookings, BookingService service,
                             ComboProductRepository combos, QrCodeService qrCodes) {
        this.showtimes = showtimes;
        this.showSeats = showSeats;
        this.users = users;
        this.bookings = bookings;
        this.service = service;
        this.combos = combos;
        this.qrCodes = qrCodes;
    }

    @GetMapping("/booking/{showtimeId}")
    String booking(@PathVariable Long showtimeId, Model model) {
        Showtime showtime = showtimes.findById(showtimeId).orElseThrow();
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(java.time.LocalDateTime.now()))
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");
        loadBooking(model, showtimeId);
        return "booking/seat-map";
    }

    @PostMapping("/api/showtimes/{showtimeId}/hold")
    @ResponseBody
    ResponseEntity<?> hold(@PathVariable Long showtimeId, @RequestBody HoldRequest req, Authentication auth) {
        try {
            User user = current(auth);
            service.holdSeats(user.getId(), showtimeId, req.seatIds());
            return ResponseEntity.ok(Map.of("message", "Đã giữ ghế trong 5 phút"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/api/showtimes/{showtimeId}/release")
    @ResponseBody
    ResponseEntity<?> release(@PathVariable Long showtimeId, @RequestBody HoldRequest req, Authentication auth) {
        try {
            User user = current(auth);
            service.releaseOwnHolds(user.getId(), showtimeId, req.seatIds());
            return ResponseEntity.ok(Map.of("message", "Đã trả ghế"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/booking/confirm")
    String confirm(@RequestParam Long showtimeId,
                   @RequestParam String seatIds,
                   @RequestParam PaymentMethod paymentMethod,
                   @RequestParam(required = false) String voucherCode,
                   @RequestParam(required = false) List<Long> comboIds,
                   @RequestParam(required = false) List<Integer> comboQtys,
                   Authentication auth, Model model) {
        List<Long> ids = Arrays.stream(seatIds.split(","))
                .filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        User user = current(auth);
        try {
            Booking booking = service.confirm(user.getId(), showtimeId, ids, paymentMethod,
                    voucherCode, comboIds, comboQtys);
            if (booking.getStatus() == BookingStatus.PENDING)
                return "redirect:/payment/" + booking.getId() + "/start";
            return "redirect:/bookings/" + booking.getId() + "?success";
        } catch (Exception ex) {
            service.releaseOwnHolds(user.getId(), showtimeId, ids);
            model.addAttribute("error", ex.getMessage() + ". Ghế đã được trả lại, vui lòng chọn lại.");
            loadBooking(model, showtimeId);
            return "booking/seat-map";
        }
    }

    @GetMapping("/bookings")
    String mine(Authentication auth, Model model) {
        model.addAttribute("bookings", bookings.findByUserIdOrderByCreatedAtDesc(current(auth).getId()));
        return "booking/my-bookings";
    }

    @GetMapping("/bookings/{id}")
    String ticket(@PathVariable Long id, Authentication auth, Model model) {
        Booking booking = authorizedBooking(id, auth);
        model.addAttribute("booking", booking);
        return "booking/ticket";
    }

    @GetMapping(value = "/bookings/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    ResponseEntity<byte[]> ticketQr(@PathVariable Long id, Authentication auth) {
        authorizedBooking(id, auth);
        String payload = service.qrPayloadFor(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodes.png(payload, 420));
    }

    private Booking authorizedBooking(Long id, Authentication auth) {
        Booking booking = bookings.findById(id).orElseThrow();
        User user = current(auth);
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() == Role.CUSTOMER)
            throw new IllegalStateException("Bạn không có quyền xem vé này");
        return booking;
    }

    private void loadBooking(Model model, Long showtimeId) {
        model.addAttribute("showtime", showtimes.findById(showtimeId).orElseThrow());
        model.addAttribute("showSeats", showSeats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(showtimeId));
        model.addAttribute("combos", combos.findByActiveTrueOrderByIdAsc());
    }

    private User current(Authentication auth) {
        if (auth == null) throw new IllegalStateException("Bạn cần đăng nhập");
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
    }
}
