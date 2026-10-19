package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

@ControllerAdvice(annotations = Controller.class)
public class HeaderModelAdvice {
    private final ObjectProvider<UserRepository> users; private final ObjectProvider<CinemaRepository> cinemas; private final ObjectProvider<NotificationService> notifications;
    public HeaderModelAdvice(ObjectProvider<UserRepository> users, ObjectProvider<CinemaRepository> cinemas, ObjectProvider<NotificationService> notifications) { this.users = users; this.cinemas = cinemas; this.notifications = notifications; }

    @ModelAttribute
    public void header(Model model, Authentication authentication, HttpSession session) {
        CinemaRepository cinemaRepository = cinemas.getIfAvailable();
        UserRepository userRepository = users.getIfAvailable();
        NotificationService notificationService = notifications.getIfAvailable();
        if (cinemaRepository == null) return;
        List<Cinema> activeCinemas = cinemaRepository.findByActiveTrueOrderByCityAscNameAsc();
        model.addAttribute("headerCinemas", activeCinemas);
        Long selectedCinemaId = (Long) session.getAttribute("selectedCinemaId");
        Cinema selected = selectedCinemaId == null ? null : activeCinemas.stream().filter(cinema -> cinema.getId().equals(selectedCinemaId)).findFirst().orElse(null);
        if (selected == null) session.removeAttribute("selectedCinemaId");
        model.addAttribute("headerSelectedCinema", selected);
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) return;
        if (userRepository == null || notificationService == null) return;
        User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (user == null) return;
        model.addAttribute("headerUser", user);
        model.addAttribute("headerNotifications", notificationService.latest(user.getId(), 6));
        model.addAttribute("headerUnreadCount", notificationService.unreadCount(user.getId()));
    }
}
