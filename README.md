# Rex Chain Cinema — Spring Boot + QR Check-in

Hệ thống quản lý chuỗi rạp và đặt vé xem phim trực tuyến bằng **Spring Boot + Thymeleaf + Bootstrap + JPA + MySQL/PostgreSQL/SQL Server + JWT + WebSocket + Cloudinary**, có luồng booking realtime và **vé điện tử QR check-in tại cổng**.

Ứng dụng không cung cấp secret hay mật khẩu mặc định. Hãy cấu hình qua environment variables hoặc secret store; không commit file `.env`. Chạy local với profile `dev,mysql`, còn production dùng `prod,mysql`. `JWT_SECRET` phải có ít nhất 32 byte UTF-8. `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME` và `MAIL_PASSWORD` là bắt buộc; ứng dụng dừng sớm nếu thiếu. Production bật cookie `Secure` và yêu cầu HTTPS.

### Cấu hình local

Sao chép `.env.example` thành `.env.local` để làm danh sách biến cần cấu hình. Spring Boot không tự nạp file `.env`; hãy nhập các giá trị vào cấu hình môi trường của IDE/terminal/secret manager. Dùng giá trị riêng cho máy local, không tái sử dụng credential production.

PowerShell ví dụ (đặt giá trị trực tiếp tại terminal riêng của bạn, không lưu vào source):

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/rex_chain_cinema?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "<local-db-user>"
$env:DB_PASSWORD = "<local-db-password>"
$env:JWT_SECRET = "<unique-random-secret-at-least-32-bytes>"
$env:MAIL_USERNAME = "<smtp-account>"
$env:MAIL_PASSWORD = "<smtp-app-password>"
```

Production: đặt `SPRING_PROFILES_ACTIVE=mysql,prod` (profile `prod` đứng sau để ghi đè tùy chọn môi trường), cấu hình các biến bắt buộc qua secret store, dùng HTTPS và chỉ định migration ngoài ứng dụng. Profile `prod` bật cookie `Secure`, dùng Hibernate `validate` và không chạy SQL init lúc khởi động.

### Xoay vòng credential đã từng xuất hiện trong Git

Các commit cũ có chứa credential; thay đổi file hiện tại không xóa chúng khỏi lịch sử. Thực hiện qua chủ sở hữu dịch vụ theo trình tự, rồi cập nhật secret store và khởi động lại ứng dụng:

1. Tạo `JWT_SECRET` mới, tối thiểu 32 byte và riêng cho môi trường. Sau khi triển khai, mọi JWT hiện hành sẽ mất hiệu lực và người dùng cần đăng nhập lại.
2. Thu hồi Gmail App Password cũ trong tài khoản Google, tạo App Password mới và cập nhật `MAIL_PASSWORD` cùng `MAIL_USERNAME` nếu cần.
3. Đổi mật khẩu MySQL bằng tài khoản DBA theo quy trình vận hành, cập nhật `DB_USERNAME`/`DB_PASSWORD`, rồi triển khai cấu hình đồng bộ. Không đổi credential từ ứng dụng này.
4. Kiểm tra login, OTP qua mail thử nghiệm, kết nối database staging, rồi mới triển khai production.

Không paste credential vào ticket, chat, log hoặc Git. `.env.example` chỉ chứa placeholder.

## 1. Luồng hoàn chỉnh

```text
Trang chủ / Lịch chiếu
        ↓
Chọn suất đang mở bán
        ↓
Đăng nhập (nếu cần) → quay lại đúng suất
        ↓
Chọn ghế realtime WebSocket
        ↓
Combo / voucher
        ↓
Thanh toán
        ↓
Booking CONFIRMED
        ↓
Vé điện tử + QR
        ↓
Admin/Manager quét QR tại /admin/checkin
        ↓
