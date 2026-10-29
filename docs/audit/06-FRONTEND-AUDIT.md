# 06 — KIỂM TOÁN FRONTEND

## Phạm vi đã đọc

| Nhóm | Đã đọc toàn văn | Chỉ tra cứu |
|---|---|---|
| Template (48) | `booking/seat-map.html`, `booking/bank-qr-waiting.html` (trước đó), `rewards/dashboard.html`, `admin/support.html` (một phần), `admin/showtimes.html` (một phần), `profile.html` (một phần), `fragments/header.html` (một phần) | 41 file còn lại: tra `th:action`, `th:href`, `th:utext`, `sec:authorize` |
| JS (17) | `booking.js`, `bank-qr-payment.js`, `chat-widget.js` (trước đó), `admin-support.js` (một phần), `checkin.js`, `auth-form.js`, `password-reset.js` | `admin-layout.js`, `header-nav.js`, `header-account.js`, `profile.js`, `image-fallback.js`, `trailer-modal.js`, `chevron-arrows.js`, `schedule-date-display.js` |
| CSS (1) | **Không đọc** (2.106 dòng) | — |

## Đối chiếu frontend ↔ backend (các luồng chính)

| Chức năng | Endpoint FE gọi | Có ở BE? | Khớp method/shape? |
|---|---|---|---|
| Giữ ghế | `POST /api/showtimes/{id}/hold` | Có | Có (`expiresAtEpochMillis`, `serverNowEpochMillis`) |
| Kiểm trạng thái giữ | `GET /api/showtimes/{id}/hold-status?seatIds&expiresAtEpochMillis` | Có | Có (`active`, `serverNowEpochMillis`) |
| Trả ghế | `POST /api/showtimes/{id}/release` | Có | Có |
| Báo giá | `POST /api/showtimes/{id}/quote` | Có | Có (`totalAmount`, `voucherDiscount`, `membershipDiscount`, `expectedPoints`) |
| Xác nhận | `POST /booking/confirm` (form) | Có | Có (`showtimeId`, `seatIds`, `paymentMethod`, `voucherCode`, `comboIds`, `comboQtys`) |
| Trạng thái thanh toán | `GET /api/payment-status/{bookingCode}` | Có | Có; FE đọc `booking.rebookUrl` — BE không trả field này nên FE luôn dùng `data-seat-map-url` (không lỗi) |
| Notifications | `POST /api/notifications/{id}/read`, `/read-all` | Có | Có |
| Hỗ trợ: ai | `POST /api/support/ai-consult` | Có | Có; guest phải dùng `guest_*` (đã khớp `chat-widget.js`) |
| Hỗ trợ: gửi | `POST /api/support/send` | Có | Có (401 khi chưa đăng nhập) |
| Hỗ trợ: admin | `/admin/support/api/{conversations,history,reply,mark-read}` | Có | Có |
| Check-in | `POST /admin/checkin/api/scan` | Có | Có (`code`) |
| Đăng ký OTP | `/auth/register/{send-otp,verify-otp,resend-otp}` | Có | Có (BE cũng map `/api/auth/...`) |
| Quên mật khẩu | `/api/auth/forgot-password/{send-otp,verify-otp,reset}` | Có | Có |
| Rewards redeem | `POST /rewards/redeem` (form) | Có | Có; `requestId` do JS sinh — **đã sửa** để không sinh dấu `.` |

## Lỗi frontend đã xác nhận và đã sửa

**REX-018** — `rewards/dashboard.html`: fallback `Math.random().toString(36)` sinh chuỗi chứa `.`, trong khi `RewardRedemptionService.normalizeRequestKey` chỉ nhận `[A-Za-z0-9_-]{16,80}` → trình duyệt không hỗ trợ `crypto.randomUUID` sẽ nhận 400 "Mã yêu cầu đổi điểm không hợp lệ". Đã sửa: lọc bỏ ký tự không hợp lệ và giới hạn 32 ký tự.

## Quan sát khác (chưa sửa)

| Vấn đề | Mức | Ghi chú |
|---|---|---|
| `admin/payment-settings.html`: "Thời hạn giữ chỗ" nay là trường chỉ-đọc 5 phút | — | Đã sửa theo REX-007 (phương án A): tránh admin hiểu nhầm là cấu hình được |
|---|---|---|
| `booking.js` dùng `hold-status` polling + countdown theo `serverNowEpochMillis` | — | Thiết kế tốt: mất mạng không tự kết luận hết hạn |
| `bank-qr-payment.js` không kết luận hết hạn khi lỗi mạng | — | Tốt |
| `chat-widget.js` lưu `sessionId` chỉ trong RAM | — | Tốt, giảm rò rỉ |
| Không thấy `th:utext` trong các template đã đọc | — | Giảm nguy cơ XSS thủ công |
| `admin-support.js` không kiểm `event.origin`, phụ thuộc SockJS | LOW | Đã được bù bằng kiểm quyền phía server (REX-001) |
| Form admin dùng `th:action` + CSRF ẩn | — | Có `_csrf` trong form rewards; các form khác Thymeleaf tự chèn khi dùng `th:action` |

## NOT VERIFIED

- Layout, responsive, tràn/chồng lấn, loading/empty/error state, accessibility (ARIA, contrast, keyboard) — **chưa kiểm chứng** vì không chạy giao diện và không đọc `app.css`.
- Không chạy Playwright: `tests/example.spec.js` chỉ test trang playwright.dev; `playwright.config.js` không có `baseURL`/`webServer`.