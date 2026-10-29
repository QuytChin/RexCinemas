package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.SupportMessageDto;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.SupportMessageRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.SupportChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/support")
public class AdminSupportController {

    private final SupportChatService chatService;
    private final CinemaRepository cinemas;
    private final UserRepository users;

    public AdminSupportController(SupportChatService chatService,
                                  CinemaRepository cinemas,
                                  UserRepository users) {
        this.chatService = chatService;
        this.cinemas = cinemas;
        this.users = users;
    }

    public record AdminReplyRequest(
            String sessionId,
            String message
    ) {}

    public record MarkReadRequest(
            String sessionId
    ) {}

    @GetMapping
    public String supportDashboard(@RequestParam(required = false) Long cinemaId,
                                   Authentication auth,
                                   Model model) {
        User currentUser = currentUser(auth);
        requireAssignedCinema(currentUser);

        // IDOR protection: Supervisor only views their assigned cinema
        if (currentUser != null && currentUser.getRole() == Role.SUPERVISOR) {
            if (currentUser.getCinema() != null) {
                cinemaId = currentUser.getCinema().getId();
                model.addAttribute("lockedCinema", currentUser.getCinema());
            } else {
                cinemaId = -1L;
            }
        }

        List<SupportMessageRepository.ConversationSummary> conversations = chatService.getAdminConversations(cinemaId);
        List<Cinema> activeCinemas = cinemas.findByActiveTrueOrderByCityAscNameAsc();

        model.addAttribute("conversations", conversations);
        model.addAttribute("cinemas", activeCinemas);
        model.addAttribute("selectedCinemaId", cinemaId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("adminActive", "support");
        return "admin/support";
    }

    @GetMapping("/api/conversations")
    @ResponseBody
    public List<SupportMessageRepository.ConversationSummary> getConversations(
            @RequestParam(required = false) Long cinemaId,
            Authentication auth) {
        User currentUser = currentUser(auth);
        requireAssignedCinema(currentUser);
        if (currentUser != null && currentUser.getRole() == Role.SUPERVISOR) {
            cinemaId = currentUser.getCinema() != null ? currentUser.getCinema().getId() : -1L;
        }
        return chatService.getAdminConversations(cinemaId);
    }

    @GetMapping("/api/history/{sessionId}")
    @ResponseBody
    public List<SupportMessageDto> getSessionHistory(@PathVariable String sessionId, Authentication auth) {
        requireConversationAccess(currentUser(auth), sessionId);
        return chatService.getHistory(sessionId);
    }

    @PostMapping("/api/reply")
    @ResponseBody
    public ResponseEntity<?> sendReply(@RequestBody AdminReplyRequest req, Authentication auth) {
        try {
            User currentUser = currentUser(auth);
            requireConversationAccess(currentUser, req.sessionId());
            SupportMessageDto replyDto = chatService.sendAdminReply(req.sessionId(), req.message(), currentUser);
            return ResponseEntity.ok(replyDto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/api/mark-read")
    @ResponseBody
    public ResponseEntity<?> markRead(@RequestBody MarkReadRequest req, Authentication auth) {
        requireConversationAccess(currentUser(auth), req.sessionId());
        chatService.markConversationAsRead(req.sessionId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null)
            throw new AccessDeniedException("Không xác định được nhân viên hỗ trợ");
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản nhân viên không tồn tại"));
    }

    private void requireAssignedCinema(User user) {
        if (user != null && user.getRole() == Role.SUPERVISOR && user.getCinema() == null)
            throw new AccessDeniedException("Giám sát viên chưa được gán rạp");
    }

    private void requireConversationAccess(User user, String sessionId) {
        requireAssignedCinema(user);
        if (user == null) throw new AccessDeniedException("Không xác định được nhân viên hỗ trợ");
        if (user.getRole() != Role.SUPERVISOR) return;
        Long cinemaId = user.getCinema().getId();
        List<SupportMessageDto> history = chatService.getHistory(sessionId);
        boolean matches = history.stream().anyMatch(message -> cinemaId.equals(message.cinemaId()));
        boolean otherCinema = history.stream().anyMatch(message -> message.cinemaId() != null
                && !cinemaId.equals(message.cinemaId()));
        if (!matches || otherCinema) throw new AccessDeniedException("Hội thoại không thuộc rạp được giao");
    }
}
