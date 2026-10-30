# Báo cáo frontend

## Smoke UI và baseline

Đã dùng Chromium trên tiến trình `http://localhost:8080` có sẵn trước đợt sửa, thăm 8 route tại 375×812, 390×844, 768×1024, 1366×768 và 1920×1080 (40 lượt điều hướng). Quan sát được: tất cả phản hồi 200, không overflow ngang, ảnh hỏng hoặc pageerror. Ảnh baseline nằm trong `target/audit-screenshots/` (ignored). Kết quả này thuộc tiến trình trước sửa.

Lighthouse baseline mobile: Performance 64, Accessibility 82, Best Practices 77, SEO 91. Desktop: 85/83/77/91. Các điểm yếu thấy được gồm font CSP, contrast ngày bài, thiếu description, nhãn select, heading footer và aria-label chat. Lighthouse report JSON ở `target/lighthouse-mobile.json` và `target/lighthouse-desktop.json`, ignored. Chỉ số chưa đo lại sau sửa.

## Sửa và test hồi quy

- Trang chủ: thêm meta description và label liên kết select lịch chiếu `quickCinemaSelect`.
- Footer: chuyển tiêu đề cột từ h6 thành h2; CSS đi theo selector mới.
- Chat: bỏ `aria-label` khỏi div không tương tác; đặt tên accessible lên button mở chat.
- Ngày bài viết: nâng màu chữ lên `#a8acb5`.
- CSP: cho phép Google Fonts và favicon public.
- Test hợp đồng `UiDesignSystemRegressionTest` xác nhận cấu trúc/nhãn. Bộ test đó pass sau chỉnh sửa.

## Hạn chế

Build cập nhật không chạy được trên listener cô lập trong phiên này; vì thế các sửa trên chỉ được xác nhận bằng regression test tĩnh, không bằng browser/Lighthouse sau sửa. Firefox và WebKit chưa cài; Playwright hiện có 23 test là XSS suite, không phải booking E2E. LCP baseline mobile chậm (15.2 giây theo Lighthouse); chưa xác định/đo nguyên nhân sau sửa. Xem `REMAINING_ISSUES.md`.
