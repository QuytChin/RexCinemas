package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.dto.VerifiedRegistration;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class OtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(mailSender, "noreply@example.invalid");
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
    void testGenerateSendAndVerifyOtpSuccess(CapturedOutput output) throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        RegisterRequest req = sampleRequest("user@gmail.com");
        otpService.generateAndSendOtp(req);

        String generatedOtp = sentOtp();
        assertThat(output.getAll().contains("user@gmail.com")).isFalse();
        assertThat(output.getAll().contains(generatedOtp)).isFalse();

        assertThatThrownBy(() -> otpService.resendOtp("user@gmail.com"))
                .isInstanceOf(IllegalArgumentException.class);
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

    @Test
    void expiredOtpCannotBeUsed() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("expiry@example.invalid");
        otpService.generateAndSendOtp(request);
        String code = sentOtp();

        assertThatThrownBy(() -> otpService.verifyOtpAt(request.email(), code, LocalDateTime.now().plusMinutes(6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fifthWrongAttemptConsumesOtp() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("attempts@example.invalid");
        otpService.generateAndSendOtp(request);
        String code = sentOtp();
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> otpService.verifyOtp(request.email(), wrongCode))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> otpService.verifyOtp(request.email(), code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lockoutExpiresBeforeOtpAndAllowsRetry() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("lockout@example.invalid");
        otpService.generateAndSendOtp(request);
        String code = sentOtp();
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> otpService.verifyOtp(request.email(), wrongCode))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> otpService.verifyOtp(request.email(), code))
                .isInstanceOf(com.rexchain.cinema.service.OtpRateLimitException.class);

        VerifiedRegistration verified = otpService.verifyOtpAt(request.email(), code, LocalDateTime.now().plusMinutes(2));
        assertThat(verified.email()).isEqualTo(request.email());
        assertThat(verified.passwordHash()).isNotEqualTo(request.password());
    }

    @Test
    void otpIsSingleUseAndBoundToTheRequestedEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("bound@example.invalid");
        otpService.generateAndSendOtp(request);
        String code = sentOtp();

        assertThatThrownBy(() -> otpService.verifyOtp("different@example.invalid", code))
                .isInstanceOf(IllegalArgumentException.class);
        VerifiedRegistration verified = otpService.verifyOtp(request.email(), code);
        assertThat(verified.email()).isEqualTo(request.email());
        assertThat(verified.passwordHash()).isNotEqualTo(request.password());
        assertThatThrownBy(() -> otpService.verifyOtp(request.email(), code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentVerificationAllowsOnlyOneConsumer() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("race@example.invalid");
        otpService.generateAndSendOtp(request);
        String code = sentOtp();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger customersCreated = new AtomicInteger();
        try {
            Future<Boolean> first = executor.submit(() -> verifyAfterGate(request.email(), code, ready, start, customersCreated));
            Future<Boolean> second = executor.submit(() -> verifyAfterGate(request.email(), code, ready, start, customersCreated));
            ready.await();
            start.countDown();
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
            assertThat(customersCreated).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentSendRequestsAreRateLimitedWithoutSendingDuplicateMail() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        RegisterRequest request = sampleRequest("concurrent-send@example.invalid");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> sendAfterGate(request, ready, start));
            Future<Boolean> second = executor.submit(() -> sendAfterGate(request, ready, start));
            ready.await();
            start.countDown();
            assertThat((first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean sendAfterGate(RegisterRequest request, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            otpService.generateAndSendOtp(request, "198.51.100.10");
            return true;
        } catch (IllegalArgumentException expected) {
            return false;
        }
    }

    private boolean verifyAfterGate(String email, String code, CountDownLatch ready, CountDownLatch start,
                                    AtomicInteger customersCreated) throws Exception {
        ready.countDown();
        start.await();
        try {
            otpService.verifyOtp(email, code);
            customersCreated.incrementAndGet();
            return true;
        } catch (IllegalArgumentException expected) {
            return false;
        }
    }

    private String sentOtp() throws Exception {
        ArgumentCaptor<MimeMessage> sentMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(sentMessage.capture());
        Matcher otpMatcher = Pattern.compile("(?s)font-size:34px.*?>(\\d{6})</p>")
                .matcher(messageText(sentMessage.getValue().getContent()));
        assertThat(otpMatcher.find()).isTrue();
        return otpMatcher.group(1);
    }

    private String messageText(Object content) throws Exception {
        if (content instanceof String text) return text;
        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                text.append(messageText(part.getContent()));
            }
            return text.toString();
        }
        return "";
    }
}
