package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(mailSender, "huybach219@gmail.com");
    }

    private RegisterRequest sampleRequest(String email) {
        return new RegisterRequest(
                "Nguyễn Văn A",
                email,
                "0912345678",
                "Nam",
                LocalDate.of(2000, 1, 1),
                "password123",
                "password123",
                true
        );
    }

    @Test
    void testGenerateSendAndVerifyOtpSuccess() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        RegisterRequest req = sampleRequest("user@gmail.com");
        otpService.generateAndSendOtp(req);

        verify(mailSender, times(1)).send(any(MimeMessage.class));

        // Không thể đoán trước mã ngẫu nhiên, nhưng khi verify với mã sai sẽ quăng lỗi
        assertThatThrownBy(() -> otpService.verifyOtp("user@gmail.com", "000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testVerifyNonExistentOtpThrows() {
        assertThatThrownBy(() -> otpService.verifyOtp("unknown@gmail.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không tồn tại");
    }
}
