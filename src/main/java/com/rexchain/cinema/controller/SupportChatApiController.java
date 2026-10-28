package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.SupportMessageDto;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.SupportChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportChatApiController {

    private final SupportChatService chatService;
    private final CinemaRepository cinemas;
    private final UserRepository users;

    public SupportChatApiController(SupportChatService chatService,
                                    CinemaRepository cinemas,
                                    UserRepository users) {
        this.chatService = chatService;
        this.cinemas = cinemas;
        this.users = users;
    }

    public record SendMessageRequest(
            String sessionId,
            Long cinemaId,
            String message,
            String guestName,
            String guestEmail
    ) {}

    public record AiConsultRequest(
            String sessionId,
            String prompt,
            String guestName
    ) {}

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        User user = currentUser(auth);
        if (user != null) {
            res.put("authenticated", true);
            res.put("id", user.getId());
            res.put("name", user.getFullName());
            res.put("email", user.getEmail());
            res.put("favoriteCinemaId", user.getFavoriteCinema() != null ? user.getFavoriteCinema().getId() : null);
        } else {
            res.put("authenticated", false);
        }
        return res;
    }

    @GetMapping("/cinemas")
    public List<Map<String, Object>> getCinemas() {
        return cinemas.findByActiveTrueOrderByCityAscNameAsc().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    map.put("city", c.getCity());
                    return map;
                })
                .toList();
    }

    @GetMapping("/history")
    public List<SupportMessageDto> getHistory(@RequestParam String sessionId) {
        return chatService.getHistory(sessionId);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendCustomerMessage(@RequestBody SendMessageRequest req, Authentication auth) {
        try {
            User user = currentUser(auth);
            SupportMessageDto sent = chatService.sendCustomerMessage(
                    req.sessionId(), req.cinemaId(), req.message(), user, req.guestName(), req.guestEmail()
            );
            return ResponseEntity.ok(sent);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/ai-consult")
    public ResponseEntity<?> askAi(@RequestBody AiConsultRequest req, Authentication auth) {
        try {
            User user = currentUser(auth);
            SupportMessageDto aiReply = chatService.askAi(
                    req.sessionId(), req.prompt(), user, req.guestName()
            );
            return ResponseEntity.ok(aiReply);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return users.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }
}
