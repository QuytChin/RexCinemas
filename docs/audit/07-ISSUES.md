# 07 — DANH SÁCH VẤN ĐỀ (đã xác minh lại từng mục REX-001..REX-023)

Phân loại: **BUG** (lỗi thực, có bằng chứng) · **DESIGN** (quyết định thiết kế, không phải lỗi) · **IMPROVEMENT** · **MISSING** · **NOT VERIFIED**.

| Mã | Mức | Loại | Trạng thái xác minh | Trạng thái xử lý |
|---|---|---|---|---|
| REX-001 | HIGH | BUG | CONFIRMED | **ĐÃ SỬA** |
| REX-002 | HIGH | BUG | CONFIRMED | **ĐÃ SỬA** |
| REX-003 | MEDIUM | BUG | CONFIRMED | **ĐÃ SỬA** |
| REX-004 | HIGH | BUG | CONFIRMED | **ĐÃ SỬA** |
| REX-005 | MEDIUM | BUG (hiệu năng + khớp nhầm) | CONFIRMED | **ĐÃ SỬA** |
| REX-006 | MEDIUM | BUG (đồng thời) | SUSPECTED | Chưa sửa — P2 |
| REX-007 | MEDIUM | BUG cấu hình (quy tắc = 5 phút cố định) | CONFIRMED | **ĐÃ SỬA** (phương án A) |
| REX-008 | MEDIUM | BUG bảo mật | CONFIRMED | **ĐÃ SỬA** |
| REX-009 | MEDIUM | DESIGN (API client) | CONFIRMED | Chưa sửa |
| REX-010 | LOW | DESIGN (lazy init) | CONFIRMED | Chưa sửa |
| REX-011 | LOW | BUG schema | CONFIRMED | **ĐÃ SỬA** (schema.sql) |
| REX-012 | LOW | BUG (timezone) | CONFIRMED logic | **ĐÃ SỬA** |
| REX-013 | LOW | BUG dữ liệu (giá hard-code) | CONFIRMED | Chưa sửa |
| REX-014 | LOW | MISSING | CONFIRMED | Chưa sửa |
| REX-015 | LOW | IMPROVEMENT | CONFIRMED | Chưa sửa |
| REX-016 | LOW | BUG CI (test mẫu) | CONFIRMED | Chưa sửa |
| REX-017 | LOW | IMPROVEMENT (hiệu năng) | CONFIRMED | Chưa sửa |
| REX-018 | LOW | BUG | CONFIRMED | **ĐÃ SỬA** |
| REX-019 | LOW | DESIGN (khóa toàn map) | CONFIRMED | Chưa sửa |
| REX-020 | LOW | BUG nhỏ (preview) | CONFIRMED | Chưa sửa |
| REX-021 | LOW | BUG (hiếm) | SUSPECTED | **ĐÃ SỬA** |
| REX-022 | LOW | MISSING (rate limit) | CONFIRMED | Chưa sửa |
| REX-023 | LOW | BUG (NPE tiềm ẩn) | CONFIRMED | **ĐÃ SỬA** |

## Đính chính so với báo cáo trước

- Báo cáo trước ghi hạng thành viên là `PLATINUM`. **Sai**: enum thực tế là `MEMBER, SILVER, GOLD, DIAMOND` (`MembershipTier.java`).
- Báo cáo trước ghi REX-005 là "quét toàn bộ booking" — chính xác về mặt logic; đã bổ sung rủi ro khớp nhầm chuỗi con.

---

## REX-001 — WebSocket thiếu kiểm quyền subscribe (HIGH) — ĐÃ SỬA

- Nguyên nhân gốc: interceptor chỉ chặn `SEND`, còn `SUBSCRIBE` chỉ kiểm regex `support/.*`; session id dạng `user_{id}` (tuần tự, đoán được).
- Tái hiện: mở console trang bất kỳ, `client.subscribe('/topic/support/session/user_5')` → nhận `SupportMessageDto` của tài khoản 5.
- Hậu quả: rò rỉ tên, email, nội dung hội thoại của mọi khách hàng.
- Sửa: regex chặt + kiểm `Principal` (chủ tài khoản cho `session/user_N`, ADMIN cho `admin`, ADMIN/SUPERVISOR đúng rạp cho `cinema/{id}`).
- Test: `WebSocketConfigTest` (6 bài) — anonymous bị chặn, chủ tài khoản được phép, người khác bị chặn, admin/cinema scope, guest/seat-map vẫn mở.

