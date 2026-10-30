# RexCinemas

Nền tảng đặt vé trực tuyến và quản lý vận hành chuỗi rạp phim, xây dựng bằng Java 21, Spring Boot, Thymeleaf và MySQL 8.

RexCinemas đưa hành trình xem phim, chọn ghế, đặt vé và nhận vé điện tử vào cùng một ứng dụng với các công cụ quản trị phim, rạp, lịch chiếu, giao dịch và chương trình thành viên Rex Rewards.

> **Tình trạng dự án:** Ứng dụng có thể chạy trong môi trường local. Thanh toán SePay, email SMTP, Cloudinary và trợ lý Grok cần cấu hình thông tin tích hợp riêng. Các tích hợp này không thực hiện giao dịch hay gửi nội dung ra dịch vụ bên ngoài nếu chưa được cấu hình và sử dụng.

## Giới thiệu

Khách hàng có thể tìm phim và lịch chiếu, giữ ghế trong thời gian checkout, chọn combo hoặc voucher, thanh toán qua BANK_QR và sử dụng vé điện tử để check-in. WebSocket/STOMP cập nhật trạng thái ghế để giảm nguy cơ hai khách cùng đặt một chỗ.

Nhân viên được phân quyền theo vai trò. STAFF tập trung vào check-in; SUPERVISOR quản lý hoạt động trong rạp được giao; ADMIN quản trị toàn hệ thống. Giao diện máy chủ được render bằng Thymeleaf, đồng thời ứng dụng cung cấp REST API và WebSocket cho các tương tác động.

## Tính năng chính

- **Khám phá phim:** danh mục, trang chi tiết, nội dung, rạp và lịch chiếu.
- **Tài khoản:** đăng ký, đăng nhập, OTP email và đặt lại mật khẩu.
- **Đặt vé:** chọn ghế, giữ chỗ tạm thời, chọn combo, voucher và theo dõi booking.
- **Thanh toán:** luồng BANK_QR/VietQR cùng xử lý webhook SePay có xác thực.
- **Vé điện tử:** mã QR/token và quy trình check-in dành cho nhân viên.
- **Rex Rewards:** tích điểm, hạng thành viên, đổi thưởng và lịch sử điểm.
- **Quản trị:** phim, rạp, phòng, ghế, suất chiếu, booking, giao dịch, nội dung, người dùng và hỗ trợ khách hàng.
- **Cập nhật trực tiếp:** WebSocket/STOMP cho trạng thái ghế và trao đổi hỗ trợ.
- **Bảo vệ dữ liệu:** phân quyền theo vai trò, giới hạn đăng nhập, xác thực đầu vào và Hibernate validate để không tự ý thay đổi schema.

## Kiến trúc tổng quan

~~~mermaid
flowchart LR
    Browser[Trình duyệt] -->|HTTP / Thymeleaf| MVC[Spring MVC]
    Browser -->|REST| API[REST API]
    Browser <-->|WebSocket / STOMP| WS[WebSocket]
    MVC --> Controller[Controllers]
    API --> Controller
    Controller --> Service[Services]
    WS <--> Service
    Service --> JPA[JPA Repositories]
    Service --> SQL[Native SQL Repositories]
    JPA --> DB[(MySQL 8)]
    SQL --> DB
    Service -. tùy cấu hình .-> Integrations[SMTP / Cloudinary / SePay / Grok]
~~~

Các lớp Java chính nằm dưới **com.rexchain.cinema**: controller tiếp nhận request, service xử lý nghiệp vụ, repository truy cập dữ liệu và entity ánh xạ bảng. Một số truy vấn báo cáo hoặc tối ưu sử dụng native SQL. Thymeleaf, JavaScript và CSS cung cấp giao diện web.

### Luồng đặt vé rút gọn

~~~mermaid
sequenceDiagram
    actor Customer as Khách hàng
    participant App as RexCinemas
    participant DB as MySQL
    participant Pay as SePay (nếu bật)
    Customer->>App: Chọn suất chiếu và ghế
    App->>DB: Giữ ghế và tạo booking chờ thanh toán
    Customer->>App: Chọn BANK_QR và thanh toán
    Pay-->>App: Gửi webhook giao dịch
    App->>App: Xác thực webhook và đối chiếu giao dịch
    App->>DB: Xác nhận booking, phát hành vé
    Customer->>App: Mở vé QR để check-in
