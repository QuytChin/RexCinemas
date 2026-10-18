# BÁO CÁO TỔNG KẾT ĐỒ ÁN MÔN HỌC
## HỆ THỐNG QUẢN LÝ CỤM RẠP CHIẾU PHIM VÀ ĐẶT VÉ TRỰC TUYẾN (REX CHAIN CINEMA)

* **Môn học áp dụng**: Lập trình Web (LTW) & Công nghệ Phần mềm (CNPM)
* **Đối tượng đề tài**: Xây dựng hệ thống Web Full-stack đặt vé xem phim, bắp nước (F&B), cổng soát vé QR và quản lý rạp chiếu phim theo tiêu chuẩn thực tế.
* **Nền tảng công nghệ**: Java 21 LTS, Spring Boot 3.3.5, Spring Security 6, Spring Data JPA, Thymeleaf, WebSocket STOMP, MoMo, VNPAY, SePay VietQR, Microsoft SQL Server / MySQL.

---

# PHẦN 1: CÁC CÔNG NGHỆ ÁP DỤNG TRONG MÔN LẬP TRÌNH WEB (LTW)

Hệ thống được thiết kế theo mô hình **Model - View - Controller (MVC)** kết hợp kiến trúc **RESTful API**, tối ưu hóa cho cả người dùng Web máy tính, điện thoại và thiết bị di động.

### 1. Công nghệ Frontend (Giao diện & Tương tác)
* **HTML5 & Semantic Elements**: 
  - Sử dụng cấu trúc HTML ngữ nghĩa (`<main>`, `<article>`, `<section>`, `<aside>`, `<header>`, `<footer>`) tối ưu cho trải nghiệm người dùng và chuẩn SEO.
  - Sử dụng Form truyền tải bảo mật `enctype="multipart/form-data"` để upload poster phim, ảnh combo bắp nước lên Cloud.
* **CSS3 & Responsive Web Design**:
  - Tích hợp **Bootstrap 5.3.3** kết hợp hệ thống CSS tuỳ biến (**Custom Dark Modern Cinema Palette**).
  - Sử dụng **CSS Grid** và **Flexbox** để dựng sơ đồ phòng chiếu linh hoạt, tự động co giãn theo số hàng, số cột của từng phòng chiếu (`auditorium`).
  - **CSS `@media print` chuyên biệt**: Tự động nhận diện khi người dùng bấm nút *In vé / Lưu PDF* (`Ctrl + P`), ẩn các thành phần thừa (Header, Footer, Button) và dàn trang in vé theo đúng khổ vé rạp phim sắc nét.
* **JavaScript (ES6+) & Xử lý bất đồng bộ (Async/Await)**:
  - Sử dụng `fetch API` bất đồng bộ để gọi các API giữ ghế (`/api/showtimes/{id}/hold`), nhả ghế, kiểm tra mã voucher và thanh toán không cần reload trang.
  - Đồng hồ đếm ngược thời gian giữ chỗ (Countdown Timer 5 phút) bằng `setInterval`.
  - Tích hợp **Modal Popup Trailer**: Nhúng iframe YouTube phát trailer phim ngay trên trang chi tiết phim thông qua JavaScript DOM manipulation.
* **Thymeleaf Template Engine**:
  - Đóng gói giao diện thành các thành phần dùng chung (Fragments): `header.html`, `footer.html`, `admin-nav.html`, `movie-card.html`, `showtime-selection.html`.
  - Kết hợp cú pháp Thymeleaf logic: `th:if`, `th:each`, `th:switch`, `th:selected`, `#temporals.format` định dạng ngày giờ, `#numbers.formatDecimal` định dạng tiền tệ Việt Nam Đồng.

### 2. Công nghệ Realtime & Đa phương tiện (Realtime & Multimedia)
* **WebSocket & STOMP Protocol**:
  - Tích hợp `spring-boot-starter-websocket` với thư viện Client `StompJs` và `SockJS`.
  - Kênh truyền tin `/topic/showtimes/{id}/seats`: Khi Khách hàng A bấm chọn/giữ ghế, hệ thống lập tức broadcast trạng thái `HELD` tới màn hình của tất cả khách hàng khác đang xem cùng suất chiếu đó trong thời gian thực (dưới 100ms) mà không cần F5 trình duyệt.
* **Tích hợp Camera Web Quét mã QR (`html5-qrcode`)**:
  - Tại trang nhân viên soát vé (`/admin/checkin`), hệ thống sử dụng camera của điện thoại hoặc webcam máy tính để quét mã QR vé điện tử trực tiếp trên trình duyệt.
