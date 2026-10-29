# PROGRESS — RexCinemas Audit & Fix

Cập nhật: 2026-10-30

## Trạng thái tổng

| Giai đoạn | Trạng thái |
|---|---|
| 1. Inventory | Hoàn thành (`00-INVENTORY.md`, 325 file văn bản / 25.194 dòng) |
| 2. Kiểm tra logic toàn hệ thống | Hoàn thành cho tầng Java (100% file `src/main/java` đọc toàn văn) |
| 3. Kiểm tra nghiệp vụ (26 nhóm) | Hoàn thành cho các luồng có mã nguồn Java |
| 4. Xác minh REX-001..REX-023 | Hoàn thành — 15 CONFIRMED, 3 DESIGN, 5 SUSPECTED/NOT VERIFIED |
| 5. Sửa lỗi | 13 lỗi đã sửa (gồm REX-A01) + 2 cải thiện schema/template/UI |
| 6. Kiểm thử | `mvn test` → 190 test, 0 failure, 0 error, 9 skipped |
| 7. Tài liệu | Hoàn thành 00..10 |
| 8. Xác nhận hoàn thành | Có công bố phần chưa kiểm chứng trong `10-FINAL-REPORT.md` |

## File đã đọc toàn văn

- Toàn bộ 170 file `src/main/java` (entity 47, repository 34, service 31, controller 20, dto 23, config 10, security 4, bootstrap 1).
- `pom.xml`, 5 file `.properties`, 4 file SQL, `.github/workflows/playwright.yml`, `playwright.config.js`, `package.json`, `tests/example.spec.js`.
- 5 tài liệu markdown (README, ARCHITECTURE, CUSTOMER_POLICIES, OPERATIONS_MYSQL_BACKUP_RESTORE, FIXES-2026-10-29).
- 7 file test trọng yếu (WebSocket, SupportChat API, PaymentService, SePay webhook x2, hold-cycle integration, booking release guard).

## File đã đọc một phần

- 48 template Thymeleaf: đọc toàn văn 7 file (seat-map, bank-qr-waiting, rewards/dashboard, admin/support, admin/showtimes, profile, header fragment); các file còn lại chỉ tra `th:action`, `th:href`, `th:utext`.
- 17 file JS: đọc toàn văn 7 file (booking.js, bank-qr-payment.js, chat-widget.js, admin-support.js, checkin.js, auth-form.js, password-reset.js); còn lại tra cứu.
- 53 file test: đọc toàn văn 7, còn lại đếm/grep theo annotation.

## File chưa đọc

- `src/main/resources/static/css/app.css` (2.106 dòng) — không review. Mọi kết luận về layout, responsive, accessibility là NOT VERIFIED.

## Lỗi đã xác nhận (chi tiết ở 07-ISSUES.md)

REX-001, REX-002, REX-003, REX-004, REX-005, REX-008, REX-010, REX-011, REX-013, REX-014, REX-015, REX-016, REX-017, REX-018, REX-020, REX-022, REX-023.

## Lỗi đã sửa

REX-001, REX-002, REX-003, REX-004, REX-005, REX-007, REX-008, REX-011, REX-018, REX-021, REX-023, REX-A01.

## Lỗi chưa sửa (có lý do)

- REX-006: cần thiết kế retry riêng (transaction poisoning) — P2.
- REX-009, REX-010, REX-013, REX-014, REX-015, REX-016, REX-017, REX-020, REX-022: cải thiện/thiếu tính năng, ngoài phạm vi sửa an toàn trong phiên này.

## Test đã chạy

`mvn -B test` → **190 tests, 0 failures, 0 errors, 9 skipped, BUILD SUCCESS**.

## Test thất bại

Không còn. (Trong quá trình sửa có 2 test cũ phải cập nhật vì chúng stub đúng hành vi lỗi — xem 08-FIX-LOG.md.)

## Vấn đề còn tồn tại

Xem phần "Rủi ro chưa xử lý" trong `10-FINAL-REPORT.md`.