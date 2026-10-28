package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.SupportMessageDto;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.SenderType;
import com.rexchain.cinema.entity.SupportMessage;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.SupportMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportChatService {

    private final SupportMessageRepository supportMessages;
    private final CinemaRepository cinemas;
    private final AiMovieConsultantService aiConsultant;
    private final SimpMessagingTemplate messagingTemplate;

    public SupportChatService(SupportMessageRepository supportMessages,
                              CinemaRepository cinemas,
                              AiMovieConsultantService aiConsultant,
                              SimpMessagingTemplate messagingTemplate) {
        this.supportMessages = supportMessages;
        this.cinemas = cinemas;
        this.aiConsultant = aiConsultant;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDto> getHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return List.of();
        return supportMessages.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(SupportMessageDto::fromEntity)
                .toList();
    }

    @Transactional
    public SupportMessageDto sendCustomerMessage(String sessionId, Long cinemaId, String messageText,
                                                 User customer, String guestName, String guestEmail) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("Session ID không hợp lệ");
        if (messageText == null || messageText.isBlank()) throw new IllegalArgumentException("Tin nhắn không được để trống");

        Cinema cinema = null;
        if (cinemaId != null) {
            cinema = cinemas.findById(cinemaId).orElse(null);
        }

        String customerName = customer != null ? customer.getFullName() : (guestName != null ? guestName : "Khách vãng lai");
        String customerEmail = customer != null ? customer.getEmail() : guestEmail;

        SupportMessage message = new SupportMessage(sessionId, customer, customerName, customerEmail,
                cinema, SenderType.CUSTOMER, messageText.trim());
        SupportMessage saved = supportMessages.save(message);
        SupportMessageDto dto = SupportMessageDto.fromEntity(saved);

        // Real-time broadcast
        broadcastMessage(dto);

        return dto;
    }

    @Transactional
    public SupportMessageDto sendAdminReply(String sessionId, String replyText, User adminUser) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("Session ID không hợp lệ");
        if (replyText == null || replyText.isBlank()) throw new IllegalArgumentException("Nội dung phản hồi không được để trống");

        // Tìm cinema từ tin nhắn trước đó trong cùng session
        List<SupportMessage> history = supportMessages.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Cinema cinema = null;
        for (SupportMessage sm : history) {
            if (sm.getCinema() != null) {
                cinema = sm.getCinema();
                break;
            }
        }
        if (cinema == null && adminUser != null && adminUser.getCinema() != null) {
            cinema = adminUser.getCinema();
        }

        SupportMessage reply = new SupportMessage(sessionId, null,
                adminUser != null ? adminUser.getFullName() : "Quản trị viên",
                adminUser != null ? adminUser.getEmail() : "admin@rexchain.vn",
                cinema, SenderType.STAFF, replyText.trim());
        reply.setRead(true);
        SupportMessage saved = supportMessages.save(reply);

        // Đánh dấu toàn bộ tin nhắn trước đó của khách trong session này là đã đọc
        supportMessages.markAsReadBySessionId(sessionId);

        SupportMessageDto dto = SupportMessageDto.fromEntity(saved);
        broadcastMessage(dto);

        return dto;
    }

    @Transactional
    public SupportMessageDto askAi(String sessionId, String prompt, User customer, String guestName) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("Session ID không hợp lệ");
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("Câu hỏi không được để trống");

        String customerName = customer != null ? customer.getFullName() : (guestName != null ? guestName : "Khách hàng");
        String customerEmail = customer != null ? customer.getEmail() : null;

        // 1. Lưu câu hỏi của khách hàng
        SupportMessage customerMsg = new SupportMessage(sessionId, customer, customerName, customerEmail,
                null, SenderType.CUSTOMER, prompt.trim());
        customerMsg.setRead(true);
        supportMessages.save(customerMsg);
        broadcastMessage(SupportMessageDto.fromEntity(customerMsg));

        // 2. Tạo câu trả lời từ AI
        String aiResponse = aiConsultant.consult(prompt);
        SupportMessage aiMsg = new SupportMessage(sessionId, null, "Trợ lý Rex AI",
                "ai@rexchain.vn", null, SenderType.AI, aiResponse);
        aiMsg.setRead(true);
        SupportMessage savedAi = supportMessages.save(aiMsg);
        SupportMessageDto aiDto = SupportMessageDto.fromEntity(savedAi);

        broadcastMessage(aiDto);
        return aiDto;
    }

    @Transactional
    public void markConversationAsRead(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            supportMessages.markAsReadBySessionId(sessionId);
        }
    }

    @Transactional(readOnly = true)
    public List<SupportMessageRepository.ConversationSummary> getAdminConversations(Long cinemaId) {
        return supportMessages.findRecentConversations(cinemaId);
    }

    private void broadcastMessage(SupportMessageDto dto) {
        // 1. Gửi về session riêng của khách hàng
        messagingTemplate.convertAndSend("/topic/support/session/" + dto.sessionId(), dto);

        // 2. Gửi về kênh của rạp tương ứng nếu có
        if (dto.cinemaId() != null) {
            messagingTemplate.convertAndSend("/topic/support/cinema/" + dto.cinemaId(), dto);
        }

        // 3. Gửi về kênh tổng quản trị
        messagingTemplate.convertAndSend("/topic/support/admin", dto);
    }
}