CHECKED-IN
```

## 2. Stack
- Java 21
- Spring Boot 3.3.5
- Thymeleaf + Bootstrap 5.3
- Spring Data JPA / Hibernate
- Spring Security + JWT HttpOnly cookie / Bearer token
- WebSocket STOMP + SockJS
- MySQL / PostgreSQL / SQL Server
- Cloudinary
- MoMo sandbox / VNPAY sandbox
- Chart.js
- ZXing Core 3.5.4 để sinh QR
- html5-qrcode 2.3.8 để quét QR bằng camera trên trình duyệt

## 3. Lịch chiếu và đặt vé

Trang:
```text
/schedule
```

Có thể lọc theo:
- ngày;
- rạp;
- phim.

Mỗi suất hiển thị:
- giờ chiếu;
- phòng;
- giá chuẩn;
- số ghế còn / tổng ghế;
- nút **ĐẶT VÉ** nếu còn ghế.

Nếu chưa đăng nhập, hệ thống chuyển đến login rồi tự quay lại đúng `/booking/{showtimeId}`.

Trang chủ còn có khu **Đặt nhanh – Suất chiếu sắp tới**.

## 4. Booking 4 bước

```text
1. Chọn ghế
2. Combo / bắp nước
3. Thanh toán
4. Vé điện tử
```

- STANDARD / VIP / COUPLE.
- WebSocket cập nhật ghế realtime.
- Giữ ghế 5 phút.
- Tối đa 10 ghế/booking.
- Combo tối đa 10 phần/món.
- Voucher.
- PAY_AT_COUNTER / MOMO_SANDBOX / VNPAY_SANDBOX.
- Online booking `PENDING -> CONFIRMED/CANCELLED`.
- Booking pending quá hạn tự trả ghế.

## 5. Vé QR

Booking `CONFIRMED` có QR thật tại:
```text
/bookings/{id}
```

Ảnh QR:
```text
GET /bookings/{id}/qr
```

QR không chứa ID booking. Nội dung là token bí mật ngẫu nhiên:
```text
REXCINEMAS:TICKET:<64-character-random-token>
```

Booking cũ từ V3 vẫn dùng được: khi mở QR lần đầu, token sẽ được tạo tự động.

Vé có:
- tên phim;
- rạp / phòng;
- giờ chiếu;
- ghế;
- combo/voucher;
- tổng tiền;
- mã booking;
- QR;
- trạng thái check-in;
- nút In / lưu PDF.

## 6. Quét QR check-in

Đăng nhập bằng ADMIN hoặc CINEMA_MANAGER và mở:
```text
/admin/checkin
```

Có 3 cách:
1. Camera quét QR trên vé.
2. Chọn ảnh QR từ thiết bị (UI của html5-qrcode).
3. Nhập mã booking thủ công, ví dụ `REXABC123456`.

Backend xác thực:
- vé có tồn tại không;
- booking đã CONFIRMED chưa;
- booking có bị CANCELLED không;
- vé đã check-in chưa;
- suất chiếu đã hết thời gian check-in chưa.

Check-in thành công lưu:
```text
checkedInAt
checkedInBy
```

Quét lại cùng vé sẽ báo `ALREADY_CHECKED_IN`.

> Camera trình duyệt hoạt động tốt trên `localhost` hoặc HTTPS. Nếu camera không khả dụng, vẫn có thể nhập booking code thủ công.

## 7. Quản lý suất chiếu nâng cấp

Trang:
```text
/admin/showtimes
```

Khi tạo suất:
- tự sinh ShowtimeSeat;
- tự mở bán trên lịch chiếu;
- chặn giờ quá khứ;
- chặn giá <= 0;
- chặn trùng phòng.

Quy tắc chống chồng lịch:
```text
startTime
→ thời lượng phim
→ +15 phút dọn phòng
→ suất tiếp theo mới được bắt đầu
```

Nếu suất đã có booking chưa hủy, admin không thể đổi giờ/giá để tránh làm sai vé đã bán. Có thể **Ẩn** suất để ngừng bán thêm.

Nếu database local lâu ngày không còn suất tương lai trong 8 ngày, demo seeder sẽ tạo một lịch mới khi khởi động.

## 8. Rex Member
- 1 điểm / 10.000đ booking CONFIRMED.
- MEMBER: 0–249 lifetime points.
- SILVER: 250–599.
- GOLD: 600–1199.
- DIAMOND: 1200+.
- Admin hủy booking chưa check-in sẽ hoàn tác điểm và lượt voucher.

## 9. Admin
- Dashboard + doanh thu + AOV + top phim + top rạp.
- KPI check-in QR.
- Movie CRUD.
- Cinema CRUD.
- Auditorium + tự sinh ghế.
- Showtime + chống chồng lịch.
- Booking + trạng thái check-in.
- QR gate scanner.
- Combo.
- Voucher.
- Review.
- Event.
- Article.
- Trailer / Clip.
- User / Role (ADMIN only).

## 10. REST API

Public:
```text
GET /api/movies/now-showing
GET /api/movies/coming-soon
GET /api/movies/{id}
GET /api/movies/{id}/showtimes
GET /api/showtimes/{id}/seats
GET /api/snacks
```

Showtime API có thêm:
```json
{
  "availableSeats": 52,
  "totalSeats": 60,
  "bookingUrl": "/booking/15"
}
```

Cần JWT:
```text
GET  /api/me
GET  /api/me/bookings
GET  /api/payment-status/{bookingCode}
POST /api/showtimes/{showtimeId}/hold
POST /api/showtimes/{showtimeId}/release
POST /api/bookings/checkout
```

Booking JSON có thêm:
```text
checkedIn
checkedInAt
ticketUrl
qrImageUrl
```

## 11. Chạy trên IntelliJ IDEA 2025.3.11

1. Giải nén ZIP.
2. IntelliJ → **Open** thư mục có `pom.xml`.
3. Project SDK = **Java 21**.
4. Maven → **Reload All Maven Projects** để tải thêm ZXing 3.5.4.
5. Giữ MySQL hiện tại của bạn đang chạy.
6. Run:
```text
src/main/java/com/rexchain/cinema/RexChainCinemaApplication.java
```
7. Truy cập:
```text
http://localhost:8080
```

## 12. Database cũ V3

**Không cần xóa database.**

`spring.jpa.hibernate.ddl-auto=update` sẽ tự thêm vào bảng `bookings`:
```text
qr_token
checked_in_at
checked_in_by
```

`application-mysql.properties` trong bản này chính là cấu hình từ project local-ready đã chạy trên máy bạn.

## 13. Tài khoản khởi tạo và demo

Project không chứa mật khẩu mặc định. Trước lần chạy đầu, đặt `ADMIN_INITIAL_PASSWORD`
để tạo tài khoản quản trị tại email trong `ADMIN_INITIAL_EMAIL` (mặc định chỉ là tên đăng nhập
`admin@rexchain.vn`). Tài khoản demo chỉ được tạo khi `SEED_DEMO_USERS=true` và đã đặt cả
`DEMO_CUSTOMER_PASSWORD`, `DEMO_MANAGER_PASSWORD`.

Voucher:
```text
REX10
```

## 14. Cách test QR nhanh

1. Login bằng tài khoản customer đã đăng ký hoặc tài khoản demo được cấu hình qua biến môi trường.
2. Vào **Lịch chiếu**.
3. Bấm **ĐẶT VÉ** ở một suất còn ghế.
4. Chọn ghế → giữ ghế → combo → thanh toán `PAY_AT_COUNTER`.
5. Mở vé điện tử, QR sẽ xuất hiện.
6. Mở trình duyệt/thiết bị khác, login bằng tài khoản `ADMIN` hoặc `CINEMA_MANAGER` đã cấu hình.
7. Vào `/admin/checkin`.
8. Dùng camera quét QR trên màn hình vé.
9. Kết quả đúng: `CHECKED_IN` + thông tin phim/rạp/phòng/ghế.
10. Quét lại: phải báo `ALREADY_CHECKED_IN`.

## 15. Cloudinary và thanh toán sandbox
Cloudinary và MoMo/VNPAY vẫn là tùy chọn. Không cần cấu hình để test luồng booking + QR bằng `PAY_AT_COUNTER`.

Khi deploy thật:
- đặt `JWT_SECRET` mạnh và riêng cho từng môi trường;
- không bật `SEED_DEMO_USERS`;
- đặt `COOKIE_SECURE=true` và dùng HTTPS;
- `COOKIE_SECURE=true`;
- cấu hình public callback/IPN cho MoMo/VNPAY.