* **ZXing Core (Zebra Crossing)**:
  - Thư viện backend chuyên dụng để sinh ảnh QR Code (PNG) từ mã token bảo mật 64 hex ký tự cho từng vé.
* **Cloudinary Cloud Storage**:
  - Tích hợp Cloudinary SDK để lưu trữ và phân phối ảnh poster phim, ảnh sản phẩm bắp nước lên CDN đám mây với tốc độ cao.

### 3. Bảo mật Web (Web Security)
* **Spring Security 6 & JSON Web Token (JWT)**:
  - Xác thực người dùng không trạng thái (Stateless Authentication) với `jjwt 0.12.6`.
  - **Lưu trữ an toàn**: JWT token được lưu trong `HttpOnly Cookie`, gắn cờ `SameSite=Lax` giúp chống hoàn toàn các cuộc tấn công đánh cắp token qua mã độc Javascript (**XSS**).
  - Hỗ trợ đồng thời cả Header `Authorization: Bearer <token>` phục vụ cho các REST API client.
* **Bảo vệ chống tấn công CSRF (Cross-Site Request Forgery)**:
  - Bật bảo vệ CSRF Token cho toàn bộ các Form MVC Thymeleaf thông qua `CsrfCookieFilter`.
  - Cấu hình miễn trừ hợp lệ cho các stateless REST API, WebSocket handshake và IPN Payment Webhook.
* **Phân quyền người dùng (Role-Based Access Control - RBAC)**:
  - Phân chia chặt chẽ 3 cấp quyền:
    + `ROLE_CUSTOMER`: Khách hàng (Đặt vé, xem lịch sử đặt chỗ, đánh giá phim).
    + `ROLE_CINEMA_MANAGER`: Quản lý rạp (Soát vé tại cổng check-in, quản lý phòng chiếu).
    + `ROLE_ADMIN`: Quản trị viên tối cao (Quản lý phim, suất chiếu, combo, duyệt hoàn tiền, tài khoản).
* **Mã hóa mật khẩu**:
  - Sử dụng thuật toán một chiều `BCryptPasswordEncoder` băm mật khẩu có muối (salt) bảo mật tuyệt đối trong database.

### 4. Tích hợp Cổng thanh toán Trực tuyến (Payment Gateways)
* **Cổng MoMo V2**: Tạo yêu cầu thanh toán QRCode/App, ký số bảo mật bằng thuật toán **HMAC-SHA256**.
* **Cổng VNPAY Sandbox**: Tạo URL thanh toán ngân hàng/VNPAY-QR, ký số bảo mật bằng thuật toán **HMAC-SHA512**.
* **Cổng SePay VietQR (Tự động xác nhận chuyển khoản)**: Nhận webhook từ ngân hàng, đối soát mã đơn hàng tự động và chuyển trạng thái vé sang `CONFIRMED` tức thì.

---

# PHẦN 2: CÁC CÔNG NGHỆ ÁP DỤNG TRONG MÔN CÔNG NGHỆ PHẦN MỀM (CNPM)

Đồ án áp dụng các nguyên lý, mô hình kiến trúc và kỹ thuật kiểm thử theo chuẩn công nghiệp phần mềm:

### 1. Kiến trúc phần mềm & Design Patterns
* **Kiến trúc phân tầng (N-Tier Layered Architecture)**:
  - Tách bạch rõ ràng 4 tầng: `Presentation Layer` (Controller) $\rightarrow$ `Business Logic Layer` (Service) $\rightarrow$ `Data Access Layer` (Repository) $\rightarrow$ `Database`.
  - Triệt tiêu hiện tượng **God Class**: Phân rã `BookingService` trước đây thành các Service chuyên trách theo nguyên lý Single Responsibility Principle (SRP):
    + `SeatReservationService`: Quản lý giữ ghế, nhả ghế và thuật toán Orphan Seat.
    + `PaymentService`: Xử lý thanh toán MoMo, VNPAY, SePay và hoàn tiền.
    + `CheckInService`: Soát vé đơn lẻ, vé đoàn tại cổng.
    + `SeatBroadcastService`: Phát sóng WebSocket realtime.
