# 10 — BÁO CÁO CUỐI

## 1. Tổng số file source

- **325 file văn bản / 25.194 dòng** thuộc phạm vi (đã loại `target/`, `node_modules/`, `.git/`, báo cáo Playwright và ảnh nhị phân).
- Trong đó: 170 file Java main, 53 file Java test, 48 template, 17 JS, 1 CSS, 4 SQL, 5 properties, 5 markdown, 1 workflow CI, cùng cấu hình/script khác.

## 2. Số file đã đọc và tỷ lệ

| Nhóm | Đọc toàn văn | Tỷ lệ trên nhóm |
|---|---|---|
| Java `src/main/java` (170) | 170 | **100%** |
| Java `src/test` (53) | 7 | 13% |
| Template (48) | 7 | 15% |
| JS (17) | 7 | 41% |
| CSS (1) | 0 | 0% |
| SQL (4) + properties (5) + CI/docs | 100% | 100% |

Toàn bộ file **có chứa logic nghiệp vụ** (Java main) đã đọc hết. Các phần chưa đọc là template trình bày, test phụ, và CSS — được ghi rõ là NOT VERIFIED.

## 3. Số module đã kiểm tra

11/11 module: Auth & OTP · Phim/Rạp/Phòng/Ghế · Suất chiếu · Đặt vé & giữ ghế · Thanh toán SePay · Vé & QR check-in · Rex Rewards · Hỗ trợ chat/AI · Thông báo · Nội dung · Admin Dashboard/Báo cáo/Hậu kiểm.

## 4. Số lỗi phát hiện

**24** mục: REX-001..REX-023 tái xác minh + REX-A01 (mới, do chủ dự án báo). Phân loại: 17 BUG, 3 DESIGN, 1 MISSING, 2 IMPROVEMENT/NOT VERIFIED.

## 5. Số lỗi đã sửa

**13**: REX-001, REX-002, REX-003, REX-004, REX-005, REX-007, REX-008, REX-011, REX-012, REX-018, REX-021, REX-023, REX-A01.

Trong đó 4 lỗi HIGH (REX-001, REX-002, REX-004, REX-A01) và 4 lỗi MEDIUM (REX-003, REX-005, REX-007, REX-008).

## 6. Số lỗi còn lại

- P2 (cần thiết kế riêng): **REX-006**.
- Còn lại (P3/cải thiện): REX-009, REX-010, REX-013, REX-014, REX-015, REX-016, REX-017, REX-019, REX-020, REX-022.
- Tổng: **11** mục chưa xử lý, không mục nào gây mất dữ liệu hoặc đặt trùng ghế.

## 7. Kết quả compile và test

- `mvn -DskipTests compile`: **BUILD SUCCESS**
- `mvn test`: **190 bài, 0 failure, 0 error, 9 skipped, BUILD SUCCESS**

## 8. Rủi ro chưa xử lý

| Rủi ro | Mức | Ghi chú |
|---|---|---|
| MySQL thật chưa được kiểm chứng (schema 36 bảng, index mới, collation) | CAO khi deploy | Cần chạy `schema.sql` trên DB trống + `ALTER TABLE` thêm index cho DB hiện hữu |
| Đồng thời thật (2 user cùng ghế, 2 webhook trùng) chưa test tải | TRUNG BÌNH | Logic dựa trên khóa bi quan + UNIQUE; cần test tích hợp MySQL |
| `seat_hold_sessions` UNIQUE violation khi double-click (REX-006) | THẤP | Trả 500 thay vì 409 |
| Cấu hình timeout thanh toán không có tác dụng (REX-007) | THẤP | Gây hiểu nhầm cho admin |
| CI không chạy test dự án (REX-016) | TRUNG BÌNH | Có thể merge lỗi mà CI vẫn xanh |
| Không có rate limit đăng nhập (REX-022) | THẤP | BCrypt giảm tốc độ brute-force |
| UI/responsive/accessibility chưa review (`app.css`) | TRUNG BÌNH | Cần review thủ công/Playwright |

## 9. Có sẵn sàng demo không?

**Có.** Luồng chính hoạt động và đã được kiểm bằng test tự động: đăng ký OTP, đăng nhập, giữ ghế, xác nhận, mô phỏng xác nhận thanh toán, check-in, rewards.
Ba lỗi nghiêm trọng nhất (rò rỉ hội thoại, tồn kho/voucher sai, ghi chéo hội thoại) đã được sửa nên demo an toàn hơn trước.
Điều kiện: dùng profile `dev` với H2/MySQL test, không bật `SEPAY_ENABLED` nếu chưa cấu hình.

## 10. Có sẵn sàng production không?

**Chưa.** Cần tối thiểu:
1. Chạy `schema.sql` (đã có index mới) + script nâng cấp trên MySQL 8 thật, backup trước.
2. Test tích hợp/đồng thời trên MySQL thật (9 bài đang skip).
3. Thêm job Maven vào CI (REX-016) và một E2E Playwright thật có `webServer`.
4. Quyết định REX-007 (timeout) và xử lý REX-006, REX-009, REX-022.
5. Cấu hình bí mật qua biến môi trường: `JWT_SECRET` (≥32 byte), DB, SMTP, Cloudinary, SePay + `app.websocket.allowed-origins` cho domain thật.