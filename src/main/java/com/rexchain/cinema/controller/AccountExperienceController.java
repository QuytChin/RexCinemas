package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
public class AccountExperienceController {
    private final CinemaRepository cinemas; private final UserRepository users; private final NotificationService notifications;
    public AccountExperienceController(CinemaRepository cinemas, UserRepository users, NotificationService notifications) { this.cinemas = cinemas; this.users = users; this.notifications = notifications; }

    @PostMapping("/location")
    String selectLocation(@RequestParam(required = false) Long cinemaId, @RequestParam(required = false, defaultValue = "/schedule") String returnUrl, HttpSession session) {
        if (cinemaId == null) session.removeAttribute("selectedCinemaId");
        else { Cinema cinema = cinemas.findById(cinemaId).filter(Cinema::isActive).orElseThrow(() -> new IllegalArgumentException("Rạp đang chọn không hoạt động")); session.setAttribute("selectedCinemaId", cinema.getId()); }
        return "redirect:" + safeReturn(returnUrl);
    }

    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    ResponseEntity<?> markNotificationRead(@PathVariable Long id, Authentication authentication) {
        User user = current(authentication);
        if (!notifications.markRead(id, user.getId())) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("unreadCount", notifications.unreadCount(user.getId())));
    }

    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    ResponseEntity<?> markAllNotificationsRead(Authentication authentication) {
        User user = current(authentication);
        notifications.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", notifications.unreadCount(user.getId())));
    }

    @GetMapping("/notifications")
    String notifications(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        User user = current(authentication);
        model.addAttribute("notifications", notifications.page(user.getId(), page));
        return "notifications";
    }

    private User current(Authentication authentication) { if (authentication == null) throw new IllegalStateException("Bạn cần đăng nhập"); return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(); }
    private String safeReturn(String value) { return value != null && value.startsWith("/") && !value.startsWith("//") && !value.contains("\r") && !value.contains("\n") ? value : "/schedule"; }
}
