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

@Controller
@RequestMapping("/admin/checkin")
public class CheckInController {
    private final BookingService bookingService;
    private final BookingRepository bookings;
    private final AdminListSupport lists;

    public CheckInController(BookingService bookingService, BookingRepository bookings, AdminListSupport lists) {
        this.bookingService = bookingService;
        this.bookings = bookings;
        this.lists = lists;
    }

    @GetMapping
    String page(@RequestParam Map<String, String> params, HttpServletRequest request, Model model) {
        var options = new AdminListSupport.Options("bookingCode,user.email", null,
                new String[]{"checkedInAt", "createdAt", "id"},
                new String[]{"user", "showtime.movie", "showtime.auditorium.cinema"})
                .withLabels("Trạng thái", "Mã booking hoặc email khách")
                .withoutSortControls();
        var page = lists.query(bookings, params, options, (root, query, cb) -> cb.isNotNull(root.get("checkedInAt")));
        model.addAttribute("recentCheckins", page.getContent());
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
