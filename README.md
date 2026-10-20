# RexCinemas

Ứng dụng đặt vé và quản lý chuỗi rạp phim, xây dựng bằng Java 21, Spring Boot, Thymeleaf và MySQL. Tài liệu này mô tả cách chuẩn bị môi trường, chạy ứng dụng, kiểm thử và triển khai dựa trên cấu hình hiện có trong repository.

## Yêu cầu

- JDK 21.
- Apache Maven 3.8 trở lên.
- MySQL 8.x (hoặc PostgreSQL / SQL Server nếu chọn profile tương ứng).
- SMTP Gmail để gửi OTP trong các luồng xác thực tài khoản và đặt lại mật khẩu. Cần cấu hình SMTP nếu muốn thử các luồng gửi thư.
- Trình duyệt hiện đại. Quét QR bằng camera cần `localhost` hoặc HTTPS.

## Chức năng chính

- Danh sách phim, lịch chiếu và lọc theo ngày, rạp, phim.
- Đăng ký/đăng nhập, OTP qua email, quản lý hồ sơ và đặt lại mật khẩu.
- Chọn ghế có giữ chỗ tạm thời và cập nhật trạng thái ghế realtime qua WebSocket.
- Booking, combo có kiểm soát số lượng tồn, voucher, thanh toán BANK_QR/VietQR qua SePay và vé điện tử QR check-in. Đây là phương thức thanh toán trực tuyến duy nhất được hỗ trợ trong luồng đặt vé hiện tại.
- Rex Rewards: điểm thành viên, hạng, voucher đổi điểm và sổ lịch sử biến động.
- Trang quản trị dành cho ADMIN: phim, rạp, phòng chiếu, suất chiếu, đặt vé, quét vé, người dùng, thanh toán, nội dung và Rex Rewards.

Cinema Manager đã ngừng được cấp quyền và không còn được tạo bởi seed. Chỉ tài khoản `ADMIN` được truy cập `/admin/**`. Giá trị role `CINEMA_MANAGER` còn trong enum để đọc an toàn các bản ghi cũ trong database; nó không được phép truy cập trang quản trị và không xuất hiện như lựa chọn cấp quyền mới.

## Chạy local trên Windows

### 1. Chuẩn bị MySQL

Tạo database rỗng `rex_chain_cinema` hoặc cấp cho tài khoản local quyền tạo database. Profile MySQL dùng JDBC URL mặc định:

```text
jdbc:mysql://localhost:3306/rex_chain_cinema?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
```

Ứng dụng không tự chạy SQL init. Hibernate mặc định kiểm tra schema bằng `validate`; vì vậy database phải được tạo/cập nhật trước theo các script migration phù hợp. Không đổi sang `update` trên database dùng chung để né lỗi schema.

### 2. Cấu hình biến môi trường

Spring Boot không tự đọc `.env`. `.env.example` là danh sách tham khảo, không phải file tự nạp. Có thể cấu hình biến trong Run Configuration của IDE hoặc trong một cửa sổ PowerShell riêng:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"
$env:DB_URL = "jdbc:mysql://localhost:3306/rex_chain_cinema?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "<tài-khoản-MySQL-local>"
$env:DB_PASSWORD = "<mật-khẩu-MySQL-local>"
$env:JWT_SECRET = "<chuỗi-ngẫu-nhiên-riêng-tối-thiểu-32-byte>"
$env:MAIL_USERNAME = "<địa-chỉ-Gmail>"
$env:MAIL_PASSWORD = "<Gmail-App-Password>"
```

Các biến kết nối database và `JWT_SECRET` cần có giá trị hợp lệ. Cấu hình `MAIL_USERNAME`/`MAIL_PASSWORD` khi cần thử gửi OTP hoặc đặt lại mật khẩu. Nếu cần tạo tài khoản quản trị ban đầu trên database chưa có admin, đặt thêm:

```powershell
$env:ADMIN_INITIAL_EMAIL = "admin@example.com"
$env:ADMIN_INITIAL_PASSWORD = "<mật-khẩu-mạnh>"
$env:SEED_ENABLED = "true"
```

Seeder chỉ chạy trên profile `dev` và mặc định tắt. Chỉ bật `SEED_ENABLED=true` khi chủ động khởi tạo database dev: ngoài admin, seeder có thể thêm dữ liệu phim, rạp, suất chiếu, combo, voucher và nội dung mẫu nếu các bảng tương ứng đang trống. Tắt/xóa biến này sau khi khởi tạo để tránh seeder cập nhật lại mật khẩu admin ở lần chạy sau. Nếu chỉ muốn demo tài khoản khách, đặt thêm `SEED_DEMO_USERS=true` và `DEMO_CUSTOMER_PASSWORD`. Không còn biến hay tài khoản demo Cinema Manager.

Không lưu secret thật vào source, README, `.env.example` hoặc Git. Dùng thông tin riêng cho môi trường local; không dùng lại mật khẩu production.

### 3. Khởi động

Trong thư mục chứa `pom.xml`, chạy:

```powershell
mvn spring-boot:run
```

Hoặc mở thư mục project trong IntelliJ IDEA, chọn Project SDK 21, nạp lại Maven, khai báo các biến môi trường ở trên trong Run Configuration rồi chạy `RexChainCinemaApplication`.

Trên máy Windows đã cấu hình trước bằng script DPAPI của repository, có thể dùng:

```powershell
.\start-rex-prefilled.ps1
```

Script sẽ nạp cấu hình local đã mã hóa cho tài khoản Windows hiện tại và chạy ứng dụng ở cổng 8080. Lần đầu cần nhập các thông tin được hỏi. Thêm `-Reconfigure` để nhập lại cấu hình; `-NoStart` chỉ nạp/kiểm tra cấu hình mà không chạy server.

Mở `http://localhost:8080`. Health endpoint: `http://localhost:8080/actuator/health`.

