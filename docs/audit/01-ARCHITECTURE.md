# 01 — KIẾN TRÚC THỰC TẾ

## Công nghệ (từ `pom.xml` + properties, đã đọc toàn văn)

Java 21 · Spring Boot 3.3.5 (parent) · Spring MVC + Thymeleaf · Spring Data JPA/Hibernate (`ddl-auto=validate`) · Spring Security + JWT (jjwt 0.12.6) · Spring WebSocket STOMP/SockJS · MySQL 8 · Spring Mail (SMTP) · Actuator (chỉ `health`) · ZXing 3.5.4 · Cloudinary 1.39.0 · TwelveMonkeys WebP 3.15.3 · H2 (test).

## Số liệu đo được

| Hạng mục | Số lượng |
|---|---|
| File Java `src/main/java` | 170 (11.154 dòng) |
| Controller / Service / Repository / Entity / DTO / Config / Security | 20 / 31 / 34 / 47 / 23 / 10 / 4 |
| Template Thymeleaf | 48 |
| File JS | 17 (1.889 dòng) |
| CSS | 1 file (2.106 dòng) |
| Script SQL | 4 (schema 745 dòng) |
| Bảng trong `schema.sql` | 36 |
| Test Java | 53 file, 188 bài khi chạy |

## Luồng kiến trúc

```
Browser ─┬─ MVC/Thymeleaf ─ Controller ─ Service ─ Repository ─ MySQL
         ├─ REST /api/**   ─ Controller ─ Service ─ Repository ─ MySQL
         └─ STOMP /ws      ─ SimpMessagingTemplate ─ /topic/**   (đã thêm kiểm quyền)
SePay  ──── POST /api/payments/sepay/webhook ─ SePayWebhookService
                                                ─ BookingService ─ PaymentService
```

`BookingService` là facade mỏng uỷ quyền cho `SeatReservationService`, `PaymentService`, `CheckInService`. Chuỗi sâu nhất: `SePayWebhookService → BookingService → PaymentService → SeatReservationService → Repository` (4 tầng).

## Phân quyền (SecurityConfig)

- Public: `/`, `/error`, `/login`, `/register`, `/policies`, `/auth/**`, `/api/auth/**`, `/css/**`, `/js/**`, `/images/**`, `/ws/**`, `/api/support/**`, `/api/payments/sepay/webhook`, `/actuator/health`.
- `GET` public: `/movies/**`, `/schedule`, `/articles/**`, `/api/movies/**`, `/api/showtimes/*/seats`, `/api/snacks`.
- ADMIN: toàn bộ `/admin/**` (trừ các rule đặc thù), `/admin/payments/**`, `/admin/late-payments/**`, `/admin/rewards/**`, `/admin/transactions/**`, `/admin/bookings/*/refund`.
- ADMIN + SUPERVISOR: `/admin/bookings/**`, `/admin/showtimes/**`, `/admin/users/**`, `/admin/support/**`.
- ADMIN + SUPERVISOR + STAFF: `/admin`, `/admin/`, `/admin/checkin/**`.
- Còn lại: `authenticated()`.

## Quan sát kiến trúc

1. Không có circular dependency giữa các bean.
2. `AdminController` 974 dòng là God-class (45 endpoint) — nên tách theo miền.
3. Tồn tại 2 nhánh tính tiền song song trong `BookingService` (`quote()` và `confirm()`) với 2 hàm voucher khác nhau (`previewVoucher` / `resolveVoucher`).
4. Ba nơi sinh token QR: `CheckInService.qrPayloadFor`, `CheckInService.seatQrPayloadFor`, `BookingController.ticket`.
5. Các `PaymentMethod` khác `BANK_QR` (`PAY_AT_COUNTER`, `MOMO_SANDBOX`, `VNPAY_SANDBOX`) vẫn còn trong enum/cột DB nhưng `confirm()` chặn — chỉ đọc được dữ liệu cũ.
6. Nhiều constructor "compatibility for tests" trong `BookingService`, `PaymentService`, `SeatReservationService`, `SePayWebhookService`, `CheckInService` — hữu ích cho test nhưng làm mờ luồng production.