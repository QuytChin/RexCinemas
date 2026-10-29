package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.CheckInRequest;
import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

@Controller
@RequestMapping("/admin/checkin")
public class CheckInController {
    private final BookingService bookingService;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final AdminListSupport lists;

    public CheckInController(BookingService bookingService, BookingRepository bookings, UserRepository users, AdminListSupport lists) {
        this.bookingService = bookingService;
        this.bookings = bookings;
        this.users = users;
        this.lists = lists;
    }

    @GetMapping
    String page(@RequestParam Map<String, String> params, HttpServletRequest request, Authentication auth, Model model) {
        var options = new AdminListSupport.Options("bookingCode,user.email", null,
                new String[]{"checkedInAt", "createdAt", "id"},
                new String[]{"user", "showtime.movie", "showtime.auditorium.cinema"})
                .withLabels("Trạng thái", "Mã booking hoặc email khách")
                .withoutSortControls();
        User currentUser = (auth != null && auth.getName() != null)
                ? users.findByEmailIgnoreCase(auth.getName()).orElse(null)
                : null;
        if (currentUser == null) {
            throw new AccessDeniedException("Không xác định được tài khoản soát vé");
        }
        boolean cinemaScoped = currentUser.getRole() == Role.STAFF || currentUser.getRole() == Role.SUPERVISOR;
        if (cinemaScoped && (currentUser.getCinema() == null || currentUser.getCinema().getId() == null
                || !currentUser.getCinema().isActive())) {
            throw new AccessDeniedException("Tài khoản chưa được gán vào rạp đang hoạt động");
        }
        if (!cinemaScoped && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Tài khoản không có quyền xem lịch sử soát vé");
        }
        var page = lists.query(bookings, params, options, (root, query, cb) -> {
            var predicate = cb.isNotNull(root.get("checkedInAt"));
            if (cinemaScoped) {
                var cinemaId = currentUser.getCinema().getId();
                return cb.and(predicate, cb.equal(root.get("showtime").get("auditorium").get("cinema").get("id"), cinemaId));
            }
            return predicate;
        });
        model.addAttribute("recentCheckins", page.getContent());
        if (cinemaScoped) {
            model.addAttribute("operatorCinema", currentUser.getCinema());
        }
        lists.present(model, page, params, options, request);
        return "admin/checkin";
    }

    @PostMapping("/api/scan")
    @ResponseBody
    ResponseEntity<CheckInResult> scan(@Valid @RequestBody CheckInRequest request, Authentication auth) {
        CheckInResult result = bookingService.checkInTicket(request.code(), auth.getName());
        return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}
