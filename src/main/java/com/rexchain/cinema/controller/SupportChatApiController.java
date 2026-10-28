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
    public ResponseEntity<?> getHistory(@RequestParam(required = false) String sessionId, Authentication auth) {
        User user = currentUser(auth);
        if (user == null) {
            // Chưa đăng nhập -> Trả về 401 Unauthorized (danh sách rỗng) để bảo mật tuyệt đối
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(List.of());
        }
        // Luôn ép buộc session ID theo user ID từ phiên đăng nhập, tuyệt đối không cho phép client giả mạo
        String userSessionId = "user_" + user.getId();
        return ResponseEntity.ok(chatService.getHistory(userSessionId));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendCustomerMessage(@RequestBody SendMessageRequest req, Authentication auth) {
        try {
            User user = currentUser(auth);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Vui lòng đăng nhập để gửi tin nhắn hỗ trợ đến rạp!"));
            }
            // Khách hàng gửi tin nhắn bắt buộc dùng session gắn liền với tài khoản của chính mình
            String userSessionId = "user_" + user.getId();
            SupportMessageDto sent = chatService.sendCustomerMessage(
                    userSessionId, req.cinemaId(), req.message(), user, user.getFullName(), user.getEmail()
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
            String targetSessionId = user != null ? ("user_" + user.getId()) : (req.sessionId() != null && !req.sessionId().isBlank() ? req.sessionId() : "guest_ai");
            String senderName = user != null ? user.getFullName() : (req.guestName() != null ? req.guestName() : "Khách hàng");
            SupportMessageDto aiReply = chatService.askAi(
                    targetSessionId, req.prompt(), user, senderName
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
