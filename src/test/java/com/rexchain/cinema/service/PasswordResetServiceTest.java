package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.OtpRateLimit;
import com.rexchain.cinema.entity.PasswordResetChallenge;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.OtpRateLimitRepository;
import com.rexchain.cinema.repository.PasswordResetChallengeRepository;
import com.rexchain.cinema.repository.UserRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock JavaMailSender mailSender;
    @Mock UserRepository users;
    @Mock PasswordResetChallengeRepository challenges;
    @Mock OtpRateLimitRepository rateLimits;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(mailSender, "noreply@example.invalid", users, challenges, rateLimits,
                encoder, 5, 10, 5, 1, 60, 20, 5, 20, 60);
        lenient().when(rateLimits.findByScopeKeyAndPurpose(anyString(), anyString())).thenAnswer(invocation -> {
            OtpRateLimit limit = new OtpRateLimit();
            limit.setScopeKey(invocation.getArgument(0));
            limit.setPurpose(invocation.getArgument(1));
            limit.setWindowStartedAt(LocalDateTime.now());
            return Optional.of(limit);
        });
    }

    @Test
    void unknownEmailDoesNotCreateChallengeOrSendMail() {
        when(users.findByEmailIgnoreCase("unknown@example.invalid")).thenReturn(Optional.empty());

        service.requestReset("unknown@example.invalid", "198.51.100.10");

        verify(challenges, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void resetUsesSeparatePurposeOtpAndOneTimeResetProof() throws Exception {
        User user = new User();
        user.setFullName("Test Member");
        user.setEmail("reset@example.invalid");
        when(users.findByEmailIgnoreCase("reset@example.invalid")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        service.requestReset("reset@example.invalid", "198.51.100.10");
        ArgumentCaptor<PasswordResetChallenge> saved = ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challenges).save(saved.capture());
        PasswordResetChallenge challenge = saved.getValue();
        assertThat(challenge.getPurpose()).isEqualTo(PasswordResetChallenge.PURPOSE);
        assertThat(challenge.getOtpHash()).isNotBlank();
        assertThat(challenge.getOtpSalt()).isNotBlank();

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        String otp = extractOtp(message.getValue());
        when(challenges.findByEmailAndPurpose("reset@example.invalid", PasswordResetChallenge.PURPOSE))
                .thenReturn(Optional.of(challenge));

        String resetToken = service.verifyOtp("reset@example.invalid", otp, "198.51.100.10");

        assertThat(resetToken).isNotBlank();
        assertThat(challenge.isOtpVerified()).isTrue();
        assertThat(challenge.getResetTokenHash()).isNotEqualTo(resetToken);
        assertThatThrownBy(() -> service.verifyOtp("reset@example.invalid", otp, "198.51.100.10"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrongOrExpiredResetOtpCannotChangePassword() {
        PasswordResetChallenge challenge = challenge("reset@example.invalid", "654321", LocalDateTime.now().plusMinutes(5));
        when(challenges.findByEmailAndPurpose("reset@example.invalid", PasswordResetChallenge.PURPOSE))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.verifyOtp("reset@example.invalid", "000000", "unknown"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).findByEmailIgnoreCaseForUpdate(anyString());

        PasswordResetChallenge expired = challenge("expired@example.invalid", "654321", LocalDateTime.now().minusSeconds(1));
        when(challenges.findByEmailAndPurpose("expired@example.invalid", PasswordResetChallenge.PURPOSE))
                .thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verifyOtp("expired@example.invalid", "654321", "unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void successfulResetUsesBcryptAndIncrementsCredentialVersion() throws Exception {
        String resetToken = "reset-proof-token";
        PasswordResetChallenge challenge = challenge("reset@example.invalid", "654321", LocalDateTime.now().plusMinutes(5));
        challenge.setOtpVerified(true);
        challenge.setVerifiedAt(LocalDateTime.now());
        challenge.setResetTokenHash(sha256("RESET_TOKEN:" + resetToken));
        challenge.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        User user = new User();
        user.setEmail("reset@example.invalid");
        user.setPassword(encoder.encode("OldPassword123"));
        user.setCredentialVersion(4);
        when(challenges.findByEmailAndPurpose("reset@example.invalid", PasswordResetChallenge.PURPOSE))
                .thenReturn(Optional.of(challenge));
        when(users.findByEmailIgnoreCaseForUpdate("reset@example.invalid")).thenReturn(Optional.of(user));

        service.resetPassword("reset@example.invalid", resetToken, "NewPassword123", "NewPassword123");

        assertThat(encoder.matches("NewPassword123", user.getPassword())).isTrue();
        assertThat(encoder.matches("OldPassword123", user.getPassword())).isFalse();
        assertThat(user.getCredentialVersion()).isEqualTo(5);
        verify(challenges).delete(challenge);
    }

    private PasswordResetChallenge challenge(String email, String otp, LocalDateTime expiresAt) {
        PasswordResetChallenge challenge = new PasswordResetChallenge();
        challenge.setEmail(email);
        challenge.setPurpose(PasswordResetChallenge.PURPOSE);
        challenge.setOtpSalt("0123456789abcdef0123456789abcdef");
        challenge.setOtpHash(sha256(challenge.getOtpSalt() + ":" + otp));
        challenge.setExpiresAt(expiresAt);
        return challenge;
    }

    private String extractOtp(MimeMessage message) throws Exception {
        Matcher matcher = Pattern.compile("(?s)font-size:34px.*?>(\\d{6})</p>").matcher(messageText(message.getContent()));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String messageText(Object content) throws Exception {
        if (content instanceof String text) return text;
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                result.append(messageText(part.getContent()));
            }
            return result.toString();
        }
        return "";
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