## Các profile database

Chọn đúng profile database, ví dụ `dev,mysql`, `dev,postgres` hoặc `dev,sqlserver`. Đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo server của bạn. Driver cho cả ba hệ quản trị đã có trong Maven dependencies.

- `application-dev.properties`: cookie không Secure theo mặc định để chạy HTTP local; seeder tắt; Hibernate mặc định `validate`.
- `application-mysql.properties`: kết nối MySQL; không tự chạy SQL init.
- `application-postgres.properties`: kết nối PostgreSQL; migration PostgreSQL cần chạy thủ công từ script tương ứng.
- `application-sqlserver.properties`: Flyway có thể dùng các migration tại `db/migration/sqlserver` khi được bật rõ ràng; cần xác nhận phiên bản/schema trước khi migrate.
- `application-prod.properties`: Hibernate `validate`, SQL init tắt, seeder tắt, cookie Secure bật. Production bắt buộc HTTPS và secret store.

Đặt profile production bằng `SPRING_PROFILES_ACTIVE=mysql,prod` (hoặc đúng loại database). Không bật `HIBERNATE_DDL_AUTO=update` hoặc seeder cho database production. Sao lưu và xem xét migration trước khi áp dụng. Flyway mặc định tắt; không chạy các script SQL thủ công lặp lại.

## Thanh toán SePay (tùy chọn)

SePay tắt theo mặc định. Để thử tích hợp, cần cấu hình `SEPAY_ENABLED`, thông tin VietQR/ngân hàng, `SEPAY_API_KEY` hoặc `SEPAY_WEBHOOK_SECRET`, `SEPAY_WEBHOOK_AUTH_MODE`, `SEPAY_PAYMENT_PREFIX` và `PAYMENT_BASE_URL`. Không bật với thông tin giả hoặc URL không thể truy cập từ SePay.

Webhook nhận tại `POST /api/payments/sepay/webhook`. Việc cấu hình biến môi trường chỉ bật luồng tích hợp; xác nhận thanh toán thực tế còn phụ thuộc cấu hình tài khoản SePay, ngân hàng, URL công khai và webhook hợp lệ. Có thể dùng Cloudflare Quick Tunnel trong phát triển:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Cập nhật URL HTTPS được cấp vào `PAYMENT_BASE_URL` và cấu hình webhook tương ứng ở SePay. Quick Tunnel có thể đổi URL khi chạy lại, không phù hợp production. Không tắt xác thực webhook để khắc phục lỗi 401.

## Tạo ảnh và tải nội dung

Cloudinary là tùy chọn cho upload ảnh; đặt `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` nếu sử dụng. Nếu không cấu hình, các chức năng không cần upload vẫn hoạt động. Giới hạn tải ảnh mặc định là 5 MB.

## Kiểm thử và build

Test thông thường dùng H2 trong bộ nhớ, không dùng database MySQL local:

```powershell
mvn test
mvn -DskipTests package
```

Một số lớp integration test được đặt tên rõ `*MySqlIntegrationTest` dành cho schema MySQL DEV và có thể yêu cầu database fixture sẵn có. Kiểm tra annotation/profile của test trước khi chạy riêng; không chạy các test đó trên dữ liệu production. Test JS nằm trong `src/test/js`; repository hiện không khai báo npm script làm bước build bắt buộc.

