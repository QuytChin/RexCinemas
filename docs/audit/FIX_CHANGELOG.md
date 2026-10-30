# Nhật ký sửa đổi

| Nhóm | Thay đổi | Bằng chứng xác minh |
|---|---|---|
| Booking hold | Khóa user row trước khi tạo hold session lần đầu; deadline làm tròn microsecond; regression concurrency H2. | Maven suite 245 test pass; MySQL concurrency chưa chạy. |
| Secret/config | Bỏ Gmail username hard-code mặc định; cập nhật contract test Flyway theo cấu hình hiện hành. | Runtime configuration tests pass. |
| Security headers | CSP cho Google Fonts; public root favicon; regression contract test. | Runtime configuration test pass; browser sau sửa chưa chạy. |
| Frontend accessibility | Meta description; label select; footer heading cấp 2; chat button accessible name; tăng contrast ngày bài. | UiDesignSystemRegressionTest pass; Lighthouse chưa chạy lại. |
| GitHub Actions | Pin checkout/setup-node/upload-artifact SHA; `contents: read`. | Semgrep 0 finding trong workflow; 275 file scan. |
| Code quality | Xóa dead store trong `PaymentService` và overload không dùng của `CinemaApiController`. | SpotBugs count từ 138 còn 136; gate vẫn fail. |
| Test helper | Bổ sung concurrent hold regression và UI/CSP contract tests. | Maven 245/245 pass, 10 skipped. |

Không có thay đổi schema hoặc data nghiệp vụ. Không commit/push/deploy. Backup MySQL nằm trong `backups/` (ignored).
