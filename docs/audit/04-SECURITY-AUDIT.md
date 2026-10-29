# 04 — KIỂM TOÁN BẢO MẬT (OWASP)

Không ghi giá trị thật của bí mật, token hay mật khẩu trong tài liệu này.

## Điểm mạnh đã xác minh

| Kiểm soát | Bằng chứng |
|---|---|
| Mật khẩu băm BCrypt | `SecurityConfig.passwordEncoder()` |
| Chống brute-force OTP | `OtpService.enforceRateLimit` (email + IP, cooldown, giới hạn giờ), khóa tạm sau N lần sai |
| Chống brute-force đổi mật khẩu | `PasswordChangeRateLimiter` (5 lần / 15 phút / user) |
| Thu hồi JWT | `revoked_access_tokens` + `credential_version` trong token |
| Cookie | `HttpOnly`, `SameSite=Lax`, `Secure` theo cấu hình |
| CSRF | `CookieCsrfTokenRepository` + `CsrfCookieFilter` (materialize token) |
| CSP / Referrer | `SecurityConfig.headers(...)` |
| Bí mật bắt buộc khi khởi động | `RequiredSecretsValidator` (chỉ in tên property) |
| Webhook | Apikey so sánh constant-time; HMAC-SHA256 `timestamp + "." + body`; tolerance 300s; chống trùng bằng UNIQUE |
| Upload | `ImageUploadValidator`: magic-byte, MIME khai báo, extension, giải mã thật, giới hạn byte/pixel/kích thước, tên file chặn `..`, `/`, `\`, `\0` |
| SQL Injection | Không có `nativeQuery` nhận tham số chuỗi ghép từ input; `PaymentTransactionRepository` dùng `?` + escape `LIKE` |
| Open redirect | `safeReturnUrl` / `safeReturn` chỉ nhận path nội bộ, chặn `//`, `\r`, `\n` |
| XSS | Thymeleaf `th:text` mặc định escape; `th:utext` không thấy trong các template đã đọc |
| Prod hardening | `SchemaSafetyGuard`: prod bắt buộc `ddl-auto=validate`, `sql.init.mode=never`, `seed.enabled=false` |
| Actuator | chỉ expose `health`, `show-details=never` |
| IDOR (vé, booking, hội thoại) | `authorizedBooking`, kiểm chủ sở hữu ở Payment/Booking, `requireConversationAccess` |

## Lỗ hổng đã xác nhận và đã sửa

### REX-001 — WebSocket không kiểm quyền subscribe (HIGH) — ĐÃ SỬA
- Trước: `ALLOWED_TOPIC_REGEX = ^/topic/(showtimes/\d+/seats|support/.*)$`; không xác thực STOMP; `sessionId = "user_" + id` (ID tuần tự).
- Hậu quả: bất kỳ ai mở trang đều subscribe được `/topic/support/session/user_N`, `/topic/support/admin`, `/topic/support/cinema/{id}` → đọc realtime toàn bộ hội thoại (tên, email, nội dung).
- Sau: regex chặt; `SUBSCRIBE` được kiểm theo `Principal` từ handshake:
  - `/topic/support/session/user_N` chỉ chủ tài khoản N.
  - `/topic/support/admin` chỉ ADMIN.
  - `/topic/support/cinema/{id}` chỉ ADMIN hoặc SUPERVISOR đúng rạp.
  - `/topic/support/session/guest_*` vẫn mở (chỉ chứa câu trả lời AI, session nằm trong RAM trình duyệt).
  - `/topic/showtimes/{id}/seats` vẫn public (dữ liệu ghế công khai).

### REX-004 — Ghi chéo hội thoại hỗ trợ (HIGH) — ĐÃ SỬA
- Trước: `askAi` nhận `sessionId` tuỳ ý từ client khi chưa đăng nhập.
- Sau: guest chỉ nhận `guest_[A-Za-z0-9_-]{1,59}`; mọi giá trị khác (bao gồm `user_*`, `system`, ...) → 400.

### REX-008 — Bỏ qua CSRF cho mọi header Bearer (MEDIUM) — ĐÃ SỬA
- Trước: chỉ cần header bắt đầu bằng `Bearer ` là bỏ CSRF.
- Sau: chỉ bỏ khi token **hợp lệ chữ ký và chưa bị thu hồi**.

## Rủi ro còn lại (chưa sửa)

| Mã | Mức | Nội dung |
|---|---|---|
| REX-009 | MEDIUM | `/api/auth/login` trả JWT trong body → giảm giá trị HttpOnly; bỏ qua cờ `remember`. |
| REX-022 | LOW | Không có rate limit cho đăng nhập (chỉ OTP có) và `/api/support/ai-consult`. |
| REX-015 | LOW | Mật khẩu đăng ký tối thiểu 6 ký tự, không yêu cầu độ phức tạp (đổi mật khẩu yêu cầu 8 + chữ + số). |
| REX-010 | LOW | `GET /bookings/{id}` sinh QR token; `GET /api/payment-status/{code}` có thể expire booking (side-effect trên GET). |
| — | INFO | `/api/support/**` là public; sau khi sửa, chỉ còn `/me`, `/cinemas`, `/history` (401 khi chưa đăng nhập) và `/ai-consult` (guest giới hạn `guest_*`). |
| — | INFO | `CINEMA_MANAGER` vẫn tồn tại trong enum nhưng không được cấp mới và không có quyền đặc biệt trong `SecurityConfig`. |

## Kiểm tra phụ thuộc (dependency)

Không chạy `mvn dependency-check` trong phiên này (không có plugin, không cài thêm). Đánh giá CVE của thư viện = **NOT VERIFIED**.