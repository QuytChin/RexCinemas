# 08 — NHẬT KÝ SỬA LỖI

Mọi thay đổi đều nhỏ nhất có thể, không xóa tính năng, không đổi quy tắc nghiệp vụ, không chạm database/secret.

## F-01 · REX-002 + REX-003 — Tài nguyên không được "hoàn" khi chưa từng bị trừ

**File**: `src/main/java/com/rexchain/cinema/service/PaymentService.java`

```diff
         booking.setStatus(BookingStatus.EXPIRED);
-        // A pending booking may already have reserved combo stock and voucher usage.
-        // Release all temporary resources atomically with the status transition.
-        releaseConfirmedResources(booking);
+        // A PENDING booking only ever reserved seats: combo stock and voucher usage are
+        // decremented at settlement. Releasing them here inflated stock and refunded
+        // voucher quota for orders that never consumed either.
+        seatReservationService.releaseBookingSeats(booking);
         return bookings.save(booking);
```

`releaseConfirmedResources` vẫn được dùng ở `processRefund` (đúng: refund diễn ra sau settle).

**Tác động dây chuyền**: đã kiểm `grep releaseConfirmedResources` → chỉ còn 1 caller. `PaymentServiceTest.expiredQrStatusRequestReleasesPendingResourcesExactlyOnce` vẫn pass.

## F-02 · REX-001 — Kiểm quyền subscribe STOMP

**File**: `src/main/java/com/rexchain/cinema/config/WebSocketConfig.java`

- Regex topic: `^/topic/(showtimes/\d+/seats|support/(session/[A-Za-z0-9_-]{1,64}|cinema/\d+|admin))$`
- Thêm `UserRepository` (constructor `@Autowired` 3 tham số; giữ constructor 2 tham số cho test).
- `validateInbound(command, destination, principal)`; giữ `validateInbound(command, destination)` để không phá test cũ.
- `authorizeSupportSubscription`:
  - `admin` → ADMIN
  - `cinema/{id}` → ADMIN, hoặc SUPERVISOR có `user.cinema.id == id`
  - `session/user_N` → `user.id == N`
  - `session/guest_*` → cho phép (chỉ AI, session trong RAM)
  - còn lại → 403

**Lưu ý vận hành**: `Principal` lấy từ handshake (`getUserPrincipal()`), nên JWT cookie phải được gửi cùng handshake — trình duyệt gửi tự động (cookie same-origin).

## F-03 · REX-004 — Chặn guest ghi vào session của người khác

**File**: `src/main/java/com/rexchain/cinema/controller/SupportChatApiController.java`

Thêm `guestSessionId(String)` chỉ nhận `guest_[A-Za-z0-9_-]{1,59}`, mặc định `guest_ai`. Khớp với `chat-widget.js` (`'guest_' + Math.random().toString(36).substring(2,9)`).

## F-04 · REX-008 — CSRF chỉ bỏ qua khi Bearer hợp lệ

**File**: `src/main/java/com/rexchain/cinema/config/SecurityConfig.java`

`filterChain` nhận thêm `JwtService` và `AccessTokenRevocationService`; matcher yêu cầu `jwtService.valid(token) && !revocations.isRevoked(token)`.

## F-05 · REX-005 — Khớp mã thanh toán chính xác theo token

**File**: `src/main/java/com/rexchain/cinema/service/SePayWebhookService.java`

- Thêm hằng `RECONCILABLE_STATUSES`.
- `findByContent`: tách token, tối đa 40 token, mỗi token tra `findByPaymentMethodAndPaymentReference(BANK_QR, token)`.

## F-06 · REX-012 — Timezone cho `transactionDate`

**File**: `src/main/java/com/rexchain/cinema/service/SePayWebhookService.java`

Thử `OffsetDateTime.parse(ISO_OFFSET_DATE_TIME)` trước, quy đổi `ZoneId.systemDefault()`; sau đó mới thử các format không offset.

## F-07 · REX-021 — Không trả ghế khi booking đã tạo

**Files**:
- `repository/BookingRepository.java`: thêm `existsByUserIdAndShowtimeIdAndStatusInAndExpiresAtAfter(...)`
- `service/BookingService.java`: thêm `releaseUnconfirmedHolds(...)`
- `controller/BookingController.java`, `controller/CinemaApiController.java`: catch-block gọi `releaseUnconfirmedHolds`

`releaseOwnHolds` (khách chủ động trả ghế) **không** đổi hành vi.

## F-08 · REX-023 — Sort an toàn

**File**: `src/main/java/com/rexchain/cinema/repository/PaymentTransactionRepository.java`

Dùng `page.getSort().isSorted()`; mặc định `received_at DESC`.

## F-09 · REX-018 — `requestId` hợp lệ trên trình duyệt cũ

**File**: `src/main/resources/templates/rewards/dashboard.html`

Loại ký tự ngoài `[a-z0-9]` khỏi fallback và cắt còn 32 ký tự.

## F-10 · REX-011 — Đồng bộ index schema

**File**: `database/mysql/schema.sql`

Thêm `KEY idx_bookings_status_expires_at (status, expires_at)` cho bảng `bookings` (khớp khai báo trong `Booking.java`).

## F-11 · REX-007 — Chốt quy tắc giữ ghế 5 phút cố định (phương án A)

