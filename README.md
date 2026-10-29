# RexCinemas

> Nền tảng đặt vé trực tuyến và quản lý chuỗi rạp phim.

RexCinemas là ứng dụng web xây dựng bằng Java 21, Spring Boot, Thymeleaf và MySQL 8. Hệ thống hỗ trợ khách hàng xem phim, chọn ghế, đặt vé, thanh toán QR và quản trị nội dung vận hành rạp trong một nền tảng thống nhất.

> **Trạng thái:** phiên bản đồ án/demo có thể chạy local. Các tích hợp bên ngoài như SMTP, Cloudinary và SePay phụ thuộc cấu hình môi trường thực tế.

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng chính](#tính-năng-chính)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Chạy dự án](#chạy-dự-án)
- [Cấu hình biến môi trường](#cấu-hình-biến-môi-trường)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Đóng góp](#đóng-góp)
- [License](#license)
- [Roadmap](#roadmap)

## Giới thiệu

RexCinemas mô phỏng quy trình đặt vé của một chuỗi rạp: người dùng xem danh mục phim và lịch chiếu, chọn ghế đang còn trống, giữ ghế tạm thời, áp dụng ưu đãi, thanh toán qua QR và nhận vé điện tử để check-in.

Phần quản trị cung cấp các màn hình quản lý phim, rạp, phòng chiếu, ghế, suất chiếu, booking, giao dịch, nội dung và Rex Rewards. Toàn bộ dữ liệu ứng dụng dùng **MySQL 8**; Hibernate chỉ xác thực schema bằng ddl-auto=validate và không tự thay đổi cấu trúc database.

### Phạm vi hiện tại

- CUSTOMER sử dụng các luồng khách hàng thông thường.
- STAFF chỉ được vào khu vực soát vé; SUPERVISOR được vào dashboard, lịch chiếu, booking, tài khoản và hỗ trợ của rạp được giao.
- ADMIN quản trị toàn hệ thống. CINEMA_MANAGER chỉ còn tồn tại để tương thích dữ liệu cũ, không được cấp mới và không có quyền truy cập trang quản trị.
- Mọi endpoint quản trị ngoài các ngoại lệ có phân quyền tường minh chỉ cho phép ADMIN.
- Checkout hiện hỗ trợ phương thức BANK_QR; MoMo, VNPAY và thanh toán trực tiếp tại quầy chưa nằm trong phạm vi hỗ trợ.

## Tính năng chính

### Dành cho khách hàng

- Xem phim, chi tiết phim, lịch chiếu và lọc theo ngày, rạp, phim.
- Đăng ký, đăng nhập, xác thực OTP qua email và đặt lại mật khẩu.
- Chọn ghế với cơ chế giữ ghế tạm thời, chống tranh chấp và cập nhật realtime qua WebSocket.
- Đặt vé, chọn combo, áp dụng voucher và theo dõi lịch sử booking.
- Thanh toán BANK_QR/VietQR; SePay webhook được xác thực trước khi ghi nhận giao dịch.
- Nhận vé điện tử có QR/token và sử dụng cho quy trình check-in.
- Rex Rewards: điểm thưởng, hạng thành viên, voucher đổi điểm và lịch sử biến động điểm.

### Dành cho quản trị viên

- Quản lý phim, rạp, phòng chiếu, ghế và suất chiếu.
- Quản lý booking, check-in vé QR và đối soát giao dịch thanh toán.
- Quản lý người dùng, sự kiện, bài viết, media và nội dung hiển thị.
- Cấu hình Rex Rewards, chính sách hạng, voucher và ledger điểm.
- Dashboard và các bộ lọc/phân trang cho dữ liệu vận hành.

## Kiến trúc tổng quan

~~~mermaid
flowchart LR
    B[Trình duyệt] --> MVC[Spring MVC / Thymeleaf]
    B --> API[REST API]
    B <--> WS[WebSocket STOMP/SockJS]
    MVC --> C[Controller]
    API --> C
    C --> S[Service]
    S --> R[JPA Repository]
    S --> N[Native SQL Repository]
    R --> DB[(MySQL 8)]
    N --> DB
    S --> EXT[SMTP / Cloudinary / SePay]
    WS --> S
~~~

### Luồng đặt vé

~~~mermaid
sequenceDiagram
    participant K as Khách hàng
    participant A as Ứng dụng
    participant DB as MySQL
    participant P as SePay
    participant Q as QR check-in

    K->>A: Chọn suất chiếu và ghế
    A->>DB: Khóa/giữ ghế tạm thời
    K->>A: Xác nhận booking và chọn BANK_QR
    A->>DB: Tạo booking chờ thanh toán
    P-->>A: Webhook giao dịch đã xác thực
    A->>DB: Cập nhật booking CONFIRMED và tạo vé QR
    K->>Q: Xuất trình mã vé
    Q->>DB: Kiểm tra token và trạng thái check-in
~~~

### Nguyên tắc dữ liệu

- database/mysql/schema.sql là schema hoàn chỉnh cho cài đặt mới.
- database/mysql/upgrade-staff-supervisor.sql nâng cấp dữ liệu MySQL đang có, bổ sung quyền nhân viên và trạng thái phòng. Sao lưu trước khi chạy.
- database/mysql/initial-data.sql chỉ chứa cấu hình Rewards và chính sách hạng tối thiểu.
- Không đưa tài khoản, mật khẩu, booking, giao dịch hoặc dữ liệu demo vào schema phát hành.
- Không chạy lại schema cài mới trên database đã có dữ liệu kinh doanh nếu chưa sao lưu và review.

## Yêu cầu hệ thống

- JDK 21.
- Apache Maven 3.8 trở lên.
- MySQL 8.0 trở lên.
- Trình duyệt hiện đại.
- localhost hoặc HTTPS nếu sử dụng camera để quét QR.
- SMTP Gmail/App Password nếu cần kiểm thử OTP hoặc đặt lại mật khẩu.
- Cloudinary nếu cần tải ảnh từ trang quản trị.

## Cài đặt

### 1. Lấy mã nguồn

~~~powershell
git clone https://github.com/qkhanh06/RexCinemas.git
cd RexCinemas
~~~

### 2. Chuẩn bị MySQL

Tạo user MySQL có quyền trên database cài đặt. Có thể để schema.sql tạo database mặc định rex_chain_cinema, hoặc tạo database trước bằng MySQL Workbench/CLI.

Import schema và dữ liệu hệ thống tối thiểu:

~~~powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u <mysql-user> -p < database\mysql\schema.sql
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u <mysql-user> -p < database\mysql\initial-data.sql
~~~

Schema hiện tạo 36 bảng, bao gồm dữ liệu người dùng, phim/rạp, booking, ghế, Rewards, OTP, thanh toán, chat và audit. Nếu dùng tên database khác, thay tên ở các dòng CREATE DATABASE và USE trước khi import.

**Database đã có dữ liệu:** Không import lại `schema.sql`. Sao lưu database, rồi chạy `database/mysql/upgrade-staff-supervisor.sql` bằng MySQL CLI trước khi khởi động phiên bản này (`ddl-auto=validate`). Script hỗ trợ chạy lại mà không tạo trùng cột hoặc khóa ngoại.

### 3. Kiểm tra cấu hình

Sao chép .env.example để tham khảo biến cần thiết. Spring Boot không tự đọc file .env; hãy khai báo biến trong PowerShell, IDE Run Configuration hoặc secret store của môi trường triển khai.

## Chạy dự án

### Chạy local

Khai báo tối thiểu các biến kết nối MySQL, JWT và SMTP rồi chạy:

~~~powershell
mvn spring-boot:run
~~~

Ứng dụng mặc định chạy tại:

- Website: http://localhost:8080
- Health check: http://localhost:8080/actuator/health

Nếu startup báo thiếu bảng/cột, hãy kiểm tra database URL, user, password và xác nhận schema đã được import đúng. Không đổi HIBERNATE_DDL_AUTO=update trên database dùng chung để bỏ qua lỗi.

### Build và test

~~~powershell
mvn test
mvn -DskipTests package
~~~

Test thông thường dùng H2 trong bộ nhớ. Kết quả test tự động không thay thế kiểm thử MySQL thật, trình duyệt, SMTP, Cloudinary, SePay hoặc tải hệ thống.

### Production

Dùng profile:

~~~text
SPRING_PROFILES_ACTIVE=mysql,prod
~~~

Profile production bật Thymeleaf cache, buộc Hibernate validate, tắt seeder và yêu cầu cookie Secure. Production cần HTTPS, secret store, backup database và quy trình review schema riêng.

## Cấu hình biến môi trường

Ví dụ PowerShell local:

~~~powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"
$env:DB_URL = "jdbc:mysql://localhost:3306/rex_chain_cinema?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "<mysql-user>"
$env:DB_PASSWORD = "<mysql-password>"
$env:JWT_SECRET = "<random-secret-at-least-32-bytes>"
$env:MAIL_USERNAME = "<smtp-account>"
$env:MAIL_PASSWORD = "<smtp-app-password>"
~~~

| Nhóm | Biến | Bắt buộc | Mục đích |
|---|---|:---:|---|
| Server | SPRING_PROFILES_ACTIVE, PORT, COOKIE_SECURE | Có | Profile, cổng HTTP và cookie Secure |
| MySQL | DB_URL, DB_USERNAME, DB_PASSWORD | Có | Kết nối database |
| Bảo mật | JWT_SECRET, JWT_EXPIRATION_MS | Có | Phát hành và thời hạn JWT; secret tối thiểu 32 byte |
| Email | MAIL_USERNAME, MAIL_PASSWORD | Theo luồng email | Gửi OTP và email đặt lại mật khẩu |
| Seeder | SEED_ENABLED, ADMIN_INITIAL_EMAIL, ADMIN_INITIAL_PASSWORD | Không | Tạo ADMIN ban đầu có kiểm soát ở profile dev |
| Ảnh | CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET | Không | Upload ảnh từ trang quản trị |
| Thanh toán | PAYMENT_BASE_URL, SEPAY_* | Theo tích hợp | VietQR và xác thực webhook SePay |

### Tạo ADMIN ban đầu

Seeder mặc định tắt và chỉ hoạt động trong profile dev. Trên database trống, có thể bật tạm:

~~~powershell
$env:SEED_ENABLED = "true"
$env:ADMIN_INITIAL_EMAIL = "admin@example.com"
$env:ADMIN_INITIAL_PASSWORD = "<strong-password>"
~~~

Sau lần chạy thành công, xóa mật khẩu khỏi session terminal và đặt SEED_ENABLED=false. Không commit secret thật, .env hoặc backup chứa dữ liệu khách hàng.

## Cấu trúc thư mục

~~~text
RexCinemas/
├── database/
│   └── mysql/
│       ├── schema.sql
│       ├── initial-data.sql
│       └── upgrade-staff-supervisor.sql
├── src/
│   ├── main/
│   │   ├── java/com/rexchain/cinema/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       └── application*.properties
│   └── test/
├── scripts/
│   ├── backup-mysql.ps1
│   └── restore-mysql-test.ps1
├── docs/
├── pom.xml
├── README.md
└── .env.example
~~~

| Thư mục/file | Vai trò |
|---|---|
| src/main/java | Backend Spring Boot, nghiệp vụ, bảo mật và truy vấn |
| src/main/resources/templates | Giao diện Thymeleaf |
| src/main/resources/static | CSS, JavaScript, icon và ảnh tĩnh |
| database/mysql | Schema và dữ liệu hệ thống cho cài mới |
| scripts | Công cụ backup/restore MySQL local |
| src/test | Unit, MVC, regression và integration test |

## Đóng góp

1. Tạo branch từ main với tên mô tả, ví dụ fix/booking-timeout hoặc docs/update-readme.
2. Chỉ thay đổi phần cần thiết cho issue/feature; không commit secret, log, build artifact hoặc backup dữ liệu.
3. Nếu thay đổi schema, cập nhật database/mysql/schema.sql, kiểm tra ddl-auto=validate và mô tả ảnh hưởng trong pull request.
4. Chạy mvn test và mvn -DskipTests package trước khi gửi.
5. Kiểm tra git diff --check và xác nhận không còn tham chiếu tới file đã xóa.
6. Tạo pull request với mô tả mục tiêu, phạm vi, cách kiểm thử và các phần chưa xác minh.

Các thay đổi phá vỡ dữ liệu, booking, payment hoặc API cần được review trước khi merge. Không force-push lên main.

## License

Repository hiện **chưa kèm file LICENSE chính thức**. Mã nguồn chỉ nên được sử dụng, sao chép hoặc phân phối theo sự cho phép của chủ sở hữu repository. Nếu dự án được phát hành công khai, hãy bổ sung file license cụ thể, ví dụ MIT, Apache-2.0 hoặc license nội bộ đã được phê duyệt.

## Roadmap

Roadmap dưới đây là định hướng phát triển, không phải tính năng đã hoàn thành:

- [ ] Bổ sung quy trình migration có version cho nâng cấp database đang có dữ liệu.
- [ ] Bổ sung integration test MySQL tự động trong CI với database cô lập.
- [ ] Bổ sung E2E test cho đăng nhập, chọn ghế, checkout và check-in QR.
- [ ] Cải thiện quan sát hệ thống: metric, tracing và cảnh báo lỗi theo môi trường.
- [ ] Tối ưu thêm các truy vấn dashboard, lịch sử booking và giao dịch khi dữ liệu lớn.
- [ ] Xem xét phân quyền quản lý theo từng cụm rạp nếu nghiệp vụ cần.
- [ ] Hoàn thiện quy trình sandbox payment và kiểm thử webhook an toàn.

Các mục trong roadmap cần được đánh giá lại theo yêu cầu nghiệp vụ và bằng chứng kiểm thử trước khi xem là đã hỗ trợ.

## Tài liệu liên quan

- Kiến trúc hệ thống: docs/ARCHITECTURE.md
- Backup và restore MySQL: docs/OPERATIONS_MYSQL_BACKUP_RESTORE.md