## REX-002 — Tồn kho combo tăng ảo khi booking PENDING hết hạn (HIGH) — ĐÃ SỬA

- Bằng chứng: `BookingService.confirm` chỉ trừ kho khi `!online`, mà `online` luôn `true` (chỉ nhận `BANK_QR`); `PaymentService.expirePendingBooking` lại gọi `releaseConfirmedResources()` cộng kho.
- Tái hiện: đặt 2 combo → không thanh toán → scheduler expire → `combo_products.stock_quantity` +2.
- Sửa: expire chỉ trả ghế (`releaseBookingSeats`), giữ nguyên `releaseConfirmedResources` cho luồng refund (nơi kho đã thực sự bị trừ lúc settle).
- Test: `PaymentServiceTest.expiryNeverRestoresComboStockOrVoucherQuotaThatWasNeverReserved`.

## REX-003 — Lượt dùng voucher bị trả oan (MEDIUM) — ĐÃ SỬA

- Cùng nguyên nhân với REX-002 (`decrementVoucher` trong cùng hàm).
- Hậu quả: voucher dùng chung được "hoàn lượt" cho đơn chưa hề dùng; `restoreRewardVoucher` có thể đổi trạng thái redemption của người khác.
- Test: cùng bài test trên (`verifyNoInteractions(combos, vouchers)`).

## REX-004 — Ghi chéo hội thoại hỗ trợ khi chưa đăng nhập (HIGH) — ĐÃ SỬA

- Bằng chứng: `askAi` dùng `req.sessionId()` trực tiếp cho guest.
- Sửa: chỉ chấp nhận `guest_[A-Za-z0-9_-]{1,59}`; còn lại 400.
- Test: `SupportChatApiControllerTest` (4 bài).

## REX-005 — Khớp mã thanh toán theo `contains` (MEDIUM) — ĐÃ SỬA

- Trước: tải toàn bộ booking `PENDING/EXPIRED/CANCELLED` rồi `content.contains(reference)` (O(N), nguy cơ khớp chuỗi con).
- Sau: tách `content` thành token `[A-Za-z0-9]{6,40}`, tra chính xác theo `(BANK_QR, token)`; giới hạn 40 token.
- Test: `SePayWebhookServiceTest.contentContainingOnlyAPartialReferenceTokenDoesNotSettleABooking` + 2 test cũ được cập nhật.

## REX-006 — Hai hold đồng thời cùng user (MEDIUM) — CHƯA SỬA

- Nguyên nhân: `SELECT ... FOR UPDATE` trên hàng chưa tồn tại không khóa được gap; 2 transaction cùng insert → `DuplicateKeyException` (UNIQUE `seat_hold_sessions(user_id, showtime_id)`).
- Vì sao chưa sửa: bắt exception trong cùng transaction JPA sẽ đánh dấu rollback-only; cần `REQUIRES_NEW` hoặc khóa tuần tự theo user. Thay đổi này ảnh hưởng luồng giữ ghế nên cần thiết kế riêng + test đồng thời thật.
- Đề xuất: khóa hàng `app_users` (`findByIdForUpdate`) trước khi tạo session, hoặc retry toàn bộ `holdSeats` ở tầng ngoài khi gặp `DataIntegrityViolationException`.

## REX-007 — `payment_timeout_minutes` không tác dụng với BANK_QR (MEDIUM) — ĐÃ SỬA theo phương án A

