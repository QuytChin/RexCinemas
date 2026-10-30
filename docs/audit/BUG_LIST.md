# Danh sách lỗi và trạng thái

Trạng thái dùng theo yêu cầu: `FIXED`, `VERIFIED`, `PARTIALLY_FIXED`, `UNRESOLVED`, `BLOCKED`, `NOT_REPRODUCIBLE`.

## Phát hiện trong đợt này

| ID | Mức độ | Module / vị trí | Phát hiện và nguyên nhân | Sửa / xác minh | Trạng thái |
|---|---|---|---|---|---|
| AUD-001 | MEDIUM | Booking hold — `SeatReservationService.java:89,155,180` | Hai request giữ ghế đầu tiên đồng thời của cùng user có thể cùng thấy chưa có `seat_hold_sessions` rồi tranh insert, vi phạm UNIQUE. | Khóa hàng user trước khi tạo session; chuẩn hóa deadline microsecond. Test đồng thời H2 qua trong Maven suite; chưa thử MySQL concurrency. | `PARTIALLY_FIXED` |
| AUD-002 | MEDIUM | Cấu hình mail — `application.properties:87` | Có địa chỉ Gmail mặc định trong cấu hình, có nguy cơ dùng nhầm tài khoản khi thiếu env. | Mặc định rỗng; test cấu hình hiện có qua. | `VERIFIED` |
| AUD-003 | MEDIUM | CSP — `SecurityConfig.java:113-114` | CSP không cho tải Google Fonts đang được trang sử dụng; browser báo lỗi CSP. | Cho phép đúng `fonts.googleapis.com` và `fonts.gstatic.com`; test hợp đồng qua. Chưa đo Lighthouse trên build mới. | `PARTIALLY_FIXED` |
| AUD-004 | LOW | UI/accessibility — `index.html:41`, `footer.html`, `chat-widget.html`, `app.css` | Thiếu label select, heading footer sai cấp, aria-label đặt ở div thay vì control, tương phản ngày bài viết thấp. | Sửa HTML/CSS và thêm regression test hợp đồng; test qua. Kiểm tra bằng trình duyệt sau sửa chưa có. | `PARTIALLY_FIXED` |
| AUD-005 | LOW | CI supply chain — `.github/workflows/playwright.yml:11-24` | Ba action dùng tag mutable. | Ghim SHA, đặt `contents: read`; Semgrep lần cuối 0 finding. | `VERIFIED` |
| AUD-006 | LOW | Code quality — `PaymentService.java`, `CinemaApiController.java` | Dead store và private overload không được gọi. | Xóa mã chết; SpotBugs tổng số giảm từ 138 xuống 136. | `VERIFIED` |
| AUD-007 | INFO | Phân tích tĩnh — toàn Java | SpotBugs còn 136 cảnh báo; đây là tồn đọng phân tích, chưa chứng minh 136 lỗi runtime. | Giữ nguyên, không suppress hàng loạt. Xem `REMAINING_ISSUES.md`. | `UNRESOLVED` |

## Các vấn đề còn được theo dõi từ danh sách REX trước đây

Các mục dưới đây là trạng thái theo audit nền trước đó (`docs/audit/07-ISSUES.md`), không đồng nghĩa đã tái hiện lại trong phiên này. Những mục đã có test hồi quy nằm trong Maven suite hiện tại; hành vi production vẫn cần kiểm thử bổ sung khi phần việc yêu cầu.

| ID | Mức | Mô tả | Trạng thái snapshot |
|---|---|---|---|
| REX-001 | HIGH | Quyền subscribe WebSocket | `VERIFIED` theo test hồi quy nền |
| REX-002 | HIGH | Tồn kho combo khi booking PENDING hết hạn | `VERIFIED` theo test hồi quy nền |
| REX-003 | MEDIUM | Hoàn lượt voucher sai | `VERIFIED` theo test hồi quy nền |
| REX-004 | HIGH | Ghi chéo hội thoại hỗ trợ khi chưa đăng nhập | `VERIFIED` theo test hồi quy nền |
| REX-005 | MEDIUM | Khớp mã thanh toán bằng `contains` | `VERIFIED` theo test hồi quy nền |
| REX-006 | MEDIUM | Concurrent tạo hold session | Được thay bằng AUD-001, `PARTIALLY_FIXED` |
| REX-007 | MEDIUM | Timeout BANK_QR cố định theo hold 5 phút | `VERIFIED` theo test hồi quy nền |
| REX-008 | MEDIUM | CSRF bypass bằng Bearer header bất kỳ | `VERIFIED` theo test hồi quy nền |
| REX-009 | MEDIUM | JWT trả trong body API login | `UNRESOLVED` — thiết kế API client |
| REX-010 | LOW | GET có side effect/lazy initialization | `UNRESOLVED` — rà soát từng endpoint |
| REX-011 | LOW | Index `(status, expires_at)` | `VERIFIED`; index tồn tại trên MySQL local |
| REX-012 | LOW | Parse transaction date thiếu offset | `VERIFIED` theo test nền |
| REX-013 | LOW | Giá hard-code trong AI tư vấn | `UNRESOLVED` |
| REX-014 | LOW | Thiếu hoàn điểm khi hủy voucher đổi thưởng | `UNRESOLVED` |
| REX-015 | LOW | Chính sách mật khẩu đăng ký yếu | `UNRESOLVED` |
| REX-016 | LOW | CI thiếu test ứng dụng tổng quát | `UNRESOLVED`; workflow hiện vẫn chạy bộ Playwright repository |
| REX-017 | LOW | Conversation query không giới hạn | `UNRESOLVED` |
| REX-018 | LOW | requestId cho phép dấu chấm | `VERIFIED` theo test nền |
| REX-019 | LOW | Khóa toàn seat-map khi giữ ghế | `UNRESOLVED` — design tradeoff/performance |
| REX-020 | LOW | Quote không kiểm holdUntil | `UNRESOLVED` |
| REX-021 | LOW | Race giải phóng ghế sau commit booking | `VERIFIED` theo test nền |
| REX-022 | LOW | Rate limit đăng nhập/AI | `UNRESOLVED`; kiểm tra sâu theo policy chưa hoàn tất |
| REX-023 | LOW | NPE khi search không có sort | `VERIFIED` theo test nền |

## Lỗi môi trường / chưa tái hiện

Không có compile error trong Maven suite. Lần khởi động app H2 cô lập bằng `spring-boot:run` không tạo listener hoạt động và không sinh log; vì vậy không thể xác nhận trình duyệt trên build mới. Đây là giới hạn verification, không kết luận app không thể chạy ở cấu hình người dùng.
