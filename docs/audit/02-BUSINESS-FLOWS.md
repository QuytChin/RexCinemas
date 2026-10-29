# 02 — LUỒNG NGHIỆP VỤ (truy vết Frontend → Controller → Service → Repository → DB)

## 1. Đăng ký (bắt buộc OTP)

`auth/register.html` + `auth-form.js`
→ `POST /auth/register/send-otp` → `AuthController.sendOtp` → `AuthService.validateRegistrationData` + `OtpService.generateAndSendOtp` → `otp_challenges` + `otp_rate_limits`, gửi SMTP.
→ `POST /auth/register/verify-otp` → `OtpService.verifyOtp` (`REQUIRES_NEW`, đếm lần sai, khóa tạm) → trả `VerifiedRegistration` (hash BCrypt) → `AuthService.createUserAfterOtp` → `app_users`.
`POST /auth/register` (form thuần) **cố tình** trả lỗi yêu cầu OTP. Password policy 6–72 ký tự.

## 2. Đăng nhập / đăng xuất

`POST /auth/login` → `AuthService.login` (identifier: email/username/phone qua `findAllByLoginIdentifier`) → JWT + cookie `access_token` (HttpOnly, SameSite=Lax).
`POST /auth/logout` → `AccessTokenRevocationService.revoke` (SHA-256 fingerprint) + clear cookie.
`/api/auth/login` trả thêm token trong body (ghi nhận rủi ro REX-009).

## 3. Quên mật khẩu

`password-reset.js` → `/api/auth/forgot-password/send-otp` (email enumeration được chặn: luôn trả thông điệp chung) → `verify-otp` (sinh reset token 10 phút) → `/reset` → `PasswordResetService.resetPassword` (khóa user, tăng `credential_version` để vô hiệu JWT cũ).

## 4. Giữ ghế 5 phút

`booking/seat-map.html` + `booking.js`
→ `POST /api/showtimes/{id}/hold` → `BookingController.hold` → `BookingService.holdSeats` → `SeatReservationService.holdSeats`:
1. `checkoutExpiresAt()`: một `seat_hold_sessions` duy nhất cho `(user, showtime)` = `now + 5'`; đổi ghế không được gia hạn; chặn nếu đã có booking PENDING cùng deadline.
2. Khóa bi quan **toàn bộ** seat-map (`findByShowtimeIdForUpdate`) rồi chạy luật Orphan Seat.
3. `showtime_seats.status = HELD`, `hold_until = deadline`, broadcast `HELD` **sau commit**.
→ `GET /api/showtimes/{id}/hold-status` đối chiếu deadline + `serverNowEpochMillis`.
→ `POST /api/showtimes/{id}/release` trả ghế khi khách quay lại.

## 5. Xác nhận booking

`POST /booking/confirm` (hoặc `POST /api/bookings/checkout`)
→ `BookingService.confirm`: chỉ nhận `BANK_QR`; khóa từng ghế (`findByIdInForUpdate`); kiểm `HELD` + đúng chủ + `holdUntil` còn hạn; tính lại toàn bộ tiền ở server; tạo `bookings` PENDING với `expires_at = min(holdUntil)`; tạo `booking_seats`, `booking_combos`; ghi `bank_transfer_snapshots`; broadcast `HELD`.
Nhánh lỗi ở controller gọi `releaseUnconfirmedHolds` (chỉ trả ghế khi **không** có booking PENDING) — đã sửa REX-021.

## 6. Thanh toán & webhook SePay

`booking/bank-qr-waiting.html` + `bank-qr-payment.js` (poll `/api/payment-status/{code}` mỗi 4s, dùng `serverNowEpochMillis`).
→ `POST /api/payments/sepay/webhook` → `SePayWebhookController` (Apikey constant-time **hoặc** HMAC-SHA256 trên `timestamp + "." + rawBody`, tolerance 300s)
→ `SePayWebhookService.process`: `payment_transactions.insertIfAbsent` (idempotent bằng UNIQUE) → xác minh `in`, số tiền, reference, tài khoản nhận (đối chiếu snapshot), trạng thái booking → `BookingService.completeOnlinePayment(code, txId, bankRecordedAt)`
→ `PaymentService.completeOnlinePayment`: khóa booking; kiểm `bankRecordedAt < expiresAt`; kiểm ghế còn HELD đúng chủ + đúng `holdUntil`; kiểm tồn combo và lượt voucher; chuyển CONFIRMED; trừ kho/voucher; cộng điểm (ledger idempotent); thông báo.
Nhánh không an toàn → `EXPIRED` + `late_payment_cases` (hoàn tiền thủ công).

