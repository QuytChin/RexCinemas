package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.dto.VerifiedRegistration;
import com.rexchain.cinema.entity.OtpChallenge;
import com.rexchain.cinema.entity.OtpRateLimit;
import com.rexchain.cinema.repository.OtpChallengeRepository;
import com.rexchain.cinema.repository.OtpRateLimitRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static final int DEFAULT_OTP_EXPIRY_MINUTES = 5;
    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_MINUTES = 1;
    private static final int DEFAULT_SEND_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_IP_SEND_COOLDOWN_SECONDS = 20;
    private static final int DEFAULT_EMAIL_SENDS_PER_HOUR = 5;
    private static final int DEFAULT_IP_SENDS_PER_HOUR = 20;
    private static final int DEFAULT_IP_VERIFICATIONS_PER_HOUR = 60;
    private static final String REGISTRATION_PURPOSE = "REGISTRATION";
    private static final String SEND_PURPOSE = "REGISTRATION_SEND";
    private static final String VERIFY_PURPOSE = "REGISTRATION_VERIFY";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final OtpChallengeRepository challenges;
    private final OtpRateLimitRepository rateLimits;
    private final PasswordEncoder passwordEncoder;
    private final int otpExpiryMinutes;
    private final int maxFailedAttempts;
    private final int lockoutMinutes;
    private final int sendCooldownSeconds;
    private final int ipSendCooldownSeconds;
    private final int emailSendsPerHour;
    private final int ipSendsPerHour;
    private final int ipVerificationsPerHour;

    // Used only by the package-level compatibility constructor in unit tests.
    private final Map<String, MemoryChallenge> memoryChallenges = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> memoryNextSendAt = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> memoryIpNextSendAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> memoryFailedAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> memoryLockedUntil = new ConcurrentHashMap<>();

    private record MemoryChallenge(String otp, RegisterRequest request, LocalDateTime expiresAt) {}

    /** Compatibility constructor for unit tests; production uses the DB-backed constructor. */
    public OtpService(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromEmail) {
        this(mailSender, fromEmail, null, null, new BCryptPasswordEncoder(),
                DEFAULT_OTP_EXPIRY_MINUTES, DEFAULT_MAX_FAILED_ATTEMPTS, DEFAULT_LOCKOUT_MINUTES,
                DEFAULT_SEND_COOLDOWN_SECONDS, DEFAULT_IP_SEND_COOLDOWN_SECONDS,
                DEFAULT_EMAIL_SENDS_PER_HOUR, DEFAULT_IP_SENDS_PER_HOUR, DEFAULT_IP_VERIFICATIONS_PER_HOUR);
    }

    @Autowired
    public OtpService(JavaMailSender mailSender,
                      @Value("${spring.mail.username}") String fromEmail,
                      OtpChallengeRepository challenges,
                      OtpRateLimitRepository rateLimits,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.otp.expiry-minutes:5}") int otpExpiryMinutes,
                      @Value("${app.otp.max-failed-attempts:5}") int maxFailedAttempts,
                      @Value("${app.otp.lockout-minutes:1}") int lockoutMinutes,
                      @Value("${app.otp.email-cooldown-seconds:60}") int sendCooldownSeconds,
                      @Value("${app.otp.ip-cooldown-seconds:20}") int ipSendCooldownSeconds,
                      @Value("${app.otp.email-hourly-limit:5}") int emailSendsPerHour,
                      @Value("${app.otp.ip-hourly-limit:20}") int ipSendsPerHour,
                      @Value("${app.otp.ip-verification-hourly-limit:60}") int ipVerificationsPerHour) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.challenges = challenges;
        this.rateLimits = rateLimits;
        this.passwordEncoder = passwordEncoder;
        this.otpExpiryMinutes = requirePositive("app.otp.expiry-minutes", otpExpiryMinutes);
        this.maxFailedAttempts = requirePositive("app.otp.max-failed-attempts", maxFailedAttempts);
        this.lockoutMinutes = requirePositive("app.otp.lockout-minutes", lockoutMinutes);
        this.sendCooldownSeconds = requirePositive("app.otp.email-cooldown-seconds", sendCooldownSeconds);
        this.ipSendCooldownSeconds = requirePositive("app.otp.ip-cooldown-seconds", ipSendCooldownSeconds);
        this.emailSendsPerHour = requirePositive("app.otp.email-hourly-limit", emailSendsPerHour);
        this.ipSendsPerHour = requirePositive("app.otp.ip-hourly-limit", ipSendsPerHour);
        this.ipVerificationsPerHour = requirePositive("app.otp.ip-verification-hourly-limit", ipVerificationsPerHour);
    }

    @Scheduled(fixedDelayString = "${app.otp.cleanup-interval-ms:300000}")
    @Transactional
    public void removeExpiredChallenges() {
        if (challenges != null) {
            challenges.deleteByExpiresAtBefore(LocalDateTime.now());
        }
    }

    public void generateAndSendOtp(RegisterRequest request) {
        generateAndSendOtp(request, "unknown");
    }

    @Transactional
    public void generateAndSendOtp(RegisterRequest request, String clientIp) {
        if (challenges == null) {
            generateInMemory(request, clientIp);
            return;
        }
        String email = normalizeEmail(request.email());
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge existing = challenges.findByEmailAndPurpose(email, REGISTRATION_PURPOSE).orElse(null);
        rejectIfBefore(existing == null ? null : existing.getNextSendAt(), now);
        enforceRateLimit("email:" + email, SEND_PURPOSE, emailSendsPerHour, sendCooldownSeconds, now);
        enforceIpSendLimit(clientIp, now);

        String otp = newOtp();
        OtpChallenge challenge = existing == null ? new OtpChallenge() : existing;
        fillChallenge(challenge, request, email, clientIp, otp, now);
        challenges.save(challenge);
        sendEmail(email, request.fullName(), otp);
    }

    public void resendOtp(String email) {
        resendOtp(email, "unknown");
    }

    @Transactional
    public void resendOtp(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        if (challenges == null) {
            resendInMemory(normalizedEmail, clientIp);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge challenge = challenges.findByEmailAndPurpose(normalizedEmail, REGISTRATION_PURPOSE).orElseThrow(
                () -> new IllegalArgumentException("Yêu cầu OTP không tồn tại hoặc đã hết hạn. Vui lòng đăng ký lại."));
        rejectIfBefore(challenge.getNextSendAt(), now);
        enforceRateLimit("email:" + normalizedEmail, SEND_PURPOSE, emailSendsPerHour, sendCooldownSeconds, now);
        enforceIpSendLimit(clientIp, now);

        String newOtp = newOtp();
        challenge.setOtpSalt(randomSalt());
        challenge.setOtpHash(hashOtp(newOtp, challenge.getOtpSalt()));
        challenge.setExpiresAt(now.plusMinutes(otpExpiryMinutes));
        challenge.setNextSendAt(now.plusSeconds(sendCooldownSeconds));
        challenge.setFailedAttempts(0);
        challenge.setLockedUntil(null);
        challenge.setClientIp(safeIp(clientIp));
        challenges.save(challenge);
        sendEmail(normalizedEmail, challenge.getFullName(), newOtp);
    }

    public VerifiedRegistration verifyOtp(String email, String inputOtp) {
        return verifyOtp(email, inputOtp, "unknown");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalArgumentException.class)
    public VerifiedRegistration verifyOtp(String email, String inputOtp, String clientIp) {
        return verifyOtpAt(email, inputOtp, clientIp, LocalDateTime.now());
    }

    VerifiedRegistration verifyOtpAt(String email, String inputOtp, LocalDateTime now) {
        return verifyOtpAt(email, inputOtp, "unknown", now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalArgumentException.class)
    VerifiedRegistration verifyOtpAt(String email, String inputOtp, String clientIp, LocalDateTime now) {
        String normalizedEmail = normalizeEmail(email);
        if (challenges == null) {
            synchronized (memoryChallenges) {
                return verifyInMemory(normalizedEmail, inputOtp, now);
            }
        }
        String ip = safeIp(clientIp);
        if (!"unknown".equals(ip)) {
            enforceRateLimit("ip:" + ip, VERIFY_PURPOSE, ipVerificationsPerHour, 0, now);
        }

        OtpChallenge challenge = challenges.findByEmailAndPurpose(normalizedEmail, REGISTRATION_PURPOSE).orElseThrow(
                () -> new IllegalArgumentException("Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng gửi lại mã."));
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenges.delete(challenge);
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng gửi lại mã OTP.");
        }
        if (challenge.getLockedUntil() != null && now.isBefore(challenge.getLockedUntil())) {
            throw new OtpRateLimitException("OTP đang bị khóa tạm thời. Vui lòng thử lại sau.");
        }
        if (challenge.getLockedUntil() != null) {
            challenge.setLockedUntil(null);
            challenge.setFailedAttempts(0);
        }

        String normalizedOtp = inputOtp == null ? "" : inputOtp.trim();
        boolean correct = MessageDigest.isEqual(
                HexFormat.of().parseHex(hashOtp(normalizedOtp, challenge.getOtpSalt())),
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
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }

        VerifiedRegistration verified = new VerifiedRegistration(challenge.getFullName(), challenge.getEmail(),
                challenge.getPhone(), challenge.getGender(), challenge.getBirthDate(), challenge.getPasswordHash(),
                challenge.isTermsAccepted(), challenge.getUsername(), challenge.getNationalId(), challenge.getFavoriteCinemaId());
        challenges.delete(challenge);
        return verified;
    }

    private void generateInMemory(RegisterRequest request, String clientIp) {
        synchronized (memoryChallenges) {
            String email = normalizeEmail(request.email());
            LocalDateTime now = LocalDateTime.now();
            rejectIfBefore(memoryNextSendAt.get(email), now);
            String ip = safeIp(clientIp);
            if (!"unknown".equals(ip)) rejectIfBefore(memoryIpNextSendAt.get(ip), now);
            String otp = newOtp();
            memoryChallenges.put(email, new MemoryChallenge(otp, request, now.plusMinutes(otpExpiryMinutes)));
            memoryNextSendAt.put(email, now.plusSeconds(sendCooldownSeconds));
            if (!"unknown".equals(ip)) memoryIpNextSendAt.put(ip, now.plusSeconds(ipSendCooldownSeconds));
            sendEmail(email, request.fullName(), otp);
        }
    }

    private void resendInMemory(String email, String clientIp) {
        synchronized (memoryChallenges) {
            MemoryChallenge existing = memoryChallenges.get(email);
            if (existing == null) throw new IllegalArgumentException("Yêu cầu OTP không tồn tại hoặc đã hết hạn. Vui lòng đăng ký lại.");
            LocalDateTime now = LocalDateTime.now();
            rejectIfBefore(memoryNextSendAt.get(email), now);
            String ip = safeIp(clientIp);
            if (!"unknown".equals(ip)) rejectIfBefore(memoryIpNextSendAt.get(ip), now);
            String otp = newOtp();
            memoryChallenges.put(email, new MemoryChallenge(otp, existing.request(), now.plusMinutes(otpExpiryMinutes)));
            memoryNextSendAt.put(email, now.plusSeconds(sendCooldownSeconds));
            if (!"unknown".equals(ip)) memoryIpNextSendAt.put(ip, now.plusSeconds(ipSendCooldownSeconds));
            sendEmail(email, existing.request().fullName(), otp);
        }
    }

    private VerifiedRegistration verifyInMemory(String email, String inputOtp, LocalDateTime now) {
        MemoryChallenge data = memoryChallenges.get(email);
        if (data == null) throw new IllegalArgumentException("Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng gửi lại mã.");
        if (!now.isBefore(data.expiresAt())) {
            memoryChallenges.remove(email);
            memoryFailedAttempts.remove(email);
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng gửi lại mã OTP.");
        }
        LocalDateTime lockedUntil = memoryLockedUntil.get(email);
        if (lockedUntil != null && now.isBefore(lockedUntil)) throw new OtpRateLimitException("OTP đang bị khóa tạm thời. Vui lòng thử lại sau.");
        if (lockedUntil != null) {
            memoryLockedUntil.remove(email);
            memoryFailedAttempts.remove(email);
        }
        if (!data.otp().equals(inputOtp == null ? "" : inputOtp.trim())) {
            int failures = memoryFailedAttempts.merge(email, 1, Integer::sum);
            if (failures >= maxFailedAttempts) {
                memoryLockedUntil.put(email, now.plusMinutes(lockoutMinutes));
                throw new OtpRateLimitException("OTP đã sai quá số lần cho phép và bị khóa tạm thời.");
            }
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }
        memoryChallenges.remove(email);
        memoryFailedAttempts.remove(email);
        RegisterRequest request = data.request();
        String hash = passwordEncoder == null ? request.password() : passwordEncoder.encode(request.password());
        return new VerifiedRegistration(request.fullName(), request.email(), request.phone(), request.gender(), request.birthDate(),
                hash, request.termsAccepted(), request.username(), request.nationalId(), request.favoriteCinemaId());
    }

    private void fillChallenge(OtpChallenge challenge, RegisterRequest request, String email, String clientIp,
                               String otp, LocalDateTime now) {
        challenge.setEmail(email);
        challenge.setPurpose(REGISTRATION_PURPOSE);
        challenge.setOtpSalt(randomSalt());
        challenge.setOtpHash(hashOtp(otp, challenge.getOtpSalt()));
        challenge.setFullName(request.fullName().trim());
        challenge.setPhone(request.phone().trim());
        challenge.setGender(request.gender().trim());
        challenge.setBirthDate(request.birthDate());
        challenge.setPasswordHash(passwordEncoder.encode(request.password()));
        challenge.setTermsAccepted(request.termsAccepted());
        challenge.setUsername(request.username());
        challenge.setNationalId(request.nationalId());
        challenge.setFavoriteCinemaId(request.favoriteCinemaId());
        challenge.setExpiresAt(now.plusMinutes(otpExpiryMinutes));
        challenge.setFailedAttempts(0);
        challenge.setLockedUntil(null);
        challenge.setNextSendAt(now.plusSeconds(sendCooldownSeconds));
        challenge.setClientIp(safeIp(clientIp));
    }

    private void enforceIpSendLimit(String clientIp, LocalDateTime now) {
        String ip = safeIp(clientIp);
        if (!"unknown".equals(ip)) enforceRateLimit("ip:" + ip, SEND_PURPOSE, ipSendsPerHour, ipSendCooldownSeconds, now);
    }

    private void enforceRateLimit(String scopeKey, String purpose, int maxPerHour, int cooldownSeconds, LocalDateTime now) {
        OtpRateLimit limit = rateLimits.findByScopeKeyAndPurpose(scopeKey, purpose).orElseGet(() -> {
            OtpRateLimit created = new OtpRateLimit();
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
        if (limit.getNextAllowedAt() != null && now.isBefore(limit.getNextAllowedAt())) {
            throw new OtpRateLimitException("Vui lòng chờ trước khi yêu cầu OTP tiếp theo.");
        }
        limit.setWindowCount(limit.getWindowCount() + 1);
        limit.setNextAllowedAt(now.plusSeconds(cooldownSeconds));
        rateLimits.save(limit);
    }

    private void rejectIfBefore(LocalDateTime allowedAt, LocalDateTime now) {
        if (allowedAt != null && now.isBefore(allowedAt)) throw new OtpRateLimitException("Vui lòng chờ trước khi yêu cầu OTP tiếp theo.");
    }

    private String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(); }

    private String safeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return "unknown";
        String value = clientIp.trim();
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    private String newOtp() { return String.format("%06d", RANDOM.nextInt(1_000_000)); }

    private int requirePositive(String property, int value) {
        if (value <= 0) throw new IllegalStateException(property + " must be positive");
        return value;
    }

    private String randomSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    private String hashOtp(String otp, String salt) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((salt + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("OTP hashing is unavailable", ex);
        }
    }

    private void sendEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Rex Cinemas");
            helper.setTo(toEmail);
            helper.setSubject("[Rex Cinemas] M\u00e3 OTP x\u00e1c th\u1ef1c t\u00e0i kho\u1ea3n");
            helper.setText(OtpEmailContent.html(fullName, otp, otpExpiryMinutes, false), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Kh\u00f4ng th\u1ec3 g\u1eedi email x\u00e1c th\u1ef1c. Vui l\u00f2ng th\u1eed l\u1ea1i sau.");
        } catch (Exception e) {
            throw new IllegalStateException("H\u1ec7 th\u1ed1ng g\u1eedi th\u01b0 g\u1eb7p s\u1ef1 c\u1ed1. Vui l\u00f2ng th\u1eed l\u1ea1i sau.");
        }
    }
}
