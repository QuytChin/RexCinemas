# Báo cáo kiểm toán tổng thể RexCinemas

**Ngày chạy:** 30/10/2026 · **Phạm vi:** workspace `D:\RexCinemas` và MySQL local `localhost:3306/rex_chain_cinema` · **Môi trường:** Windows 11, Java 21.0.12, Spring Boot 3.3.5, Maven 3.10.0, MySQL 8.0.46.

## Kết quả

- Maven: 245 test, 0 lỗi, 0 thất bại, 10 skipped. Bộ test chạy bằng profile `test` và H2; không trỏ vào MySQL.
- Playwright: 23/23 test XSS qua trên Chromium. Smoke UI trước/sau gồm 8 route và 5 viewport: 40 response đều HTTP 200, không overflow, ảnh hỏng hay lỗi JavaScript quan sát được. Firefox/WebKit chưa cài.
- Semgrep: 160 rules, 283 file được quét, 0 phát hiện sau khi sửa workflow.
- SpotBugs: `check` trả lỗi do còn 136 cảnh báo. Phần lớn là `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` từ quan hệ JPA, bean Spring hoặc DTO mutable; chưa coi là 136 lỗi ứng dụng đã tái hiện. Có nhóm cảnh báo cần triage thêm, được liệt kê trong `REMAINING_ISSUES.md`.
- Lighthouse baseline trên tiến trình localhost đang chạy trước các sửa đổi: mobile Performance 64, Accessibility 82, Best Practices 77, SEO 91; desktop 85/83/77/91. Không thể khởi động bản nguồn mới ở cổng cô lập trong phiên kiểm tra, nên các chỉ số này **không phải kết quả sau sửa**.
- MySQL đã xác minh đúng host, port và database; tạo và đọc được backup. Kiểm tra dữ liệu là read-only ngoài đăng nhập/đăng xuất thử nghiệm. Không sửa schema, tài khoản hay dữ liệu booking.

## Sửa đổi có bằng chứng

1. `SeatReservationService` khóa hàng người dùng trước lần tạo hold session đầu tiên, ngăn hai transaction cùng user tranh insert vào khóa duy nhất `(user_id, showtime_id)`. Thêm test đồng thời trên H2; kết quả nằm trong 245 test. Do chưa chạy concurrency trên MySQL, mục này mang trạng thái `PARTIALLY_FIXED`.
2. Cấu hình mail bỏ email Gmail mặc định được hard-code; cấu hình yêu cầu phải được cấp qua môi trường.
3. CSP cho phép Google Fonts từ hai host cần thiết; test hợp đồng xác nhận policy. Thêm `/favicon.ico` vào danh sách public.
4. Sửa các vấn đề accessibility thấy ở Lighthouse baseline: mô tả trang chủ, nhãn select lịch chiếu, cấu trúc heading footer, tên truy cập nút chat và tương phản ngày bài viết. Có test hợp đồng HTML; Lighthouse sau sửa chưa chạy.
5. Ghim ba GitHub Actions vào commit SHA bất biến và giới hạn quyền workflow `contents: read`. Semgrep sau sửa không còn phát hiện action dùng tag mutable.
6. Xóa dead store và private overload không được gọi được SpotBugs báo.

## Giới hạn và rủi ro còn lại

Không gọi SePay/webhook, Cloudinary upload, Grok, SMTP thật hoặc giao dịch ngân hàng. Chưa kiểm tra E2E đặt vé/thanh toán trên MySQL, double booking đa tiến trình trên MySQL, webhook concurrency thực, toàn bộ ma trận IDOR bằng request thực, Firefox/WebKit, và Lighthouse sau sửa. Xem báo cáo thành phần để biết trạng thái từng mục. Không kết luận toàn bộ hệ thống đã đạt hoặc đã kiểm thử 100%.

`docs/audit/` còn các báo cáo lịch sử từ đợt audit trước. Tám tệp mới `FULL_AUDIT_REPORT.md`, `BUG_LIST.md`, `SECURITY_REPORT.md`, `DATABASE_REPORT.md`, `FRONTEND_REPORT.md`, `TEST_RESULTS.md`, `FIX_CHANGELOG.md` và `REMAINING_ISSUES.md` là snapshot hiện tại ngày 30/10/2026.