**Files**:
- `service/SeatReservationService.java`: thêm `public static final int CHECKOUT_HOLD_MINUTES = 5;` và dùng cho mọi mốc thời gian của chu kỳ giữ ghế.
- `service/BookingService.java`: bỏ nhánh `timeoutMinutes` (biến chết vì `online` luôn là `BANK_QR`); `expiresAt` chỉ lấy từ `holdUntil`.
- `service/BankTransferPaymentService.java`: xoá `paymentTimeoutMinutes()` (không còn caller).
- `service/PaymentSettingsService.java`: luôn lưu `CHECKOUT_HOLD_MINUTES`; nếu form gửi giá trị khác thì báo lỗi rõ ràng thay vì im lặng.
- `templates/admin/payment-settings.html`: trường "Thời hạn giữ chỗ" chuyển thành chỉ-đọc, ghi chú cố định 5 phút.

```diff
-            int timeoutMinutes = method == PaymentMethod.BANK_QR && bankTransferPaymentService != null
-                    ? bankTransferPaymentService.paymentTimeoutMinutes()
-                    : 15;
-            LocalDateTime expiresAt = now.plusMinutes(timeoutMinutes);
-            if (method == PaymentMethod.BANK_QR) {
-                ...
-                expiresAt = list.stream()...
-            }
+            // The checkout window is the fixed seat hold created when the customer clicked
+            // "Giữ ghế & tiếp tục". Confirming never restarts or extends that clock.
+            LocalDateTime expiresAt = list.stream()...
```

**Vì sao không chọn phương án B**: đổi timeout sẽ phải sửa `SeatHoldSession`, luồng gia hạn và test hold-cycle; chủ dự án chọn giữ nguyên quy tắc 5 phút.

**Test**: `PaymentSettingsServiceTest` (2 bài, mới).

---

## Test phải cập nhật vì chúng khẳng định hành vi lỗi

| Test | Lý do |
|---|---|
| `SePayWebhookServiceTest.nullCodeResolvesPaymentReferenceFromContent` | Trước stub `findByPaymentMethodAndStatus`; nay stub `findByPaymentMethodAndPaymentReference(BANK_QR, "REXELEL7PKS")` |
| `SePayWebhookServiceTest.invalidCodeFallsBackToPaymentReferenceInContent` | Như trên |
| `SePayWebhookServiceTest.contentWithoutKnownPaymentReferenceIsRejected` | Stub đúng các token thực sự được tra |
| `SePayWebhookControllerTest.contentFallbackAcknowledgesConfirmedRealWebhookPayload` | Như trên |

Không có test nào bị xóa hay bị làm yếu (không dùng `lenient()`, không bỏ assert).

## Test mới

| File | Số bài | Nội dung |
|---|---|---|
| `config/WebSocketConfigTest` (bổ sung) | +4 | Chặn subscribe chéo, chủ tài khoản được phép, admin/cinema scope, seat-map + guest vẫn mở |
| `controller/SupportChatApiControllerTest` (mới) | 4 | Guest không ghi được vào `user_*`/session lạ; `guest_*` và mặc định `guest_ai` hoạt động |
| `service/BookingHoldReleaseGuardTest` (mới) | 3 | Ghế giữ nguyên khi có booking PENDING; trả ghế khi không có; `releaseOwnHolds` không bị chặn |
| `service/PaymentServiceTest` (bổ sung) | +1 | Expire không cộng kho combo, không đụng voucher |
| `service/SePayWebhookServiceTest` (bổ sung) | +1 | Token con của mã tham chiếu không settle được booking |
## F-12 · REX-A01 — Chặn Staff đọc vé/QR/trạng thái thanh toán của khách khác

**Files**:
- `controller/BookingController.java` — `authorizedBooking` (bản working tree): chỉ role CUSTOMER đi tiếp, sau đó so `booking.user.id`; ném `AccessDeniedException`. Áp dụng cho `/bookings/{id}`, `/bookings/{id}/qr`, `/bookings/{id}/seats/{seatId}/qr`.
- `controller/CinemaApiController.java` — `paymentStatus`: thay điều kiện "chỉ chặn CUSTOMER" bằng `canReadBooking(user, booking)` = chủ đơn hoặc ADMIN.

```diff
-        if (user.getRole() == Role.CUSTOMER && !booking.getUser().getId().equals(user.getId())) {
+        if (!canReadBooking(user, booking)) {
             return ResponseEntity.status(403).body(Map.of("error", "Không có quyền xem booking này"));
         }
+    }
+
+    private boolean canReadBooking(User user, Booking booking) {
+        if (user.getRole() == Role.ADMIN) return true;
+        return booking.getUser() != null && booking.getUser().getId().equals(user.getId());
     }
```

**Vì sao vẫn cho ADMIN**: ADMIN đã có toàn quyền xem booking ở `/admin/bookings` và `/admin/transactions`; đây là endpoint polling của trang chờ thanh toán nên cần dự phòng cho hỗ trợ qua điện thoại.

**Vì sao không cho STAFF/SUPERVISOR**: họ có luồng riêng (`/admin/checkin` giới hạn theo rạp). Quyền đọc chéo rạp là chính lỗ hổng REX-A01.

**Test**: `BookingTicketAccessTest` (8 bài, mới).