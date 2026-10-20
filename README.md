# RexCinemas

RexCinemas là ứng dụng web đặt vé và quản lý chuỗi rạp phim, xây dựng với Java 21, Spring Boot, Thymeleaf và MySQL 8.

## Phạm vi hiện có

- Xem phim, lịch chiếu và lọc theo ngày, rạp, phim.
- Đăng ký, đăng nhập, OTP email, hồ sơ và đặt lại mật khẩu.
- Giữ ghế tạm thời, cập nhật trạng thái ghế qua WebSocket, đặt vé, combo và voucher.
- Thanh toán `BANK_QR`/VietQR với webhook SePay khi môi trường đã được cấu hình hợp lệ.
- Vé điện tử QR và check-in dành cho quản trị viên.
- Rex Rewards: điểm, hạng thành viên, voucher đổi điểm và lịch sử biến động.
- Quản trị phim, rạp, phòng chiếu, suất chiếu, đặt vé, nội dung, thanh toán và Rewards bằng tài khoản `ADMIN`.

`CINEMA_MANAGER` chỉ còn trong enum để đọc dữ liệu cũ an toàn; không được cấp mới và không có quyền truy cập `/admin/**`.

## Yêu cầu

- JDK 21.
- Apache Maven 3.8 trở lên.
- MySQL 8.0 trở lên.
- Trình duyệt hiện đại. Quét QR bằng camera cần `localhost` hoặc HTTPS.
- SMTP và Cloudinary chỉ cần khi kiểm thử các luồng email hoặc tải ảnh tương ứng.

## Cài đặt MySQL lần đầu

Repository chỉ hỗ trợ MySQL. Hibernate chạy với `ddl-auto=validate`; ứng dụng không tự tạo hoặc tự thay đổi schema.

1. Tạo database trống (hoặc để `schema.sql` tạo database mặc định `rex_chain_cinema`).
2. Import cấu trúc và dữ liệu mặc định tối thiểu:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u <mysql-user> -p < database\mysql\schema.sql
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u <mysql-user> -p < database\mysql\initial-data.sql
```

`schema.sql` tạo toàn bộ bảng, khóa ngoại, unique key và index. `initial-data.sql` chỉ tạo cấu hình Rex Rewards/hạng thành viên bắt buộc; không có tài khoản, mật khẩu, phim, rạp, booking, thanh toán hay dữ liệu demo.

Khi cài vào một database có tên khác, hãy sửa hai dòng `CREATE DATABASE` và `USE` ở đầu hai file hoặc import bằng công cụ MySQL vào schema đích. Với database đang sử dụng, sao lưu trước và áp dụng thay đổi theo quy trình được review; không dùng `ddl-auto=update` để né lỗi schema.

## Cấu hình môi trường

Spring Boot không tự đọc file `.env`. Dùng `.env.example` làm danh sách biến tham khảo, rồi đặt biến trong Run Configuration hoặc PowerShell. Không commit file `.env` hay secret thật.

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"
$env:DB_URL = "jdbc:mysql://localhost:3306/rex_chain_cinema?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "<mysql-user>"
$env:DB_PASSWORD = "<mysql-password>"
$env:JWT_SECRET = "<unique-random-secret-at-least-32-bytes>"
$env:MAIL_USERNAME = "<smtp-account>"
$env:MAIL_PASSWORD = "<smtp-app-password>"
```

Các biến `DB_*` và `JWT_SECRET` là bắt buộc. SMTP chỉ cần để gửi OTP/thư đặt lại mật khẩu. SePay, Cloudinary và `PAYMENT_BASE_URL` là tùy chọn theo tích hợp thực tế; để trống nếu không sử dụng.

### Tạo ADMIN an toàn trên database mới

Seeder chỉ hoạt động ở profile `dev` và mặc định tắt. Để tạo một ADMIN ban đầu trên database trống, đặt tạm các biến sau cho một lần khởi động có kiểm soát:

```powershell
$env:SEED_ENABLED = "true"
$env:ADMIN_INITIAL_EMAIL = "admin@example.com"
$env:ADMIN_INITIAL_PASSWORD = "<strong-password>"
```

Sau khi tạo xong, xóa `ADMIN_INITIAL_PASSWORD` và đặt lại `SEED_ENABLED=false`. Chỉ bật `SEED_DEMO_USERS` khi chủ động cần dữ liệu demo local.

## Khởi động và kiểm tra kết nối

```powershell
mvn spring-boot:run
```

Mở `http://localhost:8080`. Kiểm tra liveness tại `http://localhost:8080/actuator/health`. Nếu ứng dụng dừng với lỗi schema, xác nhận đã import đúng cả `database/mysql/schema.sql` và `database/mysql/initial-data.sql`, đồng thời kiểm tra JDBC URL, user và password.

Production dùng `SPRING_PROFILES_ACTIVE=mysql,prod`; profile này bật Thymeleaf cache, buộc `ddl-auto=validate`, tắt seeder và bật cookie Secure. Dùng HTTPS và secret store của môi trường triển khai.

## Build, test và sao lưu

```powershell
mvn test
mvn -DskipTests package
```

Các test thông thường dùng H2 và không thay thế kiểm thử với MySQL thật, dịch vụ email, SePay, camera, Cloudinary hoặc tải thực tế.

Backup/restore MySQL an toàn được mô tả tại [docs/OPERATIONS_MYSQL_BACKUP_RESTORE.md](docs/OPERATIONS_MYSQL_BACKUP_RESTORE.md). Script restore chỉ cho phép database test có tiền tố `REX_TEST_`.

## Cấu trúc repository

- `database/mysql/schema.sql`: schema MySQL 8 dùng cho cài mới.
- `database/mysql/initial-data.sql`: cấu hình hệ thống tối thiểu, không chứa dữ liệu kinh doanh.
- `src/main/java`: controller, service, entity, repository và cấu hình Spring Boot.
- `src/main/resources/templates`, `static`: giao diện Thymeleaf, CSS, JavaScript và tài nguyên ảnh.
- `src/test`: unit, MVC, regression và integration test.
- `scripts`: backup/restore MySQL local an toàn.

## Lưu ý tích hợp

- Luồng checkout hiện hỗ trợ `BANK_QR`; không mô tả MoMo, VNPAY hay thanh toán tại quầy như chức năng đang hoạt động.
- SePay chỉ xử lý được khi webhook public, secret và tài khoản ngân hàng được cấu hình đúng; không tắt xác thực webhook để xử lý lỗi.
- Trạng thái hoàn tiền là luồng đối soát. Việc chuyển tiền hoàn cho khách là thao tác ngoài hệ thống, do ADMIN xác nhận sau đối soát.