~~~

SePay là tích hợp tùy chọn; khi chưa cấu hình, không có giao dịch thật. Chỉ bật tích hợp sau khi đã thiết lập thông tin sandbox hoặc production phù hợp.

## Cài đặt

### Yêu cầu

- JDK 21.
- Apache Maven 3.8 trở lên.
- MySQL 8.0 trở lên.
- Git và trình duyệt hiện đại.
- Tài khoản SMTP nếu cần thử OTP/email; Cloudinary nếu cần tải ảnh lên; thông tin SePay nếu cần thử thanh toán.

### Lấy mã nguồn

~~~bash
git clone https://github.com/QuytChin/RexCinemas.git
cd RexCinemas
~~~

### Tạo database

**Cài mới kèm dữ liệu demo:** chạy file độc lập **database/mysql/fresh-install/full_install.sql**. Script tạo database **rex_chain_cinema** nếu chưa có, sau đó tạo schema và dữ liệu mẫu. Chỉ chạy trên database trống; không chạy đè lên database đang có dữ liệu. Tài khoản demo và hướng dẫn chi tiết có trong [README của bộ fresh-install](database/mysql/fresh-install/README.md).

~~~bash
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < database/mysql/fresh-install/full_install.sql
~~~

**Tự cài schema tối thiểu:** dùng **database/mysql/schema.sql**, sau đó **database/mysql/initial-data.sql**. Cách này không nạp bộ dữ liệu demo đầy đủ.

~~~bash
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < database/mysql/schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 < database/mysql/initial-data.sql
~~~

Hibernate mặc định dùng **ddl-auto=validate**; schema phải được import trước khi khởi động ứng dụng. Không đổi thành **update** để bỏ qua lỗi schema. Với database đang có dữ liệu, sao lưu và review hướng dẫn migration trước khi áp dụng bất kỳ thay đổi SQL nào.

## Chạy dự án

Spring Boot không tự nạp file **.env**. Hãy đặt biến trong terminal, IDE Run Configuration hoặc secret store của môi trường triển khai. Có thể dùng [.env.example](.env.example) làm danh sách tham khảo, nhưng không commit file .env chứa secret.

Ví dụ PowerShell cho local development:

~~~powershell
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/rex_chain_cinema?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:DB_USERNAME = "<mysql-user>"
$env:DB_PASSWORD = "<mysql-password>"
$env:JWT_SECRET = "<unique-random-secret-at-least-32-bytes>"

mvn spring-boot:run
~~~

Sau khi ứng dụng khởi động:

- Website: <http://localhost:8080>
- Health check: <http://localhost:8080/actuator/health>

Build và chạy test:

~~~bash
mvn test
mvn -DskipTests package
~~~

## Cấu hình môi trường

Các tên biến bên dưới tương ứng với cấu hình trong **src/main/resources/application*.properties**.

| Biến | Bắt buộc | Mục đích |
|---|:---:|---|
| SPRING_PROFILES_ACTIVE | Có | Profile, ví dụ dev,mysql; môi trường production cần dùng profile phù hợp. |
| PORT | Không | Cổng HTTP, mặc định 8080. |
| DB_URL | Có | JDBC URL tới MySQL. |
| DB_USERNAME, DB_PASSWORD | Có | Tài khoản MySQL của ứng dụng. |
| JWT_SECRET | Có | Secret ký JWT, tối thiểu 32 byte và phải riêng cho từng môi trường. |
| HIBERNATE_DDL_AUTO | Không | Mặc định validate; giữ nguyên cho schema dùng chung. |
| COOKIE_SECURE | Theo môi trường | Đặt true khi chạy sau HTTPS; local HTTP có thể đặt false. |
| MAIL_USERNAME, MAIL_PASSWORD | Theo tính năng | SMTP để gửi OTP và email. Với Gmail, dùng App Password. |
| CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET | Không | Tải ảnh lên Cloudinary. |
| SEPAY_ENABLED và nhóm SEPAY_* | Không | Tích hợp BANK_QR/webhook; để tắt nếu chưa cấu hình. |
| PAYMENT_BASE_URL | Không | URL công khai dùng để dựng trang thanh toán và callback. |
| SEED_ENABLED, ADMIN_INITIAL_EMAIL, ADMIN_INITIAL_PASSWORD | Không | Tạo admin ban đầu theo cơ chế bootstrap trong profile dev. Mặc định seeder tắt. |
| GROK_ENABLED, GROK_API_KEY | Không | Bật/tắt và cấu hình trợ lý tư vấn phim. Tắt GROK_ENABLED nếu không sử dụng. |
| TICKET_EMAIL_ENABLED, PUBLIC_BASE_URL | Không | Gửi vé qua email và tạo URL công khai trong email. |

