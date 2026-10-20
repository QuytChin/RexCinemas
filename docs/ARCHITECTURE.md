# Kiến trúc RexCinemas

RexCinemas là ứng dụng Spring Boot MVC dùng Thymeleaf và MySQL 8.

```text
Trình duyệt
  ├─ MVC / Thymeleaf → Controller → Service → JPA/Repository → MySQL
  ├─ REST API        → Controller → Service → JPA/Repository → MySQL
  └─ WebSocket       ← cập nhật trạng thái ghế

Booking → BANK_QR / VietQR → SePay webhook
```

## Thành phần chính

- Java 21, Spring Boot 3.3, Spring MVC, Thymeleaf, Spring Data JPA/Hibernate.
- Spring Security và JWT; toàn bộ `/admin/**` yêu cầu quyền `ADMIN`.
- WebSocket STOMP/SockJS để cập nhật trạng thái ghế cho client đang theo dõi suất chiếu.
- MySQL 8 là hệ quản trị dữ liệu duy nhất được hỗ trợ. Schema phát hành nằm tại `database/mysql/schema.sql`; Hibernate chỉ kiểm tra bằng `validate`.
- ZXing tạo ảnh QR. Quét camera cần `localhost` hoặc HTTPS.
- Cloudinary, SMTP và SePay là tích hợp tùy chọn theo cấu hình môi trường.

## Nghiệp vụ có trong source

- Quản lý phim, rạp, phòng chiếu, ghế, suất chiếu và nội dung.
- Giữ ghế có thời hạn, checkout, combo, voucher, QR check-in và thông báo realtime.
- Thanh toán online duy nhất trong checkout là `BANK_QR`. SePay webhook được xác thực theo cấu hình trước khi ghi nhận giao dịch.
- Rex Rewards quản lý điểm, hạng thành viên, voucher và lịch sử biến động.
- OTP/email phục vụ xác thực tài khoản và đặt lại mật khẩu khi SMTP được cấu hình.

## Giới hạn cần biết

- Không có MoMo, VNPAY hoặc thanh toán trực tiếp tại quầy trong luồng checkout hiện tại.
- Hoàn tiền là quy trình đối soát: ứng dụng ghi nhận trạng thái, còn việc chuyển tiền thực tế do ADMIN thực hiện ngoài hệ thống.
- `CINEMA_MANAGER` tồn tại để tương thích bản ghi cũ; không được seed/cấp mới và không có quyền `/admin/**`.
- Build/test tự động không thay thế việc xác minh với dịch vụ ngoài, giao diện trình duyệt hoặc tải thực tế.