* **Các mẫu thiết kế (Design Patterns) áp dụng**:
  - **Dependency Injection / Inversion of Control (IoC)**: Quản lý vòng đời các Bean bởi Spring Container.
  - **Repository Pattern**: Che giấu chi tiết thao tác cơ sở dữ liệu qua Spring Data JPA.
  - **Data Transfer Object (DTO) Pattern**: Sử dụng Java `record` (`HoldRequest`, `LoginRequest`, `RegisterRequest`, `CheckInRequest`, `CheckoutRequest`) để chuẩn hóa dữ liệu vào/ra.
  - **Observer Pattern**: Triển khai qua WebSocket Message Broker để thông báo thay đổi trạng thái ghế cho các client.
  - **Orchestrator Pattern**: `BookingService` đóng vai trò điều phối giữa giữ ghế, thanh toán, trừ kho bắp nước và tích điểm thành viên.

### 2. Xử lý đồng thời (Concurrency Control) & Toàn vẹn dữ liệu
* **Pessimistic Locking (`SELECT ... FOR UPDATE`)**:
  - Áp dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` khi người dùng bấm giữ ghế. Khi 2 khách cùng chọn 1 ghế trong mili-giây, luồng thứ 2 sẽ bị chặn lại và nhận thông báo lỗi thân thiện thay vì gây tranh chấp dữ liệu.
* **Optimistic Locking (`@Version`)**:
  - Entity `ShowtimeSeat`, `Booking` và `Voucher` đều sở hữu trường `@Version` để ngăn ngừa lỗi ghi đè dữ liệu bị mất (Lost Update).
* **Tính Idempotency của Webhook thanh toán**:
  - Kiểm tra trạng thái đơn hàng và mã giao dịch duy nhất (`paymentReference`, `transactionId`). Nếu cổng thanh toán gửi webhook lặp lại nhiều lần, hệ thống sẽ bỏ qua để **không bị cộng điểm hoặc trừ voucher 2 lần**.
* **Atomic Update chống Double Check-in**:
  - Câu lệnh SQL nguyên tử `UPDATE bookings SET checked_in_at = NOW() WHERE id = ? AND checked_in_at IS NULL` với kiểm tra số dòng bị ảnh hưởng (`affectedRows == 1`), đảm bảo 2 nhân viên quét cùng 1 vé ở 2 cổng khác nhau thì chỉ 1 cổng được chấp thuận.
* **Bảo vệ toàn vẹn tài chính (Financial Integrity)**:
  - Tuyệt đối không dùng kiểu `double` hay `float` mà sử dụng **`BigDecimal`** (scale 2, RoundingMode.HALF_UP) để tính toán tiền tệ, snapshot giá ghế và bắp nước tại thời điểm mua.

### 3. Thuật toán nghiệp vụ cốt lõi
* **Thuật toán Orphan Seat Rule (Chống để lại ghế trống đơn độc)**:
  - Quét từng hàng ghế của phòng chiếu, nhận diện các chuỗi ghế trống (`AVAILABLE`). Nếu người dùng chọn ghế tạo ra khoảng trống chỉ có đúng 1 ghế bị kẹp giữa hoặc sát mép, hệ thống sẽ từ chối để tối ưu doanh số cho rạp.
* **Thuật toán Chống trùng lịch chiếu (Showtime Overlap Detection)**:
  - Tự động tính thời gian kết thúc `endTime = startTime + duration + 15 phút dọn phòng`.
  - Truy vấn kiểm tra giao thoa khoảng thời gian: `(start_time < new_end) AND (end_time > new_start)` để không cho phép xếp 2 phim chiếu đè giờ nhau trong cùng một phòng.

### 4. Đảm bảo chất lượng & Kiểm thử phần mềm (Testing & QA)
* **Kiểm thử tự động (Unit Test & Integration Test)**:
  - Xây dựng bộ test suite hoàn chỉnh với **JUnit 5** và **Mockito** (21/21 test cases passed 100%).
  - Kiểm tra toàn diện từ kiểm tra Constraint DB (`RepositoryMappingTest`), kiểm tra nghiệp vụ thanh toán (`PaymentServiceTest`), kiểm tra soát vé (`CheckInServiceTest`), đến kiểm tra webhook (`SePayWebhookServiceTest`).
* **Validation DTO tập trung**:
  - Sử dụng chuẩn `jakarta.validation.constraints` (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`) gắn kèm `@Valid` tại tất cả Controller.
* **Xử lý ngoại lệ toàn cục (Global Exception Handling)**:
  - Sử dụng `@ControllerAdvice` (`GlobalExceptionHandler`) tự động phân biệt Client gọi đến là REST API (trả về JSON 400/404/409) hay Web Browser (render trang thông báo lỗi đẹp mắt).

---

# PHẦN 3: TỔNG HỢP CÁC CHỨC NĂNG WEB ĐÃ XÂY DỰNG HOÀN THIỆN