## Luồng đặt vé và check-in

```text
Chọn suất chiếu → chọn ghế và giữ chỗ → combo/voucher → BANK_QR
→ webhook thanh toán hợp lệ → booking CONFIRMED → vé QR
→ ADMIN quét vé tại /admin/checkin
```

Giữ ghế có thời hạn; booking chưa thanh toán quá hạn sẽ giải phóng ghế. QR vé dùng token ngẫu nhiên, không nhúng trực tiếp ID booking. Camera chỉ hoạt động trên `localhost` hoặc HTTPS; trang check-in hỗ trợ quét QR và nhập mã thủ công. Trạng thái chờ hoàn tiền được hệ thống ghi nhận, nhưng việc chuyển tiền hoàn cho khách là thao tác thủ công ngoài hệ thống do ADMIN đối soát và xác nhận.

## Phạm vi chưa hỗ trợ / chưa xác minh

- MoMo, VNPAY và thanh toán trực tiếp tại quầy không được hỗ trợ trong luồng đặt vé hiện tại.
- Không có phân quyền vận hành riêng theo từng rạp; các trang `/admin/**` chỉ dành cho `ADMIN`. `CINEMA_MANAGER` còn trong enum để tương thích dữ liệu cũ, không phải role được cấp mới.
- Không tuyên bố đã kiểm thử tải, triển khai production, tích hợp thanh toán thật hoặc gửi email thật chỉ từ việc build/test tự động.

Các route người dùng phổ biến: `/`, `/movies`, `/schedule`, `/profile`, `/bookings`, `/rewards`. Trang quản trị: `/admin`; mọi route `/admin/**` yêu cầu role ADMIN.

## Một số biến cấu hình

| Nhóm | Biến | Mục đích |
|---|---|---|
| Server | `SPRING_PROFILES_ACTIVE`, `PORT`, `COOKIE_SECURE` | Profile, cổng HTTP, cờ cookie Secure |
| Database | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối cơ sở dữ liệu |
| Schema | `HIBERNATE_DDL_AUTO`, `FLYWAY_ENABLED` | Kiểm tra schema và bật migration có chủ đích |
| Bảo mật | `JWT_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REMEMBER_EXPIRATION_MS` | JWT; secret tối thiểu 32 byte |
| Email | `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail SMTP/App Password để gửi OTP |
| Admin/demo | `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`, `SEED_ENABLED`, `SEED_DEMO_USERS`, `DEMO_CUSTOMER_PASSWORD` | Tạo admin ban đầu và dữ liệu demo tùy chọn |
| Ảnh | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Tải ảnh lên Cloudinary |
| Thanh toán | `PAYMENT_BASE_URL`, `SEPAY_*` | VietQR và xác thực webhook SePay |

Các giá trị mặc định và giới hạn chi tiết khác được định nghĩa trong `src/main/resources/application.properties`; cấu hình theo từng profile nằm cạnh file đó.

## Xử lý sự cố nhanh

- Ứng dụng dừng ngay khi khởi động: kiểm tra đủ năm biến bắt buộc, Java 21, kết nối database và schema tương thích với `validate`.
- Không gửi được OTP: kiểm tra Gmail App Password, quyền SMTP và log server; không dùng mật khẩu đăng nhập Gmail thông thường.
- Báo lỗi cột/bảng khi startup: kiểm tra đúng database/schema và áp dụng migration đã được review sau khi backup; không xóa database để chữa lỗi.
- Camera không mở: dùng `localhost` hoặc HTTPS và cấp quyền camera; có thể nhập mã booking thủ công.
- Webhook không đến: kiểm tra URL công khai, tunnel, chế độ API key/HMAC và cấu hình SePay; không đưa secret vào log hoặc chat.

## Cấu trúc repository

- `src/main/java/.../controller`: MVC, REST API, thanh toán, check-in.
- `src/main/java/.../service`: nghiệp vụ booking, ghế, điểm thưởng, OTP, thanh toán.
- `src/main/java/.../entity` và `repository`: mô hình JPA và truy vấn dữ liệu.
- `src/main/resources/templates`: giao diện Thymeleaf.
- `src/main/resources/static`: CSS, JavaScript, biểu tượng và ảnh tĩnh.
- `src/main/resources/db`: script khởi tạo/migration theo từng database; xem kỹ ghi chú trong từng script trước khi chạy.
- `src/test`: unit, MVC, regression và integration tests.
- `docs`: kiến trúc và tài liệu dự án bổ sung.
