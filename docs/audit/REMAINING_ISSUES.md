# Vấn đề còn tồn đọng và giới hạn kiểm thử

## Cần ưu tiên xử lý

1. **MySQL concurrency cho hold/payment/check-in:** concurrency regression mới chạy H2. Chạy riêng trên MySQL clone/test DB với backup, kiểm tra unique conflict, lock order, idempotency và deadlock retry.
2. **Đặt vé và thanh toán E2E:** chưa chạy luồng chọn ghế → booking → mock payment → vé/QR trên bản mới; không khẳng định không double-book trên MySQL.
3. **SpotBugs:** còn 136 cảnh báo. 119 là EI_EXPOSE_REP/REP2 (76 + 43); phần còn lại: CT_CONSTRUCTOR_THROW 6, VA_FORMAT_STRING_USES_NEWLINE 3, JLM synchronized-on-monitor 3, REC_CATCH_EXCEPTION 2, và 3 loại khác mỗi loại 1. Cần triage từng issue; không suppress hàng loạt. SpotBugs `check` hiện fail.
4. **Giao diện sau sửa:** Lighthouse sau sửa chưa có. Accessibility/SEO/Performance fixes mới có contract tests; LCP mobile baseline còn 15.2s.
5. **Cross-browser:** Firefox/WebKit chưa cài; không có kết quả từ hai browser này.

## Các vấn đề nghiệp vụ/security được giữ lại từ audit nền

- REX-009: JWT trong response body API login (thiết kế client), cần quyết định tương thích.
- REX-010: GET có side effect/lazy initialization, cần xem từng tuyến và yêu cầu nghiệp vụ.
- REX-013: giá sản phẩm trong tư vấn AI hard-code.
- REX-014: thiếu hoàn điểm khi hủy voucher đổi thưởng.
- REX-015: chính sách password đăng ký yếu/chưa thống nhất.
- REX-017: query lịch sử hội thoại chưa có giới hạn.
- REX-019: khóa toàn bộ seat map là lựa chọn hiệu năng cần benchmark.
- REX-020: quote chưa kiểm tra hết hạn giữ ghế.
- REX-022: rate limit cho AI/login cần đánh giá chính sách và xác minh thực tế.

Chi tiết baseline/nhãn từng issue trong `BUG_LIST.md` và `docs/audit/07-ISSUES.md`. Mục nào chưa tái hiện trong phiên này giữ `UNRESOLVED`.

## Công cụ/môi trường

- SpotBugs chạy được nhưng gate thất bại vì warnings; báo cáo XML `target/spotbugsXml.xml`.
- Semgrep scan 283 file tracked; 68 file bị ignore.
- Playwright Chromium chạy được; Firefox/WebKit chưa cài.
- Bản app cô lập trên 8081 không khởi động thành listener trong phiên kiểm tra; app có sẵn ở 8080 không bị dừng hay thay thế.
- Không chạy dependency CVE scan, không gọi API bên ngoài, thanh toán thật, SMTP thật hoặc Cloudinary upload.
- Backup đã tạo và đọc được nhưng chưa restore thử trên database cô lập.
