package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.PasswordResetChallenge;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.OtpRateLimitRepository;
import com.rexchain.cinema.repository.PasswordResetChallengeRepository;
import com.rexchain.cinema.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private static final String SEND_PURPOSE = "RESET_PASSWORD_SEND";
    private static final String VERIFY_PURPOSE = "RESET_PASSWORD_VERIFY";
    private static final int DEFAULT_OTP_EXPIRY_MINUTES = 5;
    private static final int DEFAULT_RESET_TOKEN_MINUTES = 10;
    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_MINUTES = 1;
    private static final int DEFAULT_SEND_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_IP_SEND_COOLDOWN_SECONDS = 20;
    private static final int DEFAULT_EMAIL_SENDS_PER_HOUR = 5;
    private static final int DEFAULT_IP_SENDS_PER_HOUR = 20;
    private static final int DEFAULT_IP_VERIFICATIONS_PER_HOUR = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final UserRepository users;
    private final PasswordResetChallengeRepository challenges;
    private final OtpRateLimitRepository rateLimits;
    private final PasswordEncoder passwordEncoder;
    private final int otpExpiryMinutes;
    private final int resetTokenMinutes;
    private final int maxFailedAttempts;
    private final int lockoutMinutes;
    private final int sendCooldownSeconds;
    private final int ipSendCooldownSeconds;
    private final int emailSendsPerHour;
    private final int ipSendsPerHour;
    private final int ipVerificationsPerHour;

    @Autowired
    public PasswordResetService(JavaMailSender mailSender,
                                @Value("${spring.mail.username}") String fromEmail,
                                UserRepository users,
                                PasswordResetChallengeRepository challenges,
                                OtpRateLimitRepository rateLimits,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.otp.expiry-minutes:5}") int otpExpiryMinutes,
                                @Value("${app.password-reset.token-minutes:10}") int resetTokenMinutes,
                                @Value("${app.otp.max-failed-attempts:5}") int maxFailedAttempts,
                                @Value("${app.otp.lockout-minutes:1}") int lockoutMinutes,
                                @Value("${app.otp.email-cooldown-seconds:60}") int sendCooldownSeconds,
                                @Value("${app.otp.ip-cooldown-seconds:20}") int ipSendCooldownSeconds,
                                @Value("${app.otp.email-hourly-limit:5}") int emailSendsPerHour,
                                @Value("${app.otp.ip-hourly-limit:20}") int ipSendsPerHour,
                                @Value("${app.otp.ip-verification-hourly-limit:60}") int ipVerificationsPerHour) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.users = users;
        this.challenges = challenges;
        this.rateLimits = rateLimits;
        this.passwordEncoder = passwordEncoder;
        this.otpExpiryMinutes = positive("app.otp.expiry-minutes", otpExpiryMinutes);
        this.resetTokenMinutes = positive("app.password-reset.token-minutes", resetTokenMinutes);
        this.maxFailedAttempts = positive("app.otp.max-failed-attempts", maxFailedAttempts);
        this.lockoutMinutes = positive("app.otp.lockout-minutes", lockoutMinutes);
        this.sendCooldownSeconds = positive("app.otp.email-cooldown-seconds", sendCooldownSeconds);
        this.ipSendCooldownSeconds = positive("app.otp.ip-cooldown-seconds", ipSendCooldownSeconds);
        this.emailSendsPerHour = positive("app.otp.email-hourly-limit", emailSendsPerHour);
        this.ipSendsPerHour = positive("app.otp.ip-hourly-limit", ipSendsPerHour);
        this.ipVerificationsPerHour = positive("app.otp.ip-verification-hourly-limit", ipVerificationsPerHour);
    }

    /** Always returns a generic public response. Account existence is intentionally not exposed. */
    @Transactional
    public void requestReset(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();
        enforceRateLimit("email:" + normalizedEmail, SEND_PURPOSE, emailSendsPerHour, sendCooldownSeconds, now);
        enforceIpRateLimit(clientIp, now);

        User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) return;

        PasswordResetChallenge existing = challenges.findByEmailAndPurpose(normalizedEmail, PasswordResetChallenge.PURPOSE).orElse(null);
        rejectIfBefore(existing == null ? null : existing.getNextSendAt(), now);

        String otp = newOtp();
        String salt = randomSalt();
        PasswordResetChallenge challenge = existing == null ? new PasswordResetChallenge() : existing;
        challenge.setEmail(normalizedEmail);
        challenge.setPurpose(PasswordResetChallenge.PURPOSE);
        challenge.setOtpHash(hash(otp, salt));
        challenge.setOtpSalt(salt);
        challenge.setExpiresAt(now.plusMinutes(otpExpiryMinutes));
        challenge.setFailedAttempts(0);
        challenge.setLockedUntil(null);
        challenge.setNextSendAt(now.plusSeconds(sendCooldownSeconds));
        challenge.setOtpVerified(false);
        challenge.setVerifiedAt(null);
        challenge.setResetTokenHash(null);
        challenge.setResetTokenExpiresAt(null);
        challenge.setClientIp(safeIp(clientIp));
        challenges.save(challenge);
        sendEmail(normalizedEmail, user.getFullName(), otp);
    }

    @Transactional
    public String verifyOtp(String email, String inputOtp, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();
        enforceRateLimit("email:" + normalizedEmail, VERIFY_PURPOSE, emailSendsPerHour, 0, now);
        String ip = safeIp(clientIp);
        if (!"unknown".equals(ip)) enforceRateLimit("ip:" + ip, VERIFY_PURPOSE, ipVerificationsPerHour, 0, now);

        PasswordResetChallenge challenge = challenges.findByEmailAndPurpose(normalizedEmail, PasswordResetChallenge.PURPOSE)
                .orElseThrow(this::invalidResetException);
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenges.delete(challenge);
            throw invalidResetException();
        }
        if (challenge.isOtpVerified()) throw invalidResetException();
        if (challenge.getLockedUntil() != null && now.isBefore(challenge.getLockedUntil())) {
            throw new OtpRateLimitException("OTP đang bị khóa tạm thời. Vui lòng thử lại sau.");
        }

        String normalizedOtp = inputOtp == null ? "" : inputOtp.trim();
        boolean correct = MessageDigest.isEqual(
                HexFormat.of().parseHex(hash(normalizedOtp, challenge.getOtpSalt())),
                HexFormat.of().parseHex(challenge.getOtpHash()));
        if (!correct) {
            int failures = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(failures);
            if (failures >= maxFailedAttempts) {
                challenge.setLockedUntil(now.plusMinutes(lockoutMinutes));
                challenges.save(challenge);
                throw new OtpRateLimitException("OTP đã sai quá số lần cho phép và bị khóa tạm thời.");
            }
            challenges.save(challenge);
            throw invalidResetException();
        }

        String resetToken = randomToken();
        challenge.setOtpVerified(true);
        challenge.setVerifiedAt(now);
        challenge.setResetTokenHash(hashToken(resetToken));
        challenge.setResetTokenExpiresAt(now.plusMinutes(resetTokenMinutes));
        challenges.save(challenge);
        return resetToken;
    }

    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72
                || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số");
        }
        if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("Xác nhận mật khẩu mới chưa khớp");

        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();
        PasswordResetChallenge challenge = challenges.findByEmailAndPurpose(normalizedEmail, PasswordResetChallenge.PURPOSE)
                .orElseThrow(this::invalidResetException);
        if (!challenge.isOtpVerified() || challenge.getResetTokenExpiresAt() == null
                || !now.isBefore(challenge.getResetTokenExpiresAt()) || challenge.getResetTokenHash() == null
                || !MessageDigest.isEqual(hex(hashToken(resetToken)), hex(challenge.getResetTokenHash()))) {
            throw invalidResetException();
        }

        User user = users.findByEmailIgnoreCaseForUpdate(normalizedEmail).orElseThrow(this::invalidResetException);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        users.save(user);
        challenges.delete(challenge);
    }

    @Scheduled(fixedDelayString = "${app.otp.cleanup-interval-ms:300000}")
    @Transactional
    public void removeExpiredChallenges() {
        challenges.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private void enforceIpRateLimit(String clientIp, LocalDateTime now) {
        String ip = safeIp(clientIp);
        if (!"unknown".equals(ip)) enforceRateLimit("ip:" + ip, SEND_PURPOSE, ipSendsPerHour, ipSendCooldownSeconds, now);
    }

    private void enforceRateLimit(String scopeKey, String purpose, int maxPerHour, int cooldownSeconds, LocalDateTime now) {
        var limit = rateLimits.findByScopeKeyAndPurpose(scopeKey, purpose).orElseGet(() -> {
            var created = new com.rexchain.cinema.entity.OtpRateLimit();
            created.setScopeKey(scopeKey);
            created.setPurpose(purpose);
            created.setWindowStartedAt(now);
            return created;
        });
        if (limit.getWindowStartedAt() == null || !now.isBefore(limit.getWindowStartedAt().plusHours(1))) {
            limit.setWindowStartedAt(now);
            limit.setWindowCount(0);
            limit.setNextAllowedAt(null);
        }
        if (limit.getWindowCount() >= maxPerHour) throw new OtpRateLimitException("Yêu cầu OTP đã vượt giới hạn. Vui lòng thử lại sau.");
        if (limit.getNextAllowedAt() != null && now.isBefore(limit.getNextAllowedAt()))
            throw new OtpRateLimitException("Vui lòng chờ trước khi yêu cầu OTP tiếp theo.");
        limit.setWindowCount(limit.getWindowCount() + 1);
        limit.setNextAllowedAt(now.plusSeconds(cooldownSeconds));
        rateLimits.save(limit);
    }

    private void sendEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Rex Cinemas");
            helper.setTo(toEmail);
            helper.setSubject("[Rex Cinemas] Mã OTP khôi phục mật khẩu");
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;color:#172033;max-width:560px;margin:auto">
                      <h2 style="color:#d7a900">Khôi phục mật khẩu Rex Cinemas</h2>
                      <p>Xin chào %s,</p>
                      <p>Mã OTP khôi phục mật khẩu của bạn là:</p>
                      <p style="font-size:34px;font-weight:800;letter-spacing:8px;color:#d7a900">%s</p>
                      <p>Mã có hiệu lực trong %d phút và chỉ dùng một lần. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                    </div>
                    """.formatted(fullName == null ? "bạn" : fullName, otp, otpExpiryMinutes), true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể gửi email OTP lúc này", ex);
        }
    }

    private IllegalArgumentException invalidResetException() {
        return new IllegalArgumentException("Mã khôi phục không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu lại.");
    }

    private String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(); }
    private String safeIp(String ip) { if (ip == null || ip.isBlank()) return "unknown"; return ip.trim().length() > 64 ? ip.trim().substring(0, 64) : ip.trim(); }
    private String newOtp() { return String.format("%06d", RANDOM.nextInt(1_000_000)); }
    private String randomSalt() { byte[] bytes = new byte[16]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    private String randomToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hashToken(String token) { return hash(token == null ? "" : token, "RESET_TOKEN"); }
    private String hash(String value, String salt) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + ":" + value).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }
    private byte[] hex(String value) { return HexFormat.of().parseHex(value); }
    private void rejectIfBefore(LocalDateTime allowedAt, LocalDateTime now) { if (allowedAt != null && now.isBefore(allowedAt)) throw new OtpRateLimitException("Vui lòng chờ trước khi yêu cầu OTP tiếp theo."); }
    private int positive(String name, int value) { if (value <= 0) throw new IllegalStateException(name + " must be positive"); return value; }
}