## 7. Hết hạn

`@Scheduled(fixedRate=60s)` `BookingService.releaseExpired` → `SeatReservationService.releaseExpiredHolds` + `PaymentService.expirePendingBookings` (batch 100, khóa từng booking).
Sau khi sửa REX-002/003: expire **chỉ** trả ghế, không cộng kho combo / không hoàn lượt voucher.

## 8. Vé điện tử & QR

`GET /bookings/{id}` (chỉ CONFIRMED mới sinh token) → `booking_seats.ticket_code` + `qr_token`.
`GET /bookings/{id}/qr`, `/bookings/{id}/seats/{seatId}/qr` → `QrCodeService.png` (ZXing).
Payload: `REXCINEMAS:TICKET:<random>`.

## 9. Check-in

`admin/checkin.html` + `checkin.js` → `POST /admin/checkin/api/scan` → `BookingService.checkInTicket` → `CheckInService`:
- Nhận 3 dạng: QR token, `TKT-...`, `REX...`.
- `findByIdForUpdate(booking)` + `validateBookingStatus` (trạng thái, cửa sổ 60' trước giờ chiếu → duration+30', giới hạn theo rạp cho STAFF/SUPERVISOR).
- Chống quét trùng: `atomicCheckIn` / `atomicCheckInSeat` (`UPDATE ... WHERE checked_in_at IS NULL`).

## 10. Rex Rewards

- Tích điểm: `LoyaltyService.awardBooking` — `loyalty_ledger.reference = EARN:BOOKING:{code}` là UNIQUE, cộng cả `loyaltyPoints`, `tierQualifyingPoints`, `lifetimePoints`.
- Hoàn điểm: `reverseBooking` — `REVERSE:EARN:BOOKING:{code}`.
- Hạng: `MembershipService.statusFor` + `membership_tier_policies` (cache 30s), 4 hạng MEMBER/SILVER/GOLD/DIAMOND.
- Đổi thưởng: `RewardRedemptionService.redeem` — idempotency key + khóa reward + recheck sau khóa; voucher `usage_limit=1`, gắn `owner_user_id`; trừ điểm qua ledger `REDEEM:{user}:{key}`.
- Hoàn điểm khi hủy voucher: **chưa triển khai** (REX-014).

## 11. Voucher & Combo

- Voucher kiểm: active, thời gian, `usage_limit`, chủ sở hữu, `min_order_amount`; tính `FIXED`/`PERCENT` + `max_discount_amount`.
- Combo: `resolveCombos` khóa `combo_products` bằng `PESSIMISTIC_WRITE`, kiểm `active` + tồn kho, mỗi combo tối đa 10 phần.

## 12. Thông báo

`NotificationService` → `account_notifications` với `deduplication_key` UNIQUE; chuông ở `fragments/header.html` + `header-account.js` gọi `/api/notifications/{id}/read`, `/read-all`.

## 13. Hỗ trợ khách hàng + AI

`chat-widget.js` → `/api/support/me`, `/cinemas`, `/history`, `/send`, `/ai-consult`; STOMP `/topic/support/session/{sessionId}`.
- Đăng nhập: session cố định `user_{id}`.
- Khách: session `guest_xxxxxxx` chỉ trong RAM (sau sửa REX-004, server chỉ nhận prefix `guest_`).
- Admin: `admin/support.html` + `admin-support.js`, topic `/topic/support/cinema/{id}` hoặc `/topic/support/admin`.

## 14. Quản trị & báo cáo

`AdminController` (phim, rạp, phòng, suất chiếu, booking, user, combo, voucher, review), `AdminContentController` (sự kiện/bài viết/media), `AdminRewardsController`, `AdminSupportController`, `DashboardService` (doanh thu theo `paid_at`, chuỗi ngày, top phim/rạp), `MovieOccupancyService` (tỷ lệ lấp đầy theo khung giờ).