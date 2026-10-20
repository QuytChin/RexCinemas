package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.OtpChallenge;
import com.rexchain.cinema.entity.PasswordResetChallenge;
import com.rexchain.cinema.repository.OtpChallengeRepository;
import com.rexchain.cinema.repository.PasswordResetChallengeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Opt-in local-MySQL integration coverage for committed OTP failures and lockouts. */
@SpringBootTest(properties = {
        "app.otp.max-failed-attempts=5",
        "app.otp.email-hourly-limit=20",
        "app.otp.ip-verification-hourly-limit=100"
})
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}",
        "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.otp.mysql-it", matches = "true")
class OtpFailurePersistenceMySqlIntegrationTest {
    private static final String CORRECT_OTP = "583104";
    private static final String WRONG_OTP = "000000";

    @Autowired OtpService otpService;
    @Autowired PasswordResetService passwordResetService;
    @Autowired OtpChallengeRepository otpChallenges;
    @Autowired PasswordResetChallengeRepository resetChallenges;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") String datasourceUrl;
    @MockBean JavaMailSender mailSender;

    private String registrationEmail;
    private String resetEmail;

    @BeforeEach
    void requireLocalRexDevelopmentDatabaseAndCreateChallenges() {
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::3306)?/rex_chain_cinema(?:\\?.*)?$"))
                .as("OTP integration test is restricted to the local Rex development database")
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("rex_chain_cinema");

        String suffix = UUID.randomUUID().toString().replace("-", "");
        registrationEmail = "rex_otp_reg_" + suffix + "@example.invalid";
        resetEmail = "rex_otp_reset_" + suffix + "@example.invalid";
        LocalDateTime now = LocalDateTime.now();

        OtpChallenge registration = new OtpChallenge();
        registration.setEmail(registrationEmail);
        registration.setPurpose("REGISTRATION");
        registration.setOtpSalt("registration-test-salt");
        registration.setOtpHash(hash(CORRECT_OTP, registration.getOtpSalt()));
        registration.setFullName("OTP integration test");
        registration.setPhone("0900000000");
        registration.setGender("OTHER");
        registration.setPasswordHash("not-used-by-verification");
        registration.setTermsAccepted(true);
        registration.setExpiresAt(now.plusMinutes(10));
        registration.setFailedAttempts(0);
        otpChallenges.saveAndFlush(registration);

        PasswordResetChallenge reset = new PasswordResetChallenge();
        reset.setEmail(resetEmail);
        reset.setPurpose(PasswordResetChallenge.PURPOSE);
        reset.setOtpSalt("reset-test-salt");
        reset.setOtpHash(hash(CORRECT_OTP, reset.getOtpSalt()));
        reset.setExpiresAt(now.plusMinutes(10));
        reset.setFailedAttempts(0);
        resetChallenges.saveAndFlush(reset);
    }

    @Test
    void registrationOtpFailuresAndLockoutAreCommittedBeforeErrorsReturn() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> otpService.verifyOtp(registrationEmail, WRONG_OTP, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM otp_challenges WHERE email = ? AND purpose = 'REGISTRATION'",
                Integer.class, registrationEmail)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT locked_until FROM otp_challenges WHERE email = ? AND purpose = 'REGISTRATION'",
                LocalDateTime.class, registrationEmail)).isAfter(LocalDateTime.now());
        assertThatThrownBy(() -> otpService.verifyOtp(registrationEmail, CORRECT_OTP, "unknown"))
                .isInstanceOf(OtpRateLimitException.class);
    }

    @Test
    void passwordResetOtpFailuresAndLockoutAreCommittedBeforeErrorsReturn() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> passwordResetService.verifyOtp(resetEmail, WRONG_OTP, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM password_reset_challenges WHERE email = ? AND purpose = 'RESET_PASSWORD'",
                Integer.class, resetEmail)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT locked_until FROM password_reset_challenges WHERE email = ? AND purpose = 'RESET_PASSWORD'",
                LocalDateTime.class, resetEmail)).isAfter(LocalDateTime.now());
        assertThatThrownBy(() -> passwordResetService.verifyOtp(resetEmail, CORRECT_OTP, "unknown"))
                .isInstanceOf(OtpRateLimitException.class);
    }

    @AfterEach
    void removeOnlyRowsCreatedByThisTest() {
        if (registrationEmail != null) {
            jdbc.update("DELETE FROM otp_challenges WHERE email = ? AND purpose = 'REGISTRATION'", registrationEmail);
        }
        if (resetEmail != null) {
            jdbc.update("DELETE FROM password_reset_challenges WHERE email = ? AND purpose = 'RESET_PASSWORD'", resetEmail);
            jdbc.update("DELETE FROM otp_rate_limits WHERE scope_key = ? AND purpose = 'RESET_PASSWORD_VERIFY'",
                    "email:" + resetEmail);
        }
    }

    private static String hash(String otp, String salt) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((salt + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