- Bằng chứng: `BookingService.confirm` ghi đè `expiresAt = min(holdUntil)` (5 phút) cho `BANK_QR`.
- **Quyết định của chủ dự án: giữ 5 phút cố định.** Vì vậy cấu hình timeout bị loại bỏ khỏi hành vi thay vì được kích hoạt.
- Đã làm:
  - `SeatReservationService.CHECKOUT_HOLD_MINUTES = 5` (nguồn duy nhất của quy tắc).
  - `BookingService.confirm`: bỏ nhánh tính `timeoutMinutes` chết, chỉ dùng `holdUntil`.
  - `BankTransferPaymentService.paymentTimeoutMinutes()`: xoá (không còn nơi dùng).
  - `PaymentSettingsService`: luôn ghi `5` và **từ chối** giá trị khác thay vì im lặng bỏ qua.
  - `admin/payment-settings.html`: trường thành chỉ-đọc, ghi rõ "Cố định 5 phút … không thể thay đổi".
- Test: `PaymentSettingsServiceTest` (2 bài) + `BookingHoldCycleIntegrationTest` giữ nguyên assert ~5 phút.

## REX-008 — CSRF bypass bằng header Bearer bất kỳ (MEDIUM) — ĐÃ SỬA

- Sửa: chỉ bỏ CSRF khi token hợp lệ và chưa bị thu hồi.

## REX-009 — JWT trong body `/api/auth/login` (MEDIUM) — CHƯA SỬA

- Ảnh hưởng: giảm giá trị cookie HttpOnly; API bỏ qua cờ `remember`.
- Hướng: bỏ `token` khỏi body (hoặc chỉ trả cho client có header đặc biệt), thêm `remember` vào request.

## REX-010 — GET có side-effect (LOW) — CHƯA SỬA

- `GET /bookings/{id}` sinh `ticket_code`/`qr_token`; `GET /api/payment-status/{code}` gọi `expireIfDue`.
- Hướng: sinh token lúc settle; expire bằng scheduler + webhook, chỉ đọc trong GET.

## REX-011 — Thiếu index `(status, expires_at)` (LOW) — ĐÃ SỬA

- Đã thêm vào `database/mysql/schema.sql`. DB hiện hữu cần `ALTER TABLE bookings ADD INDEX idx_bookings_status_expires_at (status, expires_at);` (chưa chạy — cấm thay đổi DB).

## REX-012 — Parse `transactionDate` không có offset (LOW) — ĐÃ SỬA

- Nay chấp nhận ISO offset/`Z` và quy đổi về múi giờ server; payload không offset giữ nguyên hành vi cũ.

## REX-013 — Giá trong AI tư vấn hard-code (LOW) — CHƯA SỬA

- `AiMovieConsultantService` trả bảng giá/combo tĩnh, có thể lệch dữ liệu DB.

## REX-014 — Thiếu hoàn điểm khi huỷ voucher đổi thưởng (LOW, MISSING) — CHƯA SỬA

- Enum `REFUND_REDEMPTION` và tab "Hoàn tác" tồn tại nhưng không có nơi ghi.

## REX-015 — Password policy đăng ký yếu (LOW) — CHƯA SỬA

## REX-016 — CI Playwright chỉ chạy test mẫu (LOW) — CHƯA SỬA

- `.github/workflows/playwright.yml` chạy `npx playwright test` trên `tests/example.spec.js` (trang playwright.dev). Không có job Maven.
- Lưu ý: `.github/` đang là untracked trong git.

## REX-017 — `findRecentConversations` không giới hạn (LOW) — CHƯA SỬA

## REX-018 — `requestId` chứa dấu `.` (LOW) — ĐÃ SỬA

## REX-019 — Khóa toàn bộ seat-map khi giữ ghế (LOW, DESIGN) — CHƯA SỬA

- Đúng về toàn vẹn (bảo vệ luật ghế lẻ). Chỉ là điểm nghẽn khi tải cao.

## REX-020 — `quote()` không kiểm `holdUntil` (LOW) — CHƯA SỬA

## REX-021 — Giải phóng ghế sau khi booking đã commit (LOW) — ĐÃ SỬA

- Sửa: `releaseUnconfirmedHolds` chỉ trả ghế khi không còn booking PENDING còn hiệu lực.
- Test: `BookingHoldReleaseGuardTest` (3 bài).

