# Báo cáo bảo mật

## Đã kiểm tra

- Semgrep `p/java` + `p/owasp-top-ten`: 160 rule, 283 file, 0 finding sau khi sửa CI. 68 file bị ignore theo `.semgrepignore`; kết quả chỉ áp dụng file tracked.
- Đã đọc cấu hình Spring Security/CSP, JWT, CSRF, controller phân quyền, SePay webhook và các audit nền. SpotBugs và Playwright XSS bổ sung kiểm tra mã Java và đầu vào chat.
- Playwright XSS: 23 ca trên Chromium qua; các payload thử được giữ inert theo test hiện có.
- Kiểm tra thực tế đăng nhập bảy user local: admin, manager, hai supervisor, hai staff và customer đều đăng nhập được với ứng viên mật khẩu test do chủ dự án cung cấp; mã xác minh chỉ so BCrypt, không ghi mật khẩu/hash. Quyền route được smoke test: các route phù hợp trả 200; supervisor vào payments, staff vào showtimes, customer vào admin bị 403. Manager truy cập giao dịch được phép theo policy trong `SecurityConfig`.
- Logout thực tế xóa access token cookie; profile sau logout chuyển về login.
- GitHub Actions: ba ref đã pin SHA và workflow chỉ cần `contents: read`.

## Thay đổi

- `SecurityConfig.java:82`: cho phép favicon root mà không yêu cầu đăng nhập.
- `SecurityConfig.java:113-114`: CSP cho phép Google Fonts cần thiết, không nới `script-src`.
- `application.properties:87`: bỏ địa chỉ Gmail mặc định.
- `.github/workflows/playwright.yml:11-24`: action bất biến và quyền workflow tối thiểu.

## Chưa xác minh

Chưa chạy kiểm thử IDOR toàn tuyến bằng ma trận request cho mọi controller; chưa chạy browser role UI trên build mới; chưa audit CVE dependency bằng dependency-check; chưa test rate limit AI/đăng nhập ở tải thực; chưa test HMAC/replay webhook với endpoint runtime. Không gọi service bên ngoài hoặc webhook. Không ghi credential vào báo cáo. Danh sách vấn đề theo REX-009, REX-015, REX-022 và các giới hạn khác nằm trong `BUG_LIST.md`/`REMAINING_ISSUES.md`.