Các biến môi trường triển khai thực tế phải được quản lý bằng secret store. Không đưa mật khẩu, JWT secret, khóa webhook hay thông tin ngân hàng thật vào Git.

## Cấu trúc thư mục

~~~text
RexCinemas/
├── database/mysql/          # Schema, dữ liệu khởi tạo và script migration
│   └── fresh-install/       # Bộ cài MySQL độc lập kèm dữ liệu demo
├── docs/                    # Kiến trúc, vận hành và tài liệu dự án
├── scripts/                 # Công cụ backup/restore và tiện ích local
├── src/
│   ├── main/java/com/rexchain/cinema/
│   │   ├── config/          # Cấu hình Spring và bảo mật
│   │   ├── controller/      # MVC, REST và WebSocket endpoints
│   │   ├── entity/          # Ánh xạ dữ liệu
│   │   ├── repository/      # JPA và native SQL
│   │   └── service/         # Nghiệp vụ
│   ├── main/resources/
│   │   ├── static/          # CSS, JavaScript, ảnh và tài nguyên web
│   │   └── templates/       # Thymeleaf templates
│   └── test/                # Unit, MVC và integration tests
├── tests/                   # Kiểm thử trình duyệt Playwright
├── pom.xml
└── README.md
~~~

## Đóng góp

1. Mở issue để mô tả lỗi hoặc đề xuất tính năng; trao đổi phạm vi trước với thay đổi lớn.
2. Tạo branch từ main, ví dụ fix/booking-timeout hoặc docs/update-readme.
3. Giữ thay đổi tập trung, cập nhật tài liệu khi thay đổi hành vi hoặc cấu hình.
4. Với thay đổi database, cập nhật schema/migration liên quan; kiểm tra tương thích với ddl-auto=validate và nêu rõ ảnh hưởng dữ liệu.
5. Chạy mvn test, các kiểm thử phù hợp và git diff --check trước khi mở pull request.
6. Pull request nên nêu mục tiêu, cách kiểm thử, ảnh hưởng cấu hình/database và các phần chưa kiểm chứng.
7. Không commit secret, file .env, log, backup dữ liệu, build output hoặc ảnh chụp/phụ phẩm kiểm thử lớn. Không force-push lên main.

## License

Repository hiện chưa có file LICENSE. Vì vậy, dự án chưa công bố điều khoản cấp phép sử dụng, sửa đổi hoặc phân phối. Hãy xin phép chủ sở hữu repository trước khi tái sử dụng hoặc phân phối mã nguồn.

## Roadmap

Các mục dưới đây là định hướng đề xuất; tình trạng có thể thay đổi theo ưu tiên dự án.

- [ ] Hoàn thiện migration có version cho nâng cấp database đang có dữ liệu.
- [ ] Chạy integration test MySQL trên database cô lập trong CI.
- [ ] Mở rộng kiểm thử E2E cho đăng nhập, giữ ghế, checkout và check-in.
- [ ] Bổ sung metrics, tracing và cảnh báo theo môi trường.
- [ ] Đánh giá hiệu năng truy vấn lịch chiếu, dashboard và lịch sử giao dịch trên dữ liệu lớn.
- [ ] Hoàn thiện kiểm thử sandbox và quy trình vận hành webhook thanh toán.

## Tài liệu liên quan

- [Kiến trúc hệ thống](docs/ARCHITECTURE.md)
- [Quy tắc khách hàng](docs/CUSTOMER_POLICIES.md)
- [Vận hành backup và restore MySQL](docs/OPERATIONS_MYSQL_BACKUP_RESTORE.md)
- [Kịch bản chức năng khách hàng](docs/DEMO_KICH_BAN_CHUC_NANG_KHACH_HANG.md)
