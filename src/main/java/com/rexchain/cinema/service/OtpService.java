package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int SEND_COOLDOWN_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final String fromEmail;

    // Lưu trữ OTP tạm thời trong bộ nhớ: email -> OtpData
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttemptsByEmail = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> nextOtpSendAt = new ConcurrentHashMap<>();

    public record OtpData(String otp, RegisterRequest request, LocalDateTime expiresAt) {}

    public OtpService(JavaMailSender mailSender,
                      @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * Sinh mã OTP 6 chữ số ngẫu nhiên, lưu thông tin đăng ký và gửi email.
     */
    public void generateAndSendOtp(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        enforceSendCooldown(email);
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStorage.put(email, new OtpData(otp, request, expiresAt));
        log.info("Registration OTP generated; expiry at {}", expiresAt);

        sendEmail(email, request.fullName(), otp);
    }

    /**
     * Gửi lại mã OTP cho email đã có dữ liệu đăng ký.
     */
    public void resendOtp(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        OtpData existing = otpStorage.get(normalizedEmail);
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin đăng ký cho email này. Vui lòng đăng ký lại.");
        }

        enforceSendCooldown(normalizedEmail);
        String newOtp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStorage.put(normalizedEmail, new OtpData(newOtp, existing.request(), expiresAt));
        sendEmail(normalizedEmail, existing.request().fullName(), newOtp);
    }

    /**
     * Xác thực mã OTP. Nếu hợp lệ, trả về RegisterRequest và xóa OTP khỏi bộ nhớ.
     */
    public synchronized RegisterRequest verifyOtp(String email, String inputOtp) {
        return verifyOtpAt(email, inputOtp, LocalDateTime.now());
    }

    synchronized RegisterRequest verifyOtpAt(String email, String inputOtp, LocalDateTime now) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        OtpData data = otpStorage.get(normalizedEmail);

        if (data == null) {
            throw new IllegalArgumentException("Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng bấm gửi lại mã.");
        }

        if (!now.isBefore(data.expiresAt())) {
            otpStorage.remove(normalizedEmail);
            failedAttemptsByEmail.remove(normalizedEmail);
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng bấm 'Gửi lại mã OTP'.");
        }

        if (!isOtpCorrect(normalizedEmail, data, inputOtp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }

        // Xác thực thành công -> xóa dữ liệu OTP
        otpStorage.remove(normalizedEmail);
        return data.request();
    }

    /**
     * Gửi email OTP bằng template HTML chuẩn phong cách Rex Cinemas.
     */
    private boolean isOtpCorrect(String email, OtpData data, String inputOtp) {
        if (data.otp().equals(inputOtp == null ? "" : inputOtp.trim())) {
            failedAttemptsByEmail.remove(email);
            return true;
        }
        if (failedAttemptsByEmail.merge(email, 1, Integer::sum) >= MAX_FAILED_ATTEMPTS) {
            otpStorage.remove(email);
            failedAttemptsByEmail.remove(email);
        }
        return false;
    }

    private void enforceSendCooldown(String email) {
        LocalDateTime now = LocalDateTime.now();
        AtomicReference<Boolean> blocked = new AtomicReference<>(false);
        nextOtpSendAt.compute(email, (key, allowedAt) -> {
            if (allowedAt != null && now.isBefore(allowedAt)) {
                blocked.set(true);
                return allowedAt;
            }
            return now.plusSeconds(SEND_COOLDOWN_SECONDS);
        });
        if (blocked.get()) {
            throw new IllegalArgumentException("Please wait before requesting another OTP.");
        }
    }

    private void sendEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Rex Cinemas");
            helper.setTo(toEmail);
            helper.setSubject("[Rex Cinemas] Mã xác thực OTP đăng ký tài khoản");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Mã xác thực OTP - Rex Cinemas</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0b0d14; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #ffffff;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0b0d14; padding: 40px 10px;">
                        <tr>
                            <td align="center">
                                <table width="600" border="0" cellspacing="0" cellpadding="0" style="background: linear-gradient(180deg, #151b28 0%%, #0f131d 100%%); border-radius: 16px; border: 1px solid #28354d; overflow: hidden; box-shadow: 0 12px 40px rgba(0,0,0,0.6);">
                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 35px 20px 25px; border-bottom: 1px solid #222d42; background: radial-gradient(circle at 50%% 0%%, rgba(239,35,60,0.2), transparent 70%%);">
                                            <h1 style="margin: 0; color: #ffe500; font-size: 28px; font-weight: 900; letter-spacing: 2px;">REX CINEMAS</h1>
                                            <p style="margin: 5px 0 0; color: #8f9db5; font-size: 13px; letter-spacing: 1px; text-transform: uppercase;">Trải nghiệm điện ảnh đỉnh cao</p>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 35px 40px;">
                                            <p style="font-size: 16px; color: #e1e7f0; margin: 0 0 15px;">Xin chào <strong style="color: #ffe500;">%s</strong>,</p>
                                            <p style="font-size: 14px; line-height: 1.6; color: #b2bed2; margin: 0 0 25px;">
                                                Bạn đang thực hiện đăng ký tài khoản thành viên tại <strong>Rex Cinemas</strong>. Để hoàn tất kích hoạt tài khoản, vui lòng sử dụng mã xác thực OTP bên dưới:
                                            </p>
                                            
                                            <!-- OTP Box -->
                                            <div style="text-align: center; margin: 30px 0; padding: 22px; background: rgba(0,0,0,0.4); border-radius: 12px; border: 2px dashed #ffe500;">
                                                <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 2px; color: #8f9db5; display: block; margin-bottom: 8px;">MÃ XÁC THỰC OTP CỦA BẠN</span>
                                                <span style="font-family: monospace, 'Courier New', Courier; font-size: 38px; font-weight: 800; letter-spacing: 8px; color: #ffe500;">%s</span>
                                            </div>
                                            
                                            <p style="font-size: 13px; line-height: 1.6; color: #ff8595; margin: 0 0 15px;">
                                                ⏱️ Mã này có hiệu lực trong vòng <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.
                                            </p>
                                            <p style="font-size: 13px; line-height: 1.6; color: #78879e; margin: 25px 0 0; border-top: 1px solid #1c2638; padding-top: 20px;">
                                                Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email. Tài khoản của bạn sẽ không bị ảnh hưởng.
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 20px; background-color: #0a0d14; color: #5f6e85; font-size: 12px; border-top: 1px solid #1c2638;">
                                            © 2026 Rex Cinemas Chain. Mọi quyền được bảo lưu.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(fullName, otp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Registration OTP email sent");
        } catch (MessagingException e) {
            log.error("Failed to send registration OTP email ({})", e.getClass().getSimpleName());
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        } catch (Exception e) {
            log.error("Unexpected registration OTP email error ({})", e.getClass().getSimpleName());
            throw new IllegalStateException("Hệ thống gửi thư gặp sự cố. Vui lòng thử lại sau.");
        }
    }
}