## REX-022 — Thiếu rate limit đăng nhập / AI (LOW) — CHƯA SỬA

## REX-023 — `PaymentTransactionRepository.search` NPE khi không sort (LOW) — ĐÃ SỬA
---

## REX-A01 — Staff truy cập trang booking/vé của khách khác (HIGH) — ĐÃ XÁC MINH

**Kết luận: CONFIRMED trên bản đã commit (HEAD), đã được vá trong working tree; phần còn sót đã vá trong phiên này.**

### Bằng chứng (bản HEAD)

`git show HEAD:src/main/java/com/rexchain/cinema/controller/BookingController.java`

```java
private Booking authorizedBooking(Long id, Authentication auth) {
    Booking booking = bookings.findById(id).orElseThrow();
    User user = current(auth);
    if (!booking.getUser().getId().equals(user.getId()) && user.getRole() == Role.CUSTOMER)
        throw new IllegalStateException("Bạn không có quyền xem vé này");
    return booking;
}
```

Điều kiện chỉ chặn khi role **là CUSTOMER**. Do đó STAFF / SUPERVISOR / CINEMA_MANAGER / ADMIN đều đi qua, dù không phải chủ đơn.

### Phạm vi ảnh hưởng (3 endpoint dùng chung hàm này)

| Endpoint | Rò rỉ |
|---|---|
| `GET /bookings/{id}` | Toàn bộ trang vé: phim, rạp, phòng, giờ chiếu, danh sách ghế, tổng tiền, trạng thái check-in. Hàm còn **sinh `ticket_code`/`qr_token`** cho booking CONFIRMED → tạo luôn vé điện tử |
| `GET /bookings/{id}/qr` | Ảnh QR vé đoàn (bearer credential để check-in) |
| `GET /bookings/{id}/seats/{seatId}/qr` | Ảnh QR từng ghế |

`CheckInService.qrPayloadFor/seatQrPayloadFor` không kiểm chủ sở hữu; kiểm tra duy nhất nằm ở controller.

### Điều kiện tái hiện

1. Đăng nhập bằng tài khoản STAFF (bất kỳ rạp nào, kể cả khác rạp của khách).
2. Mở `GET /bookings/21` hoặc `GET /bookings/21/qr`.
3. Trước khi vá: trả 200 + nội dung vé/QR. Sau khi vá: 403.

### Hậu quả thực tế

- Đọc dữ liệu cá nhân và đơn hàng của mọi khách (IDOR/broken access control).
- Lấy được token QR → có thể dùng để check-in sớm/hủy hiệu lực vé của khách nếu thao tác tại đúng rạp.
- Ngược lại, ADMIN cũng không có màn hình nào dẫn tới `/bookings/{id}` nên không có nhu cầu nghiệp vụ bị mất.

### Trạng thái hiện tại

1. **Working tree đã vá** (không phải thay đổi của phiên audit trước): chỉ role CUSTOMER đi tiếp, sau đó kiểm `booking.user.id == user.id`, ném `AccessDeniedException`.
2. **Phiên này vá nốt endpoint cùng nhóm còn hở**: `GET /api/payment-status/{bookingCode}` (trước đó chỉ chặn CUSTOMER, nên STAFF/SUPERVISOR đọc được booking của mọi khách). Nay: chủ đơn hoặc ADMIN.
3. Đã khóa hành vi bằng test: `BookingTicketAccessTest` (8 bài) — staff/supervisor bị 403 ở payment-status; staff bị chặn ở trang vé và cả 2 endpoint QR; khách khác bị chặn; chủ đơn và ADMIN vẫn hoạt động.

### Ghi chú thiết kế

- STAFF/SUPERVISOR dùng luồng riêng: `/admin/checkin` (giới hạn theo rạp), `/admin/bookings` (SUPERVISOR giới hạn theo rạp), `/admin/support`.
- Nếu cần cho ADMIN xem trang vé để hỗ trợ khách, nên mở bằng route admin riêng có audit thay vì nới `/bookings/{id}`.