### 1. Phân hệ Khách hàng (Customer Web Portal)
1. **Trang chủ & Lịch chiếu (`/`, `/schedule`)**:
   - Dải banner phim nổi bật, danh sách phim Đang chiếu (`NOW_SHOWING`) và Sắp chiếu (`COMING_SOON`).
   - Phân loại nhãn độ tuổi điện ảnh chuẩn: **P, K, C13, C16, C18**.
   - Lịch chiếu theo tab ngày ngang thông minh, gom nhóm theo cụm rạp và các nút bấm giờ chiếu gọn gàng.
2. **Trang chi tiết phim (`/movies/{id}`)**:
   - Hiển thị thông tin đạo diễn, thời lượng, thể loại, mô tả.
   - Popup phát trailer YouTube trực tiếp trong trang.
   - Lịch chiếu của riêng phim đó và xem đánh giá sao (Reviews) từ cộng đồng.
3. **Sơ đồ chọn ghế tương tác Realtime (`/booking/{showtimeId}`)**:
   - Hiển thị trực quan từng loại ghế: Ghế Thường (Standard), Ghế VIP, Ghế Đôi (Couple).
   - Đồng bộ ghế realtime qua WebSocket.
   - Thuật toán kiểm tra Orphan Seat và khóa giữ ghế tạm 5 phút có đồng hồ đếm ngược.
4. **Chọn Bắp nước & Khuyến mãi**:
   - Chọn combo bắp nước, kiểm tra tồn kho tức thì.
   - Nhập mã Voucher giảm giá theo % hoặc tiền cố định.
5. **Cổng thanh toán đa dạng**:
   - Hỗ trợ MoMo QR, VNPAY, Chuyển khoản ngân hàng VietQR (SePay) và Thanh toán tại quầy.
6. **Vé điện tử thông minh (`/bookings/{id}`)**:
   - **Tách vé riêng từng ghế**: Mỗi ghế có mã vé riêng (`ticketCode`), mã QR riêng để chia sẻ qua Zalo/Messenger cho bạn bè cùng đi.
   - Nút **In vé / Lưu PDF**: Layout in vé cinema chuẩn mực.

### 2. Phân hệ Quản lý Soát vé tại Cổng (`/admin/checkin`)
1. **Quét mã QR qua Camera**: Quét trực tiếp mã vé trên điện thoại khách hàng.
2. **Nhập mã thủ công**: Cho phép nhập mã đơn hoặc mã vé khi camera không khả dụng.
3. **Soát vé đa chế độ**:
   - Quét mã vé từng ghế $\rightarrow$ Chỉ check-in đúng ghế đó.
   - Quét mã đơn hàng tổng $\rightarrow$ Tự động check-in toàn bộ các ghế còn lại của đoàn.
4. **Chống gian lận**: Báo lỗi `ALREADY_CHECKED_IN` nếu vé đã được quét trước đó, hiển thị danh sách 20 lượt check-in gần nhất.

### 3. Phân hệ Quản trị viên (Admin Dashboard - `/admin`)
1. **Bảng điều khiển (Dashboard)**: Thống kê doanh thu theo khoảng thời gian, số lượng booking, tỷ lệ lấp đầy rạp và KPI soát vé.
2. **Quản lý Phim**: Thêm, sửa, ẩn phim; chọn trạng thái chiếu và nhãn độ tuổi qua dropdown; upload poster lên Cloudinary.
3. **Quản lý Cụm rạp & Phòng chiếu**: Tạo rạp, tạo phòng chiếu với số hàng/ghế tự động sinh sơ đồ ghế vật lý.
4. **Quản lý Suất chiếu**: Tạo suất chiếu có kiểm tra chống trùng giờ (+15p dọn phòng), tự động tính `endTime`.
5. **Quản lý Bắp nước & Tồn kho**: Quản lý giá, hình ảnh, phân loại và số lượng tồn kho của từng combo.
6. **Quản lý Voucher**: Tạo mã giảm giá, giới hạn số lượt dùng, số tiền tối thiểu, thời hạn sử dụng.
7. **Quản lý Đơn hàng & Duyệt Hoàn tiền (Refund Flow)**:
   - Xem chi tiết danh sách booking không bị lỗi N+1 Query.
   - Nút **Duyệt hoàn tiền**: Đổi trạng thái sang `REFUNDED`, lưu vết kiểm toán (ai duyệt, lý do), tự động trả ghế về trạng thái trống (`AVAILABLE`) để rạp mở bán lại.
