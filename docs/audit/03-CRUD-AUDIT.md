# 03 — KIỂM TOÁN CRUD

Ký hiệu: C = Create, R = Read, U = Update, D = Delete. Kết luận: PASS / PARTIAL / FAIL / N/A.

| Entity | C | R | U | D | Phân quyền | Kết luận |
|---|---|---|---|---|---|---|
| `movies` | `POST /admin/movies/save` (validate title/desc/duration/trùng title+releaseDate) | public (`/movies`, `/movies/{id}`, `/api/movies/**`) + admin | `save`, `{id}/toggle` | **soft** (`active=false`) | ADMIN | **PASS** |
| `cinemas` | `save` (trùng name+address) | public | `save`, `toggle` | **soft** | ADMIN | **PASS** |
| `auditoriums` | `save` (kiểm rạp active, 1–26 hàng, 1–50 ghế) | admin | `rename`, `toggle` (khóa + `canHideShowtime`) | **không có** | ADMIN/SUPERVISOR | **PASS** (thiếu xóa là chủ ý) |
| `seats` | chỉ sinh tự động khi tạo phòng | — | — | — | — | **PARTIAL** — không có CRUD ghế, không khóa được ghế hỏng |
| `showtimes` | `save` (chống trùng phòng + 15' dọn) | public + admin | `save` (chặn khi đã bán vé), `toggle` | **soft** | ADMIN/SUPERVISOR theo rạp | **PASS** |
| `showtime_seats` | sinh khi tạo suất | public API | `reprice` khi sửa giá (chỉ khi chưa bán) | — | — | **PASS** (biên: reprice có thể đổi giá ghế đang HELD) |
| `combo_products` | `save` + upload ảnh | public `/api/snacks` | `save` (kèm `stockQuantity`), `toggle` | **không có** | ADMIN | **PARTIAL** (tồn kho chỉ trừ lúc settle — REX-002 đã sửa nhánh expire) |
| `vouchers` | `save` (validate %, thời gian, trùng code) | admin | `save`, `toggle`, `usedCount±1` | **không có** | ADMIN | **PARTIAL** (REX-003 đã sửa) |
| `app_users` | chỉ qua OTP | admin list (scope rạp cho SUPERVISOR) | `toggle` (không tự khóa mình, giới hạn 3 SUPERVISOR/rạp), `role` | **không cho xóa** | ADMIN/SUPERVISOR | **PASS** |
| `bookings` | chỉ qua checkout | chủ sở hữu + admin | `cancel` (admin), `refund` (ADMIN) | **không xóa** | chủ sở hữu / ADMIN | **PASS** |
| `reviews` | upsert 1 review/user/phim | public | tự update | admin `{id}/delete` | CUSTOMER/ADMIN | **PARTIAL** (không kiểm "đã xem phim") |
| `articles` | `save` (validate URL) | public `/articles/{id}` | `save`, `toggle` | **hard delete** `{id}/delete` | ADMIN | **PARTIAL** (xóa cứng) |
| `cinema_events` | `save` | public (trang chủ) | `save`, `toggle` | **hard delete** | ADMIN | **PARTIAL** |
| `media_items` | `save` | public | `save`, `toggle` | **hard delete** | ADMIN | **PARTIAL** |
| `loyalty_rewards` | `save` | public `/rewards` | `save`, `toggle` | **không có** | ADMIN | **PASS** |
| `reward_redemptions` | `redeem` (idempotent) | user + admin | `markVoucherUsed`, `restoreRewardVoucher` | — | CUSTOMER | **PASS** |
| `loyalty_ledger` | chỉ qua service (reference UNIQUE) | user + admin | — (append-only) | — | — | **PASS** |
| `membership_tier_policies` | seed/khởi tạo | public | `saveTierPolicy` (+ audit, reset cache) | — | ADMIN | **PASS** |
| `loyalty_settings` | khởi tạo | public | `saveSettings` (+ audit) | — | ADMIN | **PASS** |
| `account_notifications` | hệ thống | user | `markRead`, `markAllRead` | — | chủ sở hữu | **PASS** |
| `support_messages` | API user (auth), admin reply, guest AI | admin/user | `markAsRead` | — | hỗn hợp | **PASS** sau REX-004 |
| `payment_settings` | `save` (upsert + optimistic version) | admin | `save` + audit | — | ADMIN | **PASS** |
| `bank_transfer_snapshots` | tạo khi confirm | nội bộ | — | — | — | **PASS** |
| `payment_transactions` | `insertIfAbsent` | admin | `updateStatus` | — | ADMIN | **PASS** |
| `late_payment_cases` | `open` | admin | `markNeedsReview`, `confirmRefund`, `markRefundFailed` | — | ADMIN | **PASS** |
| `otp_challenges`, `otp_rate_limits`, `password_reset_challenges` | service | service | service | dọn theo hạn | — | **PASS** |
| `revoked_access_tokens` | `revoke` | `isRevoked` | — | dọn theo hạn | — | **PASS** |
| `seat_hold_sessions` | `checkoutExpiresAt` | hold-status | `expires_at` cố định | khi hoàn tất/hết hạn | — | **PARTIAL** (REX-006) |

## Trả lời các câu hỏi CRUD bắt buộc

1. **Xóa phim ảnh hưởng lịch chiếu?** Không xóa cứng (soft delete). Phim inactive vẫn có thể còn suất chiếu active nhưng mọi entry-point đặt vé đều kiểm `movie.active` qua truy vấn public và `Showtime.isActive`.
2. **Xóa rạp ảnh hưởng phòng chiếu?** Rạp chỉ bị ẩn; phòng/suất chiếu giữ nguyên; 5 entry-point booking kiểm `cinema.isActive`.
3. **Xóa phòng ảnh hưởng ghế?** Không có endpoint xóa phòng. PASS.
4. **Xóa lịch chiếu ảnh hưởng vé đã bán?** Chỉ ẩn, và bị chặn khi có booking PENDING/CONFIRMED/REFUND_PENDING hoặc ghế HELD (`canHideShowtime`).
5. **Sửa giá làm đổi giá đơn cũ?** Không — `bookings.seat_amount/combo_amount/total_amount` + `booking_seats.price` là snapshot.
6. **Xóa tài khoản mất lịch sử?** Không có xóa tài khoản.
7. **Dữ liệu trùng?** UNIQUE: `app_users.email`, `bookings.booking_code/qr_token/sepay_transaction_id`, `booking_seats.qr_token`, `seat_hold_sessions(user_id,showtime_id)`, `vouchers.code`, `seats(auditorium_id,seat_row,seat_number)`, `showtime_seats(showtime_id,seat_id)`, `reviews(user_id,movie_id)`, `movies(title,release_date)`, `cinemas(name,address)`, `auditoriums(cinema_id,name)`, `membership_tier_policies.tier`, `loyalty_ledger.reference_key`, `reward_redemptions(user_id,idempotency_key)`, `account_notifications(user_id,deduplication_key)`, `late_payment_cases.provider_transaction_id`, `payment_transactions.provider_transaction_id`.
8. **Sửa/xóa dữ liệu không thuộc quyền?** Vé: chặn CUSTOMER xem vé người khác. Chat: đã chặn sau REX-004/REX-001. Refund: ADMIN-only. Booking rạp khác (SUPERVISOR): chặn. Hội thoại rạp khác (SUPERVISOR): chặn ở `AdminSupportController`.
9. **Endpoint cho phép thao tác trái phép?** Sau khi sửa: không còn đường ghi chéo tài khoản qua `/api/support/ai-consult`; WebSocket không còn cho subscribe chéo.