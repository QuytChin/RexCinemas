package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.SupportMessageDto;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.SupportChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportChatApiControllerTest {
    @Mock SupportChatService chatService;
    @Mock CinemaRepository cinemas;
    @Mock UserRepository users;
    SupportChatApiController controller;

    @BeforeEach
    void setUp() {
        controller = new SupportChatApiController(chatService, cinemas, users);
    }

    @Test
    void anonymousCallerCannotWriteIntoAnAccountSession() {
        ResponseEntity<?> response = controller.askAi(
                new SupportChatApiController.AiConsultRequest("user_5", "xin chào", "Khách"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chatService);
    }

    @Test
    void anonymousCallerCannotInjectIntoAnArbitrarySessionName() {
        ResponseEntity<?> response = controller.askAi(
                new SupportChatApiController.AiConsultRequest("system", "xin chào", "Khách"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chatService);
    }

    @Test
    void anonymousCallerKeepsItsOwnEphemeralAiChannel() {
        SupportMessageDto reply = new SupportMessageDto(1L, "guest_abc123", null, "Rex Cinemas",
                "AI", "Trợ lý Rex AI", "Chào bạn", true, "10:00 30/10/2026");
        when(chatService.askAi(eq("guest_abc123"), anyString(), isNull(), anyString())).thenReturn(reply);

        ResponseEntity<?> response = controller.askAi(
                new SupportChatApiController.AiConsultRequest("guest_abc123", "Có phim gì?", "Khách"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chatService).askAi(eq("guest_abc123"), anyString(), isNull(), anyString());
    }

    @Test
    void guestSessionIdIsGeneratedWhenTheClientOmitsIt() {
        SupportMessageDto reply = new SupportMessageDto(2L, "guest_ai", null, "Rex Cinemas",
                "AI", "Trợ lý Rex AI", "Chào bạn", true, "10:00 30/10/2026");
        when(chatService.askAi(eq("guest_ai"), anyString(), any(), anyString())).thenReturn(reply);

        ResponseEntity<?> response = controller.askAi(
                new SupportChatApiController.AiConsultRequest(null, "Có phim gì?", "Khách"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chatService).askAi(eq("guest_ai"), anyString(), isNull(), anyString());
    }
}
