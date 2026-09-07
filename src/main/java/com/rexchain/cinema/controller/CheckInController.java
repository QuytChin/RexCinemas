package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.CheckInRequest;
import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.service.BookingService;
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

    public CheckInController(BookingService bookingService, BookingRepository bookings) {
        this.bookingService = bookingService;
        this.bookings = bookings;
    }

    @GetMapping
    String page(Model model) {
        model.addAttribute("recentCheckins", bookings.findTop20ByCheckedInAtIsNotNullOrderByCheckedInAtDesc());
        return "admin/checkin";
    }

    @PostMapping("/api/scan")
    @ResponseBody
    ResponseEntity<CheckInResult> scan(@RequestBody CheckInRequest request, Authentication auth) {
        CheckInResult result = bookingService.checkInTicket(request.code(), auth.getName());
        return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}
