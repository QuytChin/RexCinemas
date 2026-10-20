# Kiến trúc RexCinemas

Tài liệu này mô tả luồng và giới hạn được xác nhận trong source hiện tại. Đây không phải cam kết về khả năng vận hành production hoặc tích hợp với dịch vụ bên ngoài.

## Công nghệ và cấu trúc

- Java 21, Spring Boot 3.3.5, Spring MVC/Thymeleaf và Spring Data JPA/Hibernate.
- Spring Security với JWT; các trang quản trị `/admin/**` yêu cầu quyền `ADMIN`.
- WebSocket STOMP/SockJS dùng để phát trạng thái ghế cho client đang theo dõi suất chiếu.
- MySQL là cấu hình phát triển thường dùng; repository có cấu hình và driver cho PostgreSQL, SQL Server. Cần dùng đúng schema/migration tương ứng trước khi chọn database khác.
- ZXing tạo ảnh QR; giao diện quét camera dùng thư viện JavaScript bên ngoài và yêu cầu `localhost` hoặc HTTPS.
- Cloudinary được dùng cho luồng tải ảnh khi đã cấu hình thông tin dịch vụ.

```text
Trình duyệt
   ├── MVC/Thymeleaf ── Controller ── Service ── Repository/JPA ── Database
   ├── REST API ─────── Controller ── Service ── Repository/JPA ── Database
   └── WebSocket <───── cập nhật trạng thái ghế

Booking ── BANK_QR/VietQR ── SePay webhook
```

## Nghiệp vụ hiện có

- Danh mục phim, rạp, phòng chiếu, ghế và suất chiếu; quản trị qua các trang Admin.
- Kiểm tra trùng lịch phòng chiếu dựa trên thời lượng phim cộng thời gian dọn phòng 15 phút.
- Giữ ghế có thời hạn, giới hạn số ghế trong một booking và cập nhật trạng thái qua WebSocket.
- Đặt vé trực tuyến qua `BANK_QR` (VietQR/SePay). Webhook được xác thực theo cấu hình, đối chiếu thông tin giao dịch và ghi nhận giao dịch để xử lý trạng thái booking.
- Booking thanh toán muộn hoặc cần xử lý hoàn tiền có trạng thái chờ phù hợp; việc chuyển tiền hoàn thực hiện ngoài hệ thống, ADMIN ghi nhận sau khi đối soát.
- Combo có trường số lượng tồn; luồng checkout kiểm tra số lượng và cập nhật tồn kho.
- Vé có QR/token để check-in. Có thể quét bằng camera hoặc nhập mã; nghiệp vụ chặn check-in lặp theo trạng thái vé/booking.
- Rex Rewards gồm cấu hình tích điểm, hạng thành viên, voucher và lịch sử biến động điểm.
- OTP/email được dùng trong các luồng xác thực tài khoản và đặt lại mật khẩu nếu SMTP được cấu hình.

## Phân quyền

- `CUSTOMER`: luồng người dùng như xem nội dung, đặt vé, hồ sơ và Rewards.
- `ADMIN`: quản trị hệ thống, bao gồm check-in và các trang `/admin/**`.
- `CINEMA_MANAGER` còn trong enum phục vụ tương thích với dữ liệu cũ; hiện không được seed/cấp mới và không có quyền truy cập `/admin/**`.
- Chưa có phân quyền nhân viên/quản lý giới hạn theo từng cụm rạp.

## Các trạng thái và giới hạn tích hợp

- Phương thức thanh toán trong checkout hiện tại chỉ là `BANK_QR`; không mô tả MoMo, VNPAY hoặc thanh toán tại quầy như chức năng đang hoạt động.
- SePay cần thông tin tài khoản, secret, cấu hình webhook và URL public hợp lệ. Có code xử lý callback không đồng nghĩa đã xác minh giao dịch thật với ngân hàng/SePay.
- Trạng thái `REFUND_PENDING`/trạng thái giao dịch chờ hoàn tiền hỗ trợ đối soát. Ứng dụng không tự chuyển tiền hoàn.
- SMTP, Cloudinary, camera và webhook phụ thuộc dịch vụ/môi trường ngoài; cần kiểm tra riêng khi triển khai.
- Các script trong `src/main/resources/db` có phạm vi khác nhau: một số là migration tăng phiên bản, không phải script dựng schema mới hoàn chỉnh. Xem nội dung và hướng dẫn của đúng hệ quản trị trước khi chạy.

## Đường dẫn mã nguồn

- `controller`: endpoint MVC/REST và điều phối request.
- `service`: booking, giữ ghế, thanh toán, check-in, rewards và các nghiệp vụ khác.
- `entity`, `repository`: mô hình dữ liệu và truy vấn JPA.
- `templates`, `static`: Thymeleaf, CSS, JavaScript và tài nguyên giao diện.
- `src/test`: unit, MVC, regression và một số integration test; kết quả test không thay thế kiểm thử dịch vụ bên ngoài, UI trên trình duyệt hay kiểm thử tải.
