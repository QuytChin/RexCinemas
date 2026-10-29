# 05 — KIỂM TOÁN DATABASE (Entity ↔ JPA ↔ Repository ↔ MySQL)

## Quy ước

- `ddl-auto=validate` ở mọi profile (dev/mysql/prod). `SchemaSafetyGuard` chặn prod nếu khác `validate`, nếu `sql.init.mode != never`, hoặc nếu bật seed.
- Schema phát hành: `database/mysql/schema.sql` (36 bảng, MySQL 8, utf8mb4_0900_ai_ci, InnoDB).
- Một số bảng do quy trình nâng cấp tạo riêng (`payment_transactions`, `late_payment_cases`, `bank_transfer_snapshots`, `seat_hold_sessions`, `loyalty_*`, `reward_redemptions`, `membership_tier_policies`, `support_messages`); code đọc chúng bằng `JdbcTemplate` + `DatabaseSchemaResolver` và **fail-closed** nếu thiếu bảng.

## Ràng buộc quan trọng (đã đối chiếu entity ↔ DDL)

| Bảng | UNIQUE | Ghi chú |
|---|---|---|
| `app_users` | `email` | cộng index `cinema_id`, `favorite_cinema_id` |
| `bookings` | `booking_code`, `qr_token`, `sepay_transaction_id` | index `status+created_at`, `user_id+created_at`, `payment_method+payment_reference`; **đã thêm** `status+expires_at` (REX-011) |
| `booking_seats` | `qr_token` | index `ticket_code`, `booking_id` |
| `seat_hold_sessions` | `user_id+showtime_id` | index `expires_at` |
| `showtime_seats` | `showtime_id+seat_id` | index `showtime_id+status`, `status+hold_until` |
| `vouchers` | `code` | FK `owner_user_id` |
| `loyalty_ledger` | `reference_key` | append-only |
| `reward_redemptions` | `user_id+idempotency_key` | 1-1 với `vouchers` |
| `account_notifications` | `user_id+deduplication_key` | |
| `otp_challenges` | `email+purpose` | |
| `otp_rate_limits` | `scope_key+purpose` | |
| `password_reset_challenges` | `email+purpose` | |
| `payment_transactions` | `provider_transaction_id` | idempotency webhook |
| `late_payment_cases` | `provider_transaction_id` | |

## Kiểu dữ liệu tiền tệ

- Tất cả tiền dùng `BigDecimal` với `precision=12, scale=2`; phần trăm/hệ số điểm `precision=7, scale=4`.
- Làm tròn: giảm giá voucher `RoundingMode.HALF_UP` (chia 100), giảm giá hạng và điểm `RoundingMode.DOWN`.
- Không phát hiện chỗ nào dùng `double`/`float` cho tiền.

## Đồng bộ schema — vấn đề đã sửa

**REX-011**: entity `Booking` khai báo index `idx_bookings_status_expires_at` nhưng `schema.sql` không có → truy vấn scheduler `findExpiredBookingIds(status, expires_at)` full scan. Đã thêm index vào `schema.sql`. Lưu ý: DB đang chạy cần chạy thêm `ALTER TABLE` (không thực hiện trong phiên này — cấm thay đổi DB).

## Vấn đề đã xác nhận (chưa sửa)

| Mã | Nội dung | Ảnh hưởng |
|---|---|---|
| REX-002 | `expirePendingBooking` cộng lại tồn kho combo dù chưa từng trừ | **ĐÃ SỬA** |
| REX-003 | `expirePendingBooking` giảm `used_count` voucher dù chưa từng tăng | **ĐÃ SỬA** |
| REX-006 | Hai `hold` đồng thời cùng user → vi phạm UNIQUE `seat_hold_sessions` → 500 | P2, chưa sửa (xem 07) |

## Nguy cơ N+1 / LazyInitialization

- `Booking.seats`, `Booking.combos` dùng `@BatchSize(50)`; `AdminController.bookingPage` gọi `loadSeatsForPage` để tránh N+1.
- `findAllDetailed()` dùng `join fetch` + `left join fetch b.seats` (không phân trang) — nên thay bằng truy vấn theo trang nếu dữ liệu lớn.
- `SupportMessageRepository.findRecentConversations` dùng 2 subquery lặp theo nhóm và **không LIMIT** (REX-017).
- `HeaderModelAdvice` chạy cho mọi trang MVC: 1 truy vấn rạp + (nếu đăng nhập) 1 truy vấn user + 2 truy vấn notification.

## Transaction boundary

- `SeatReservationService.holdSeats`, `PaymentService.completeOnlinePayment`, `CheckInService.checkInTicket`, `RewardRedemptionService.redeem`, `MembershipService.saveSettings/saveTierPolicy`, `LatePaymentCaseService.*` đều `@Transactional`.
- `BookingService` bọc lại các lời gọi service con trong transaction của chính nó (self-invocation không xảy ra vì gọi bean khác).
- `OtpService.verifyOtp` / `PasswordResetService.verifyOtp` dùng `REQUIRES_NEW` + `noRollbackFor = IllegalArgumentException` để lần sai vẫn được lưu (chống brute-force).

## Khả năng migrate

`schema.sql` độc lập tên database (dùng `CREATE DATABASE IF NOT EXISTS rex_chain_cinema`), có script nâng cấp `upgrade-staff-supervisor.sql`, script backup/restore trong `scripts/`. Việc chạy thử trên MySQL thật: **NOT VERIFIED** trong phiên này.