# BÁO CÁO THẨM ĐỊNH KIẾN TRÚC, NGHIỆP VỤ VÀ AN TOÀN ĐỒNG THỜI
## DỰ ÁN: CINEMA TICKET & F&B BOOKING SYSTEM (REX CHAIN CINEMA)

* **Vai trò thực hiện**: Senior Software Engineer / Software Architect / Code Auditor
* **Hệ thống được thẩm định**: Rex Chain Cinema (Spring Boot 3.3.5, Java 21, Thymeleaf, JPA, WebSocket, MoMo/VNPAY)
* **Mục tiêu**: Đánh giá toàn diện kiến trúc, luồng nghiệp vụ, khả năng chịu tải concurrency, các lỗ hổng race condition, tính toàn vẹn dữ liệu và đề xuất giải pháp tối ưu cho đồ án chuyên ngành / production-ready.

---

## MỤC LỤC
1. [TỔNG QUAN HỆ THỐNG & TECH STACK HIỆN TẠI](#1-tổng-quan-hệ-thống--tech-stack-hiện-tại)
2. [MA TRẬN TÍNH NĂNG (FEATURE MATRIX)](#2-ma-trận-tính-năng-feature-matrix)
3. [KIỂM ĐỊNH CHUYÊN SÂU CONCURRENCY & RACE CONDITION](#3-kiểm-định-chuyên-sâu-concurrency--race-condition)
4. [ĐỐI CHIẾU MÔ HÌNH DỮ LIỆU & QUAN HỆ ENTITY](#4-đối-chiếu-mô-hình-dữ-liệu--quan-hệ-entity)
5. [KIỂM ĐỊNH CÁC NGHIỆP VỤ CỐT LÕI & THUẬT TOÁN](#5-kiểm-định-các-nghiệp-vụ-cốt-lõi--thuật-toán)
6. [ĐÁNH GIÁ AN TOÀN BẢO MẬT (SECURITY AUDIT)](#6-đánh-giá-an-toàn-bảo-mật-security-audit)
7. [RÀ SOÁT API & CHẤT LƯỢNG CODE (CODE QUALITY)](#7-rà-soát-api--chất-lượng-code-code-quality)
8. [BẢNG ĐỐI CHIẾU 25 KỊCH BẢN BIÊN (EDGE CASES)](#8-bảng-đối-chiếu-25-kịch-bản-biên-edge-cases)
9. [LỘ TRÌNH NÂNG CẤP & KHUYẾN NGHỊ TRIỂN KHAI](#9-lộ-trình-nâng-cấp--khuyến-nghị-triển-khai)

---

## 1. TỔNG QUAN HỆ THỐNG & TECH STACK HIỆN TẠI

### 1.1. Công nghệ nền tảng
* **Ngôn ngữ & Runtime**: Java 21 LTS.
* **Framework chính**: Spring Boot `3.3.5`.
* **Data Access & ORM**: Spring Data JPA / Hibernate Core. Sử dụng chiến lược `spring.jpa.hibernate.ddl-auto=update`. Hiện chưa có migration tool chuyên biệt (Flyway/Liquibase).
* **Cơ sở dữ liệu**: Đa cơ sở dữ liệu với cấu hình kích hoạt mặc định là **MySQL** (có sẵn profile cho PostgreSQL và Microsoft SQL Server).
* **Giao diện & Client**:
  * Server-side Rendering với **Thymeleaf** + Bootstrap 5.3 + Custom CSS/Vanilla JS.
  * Tích hợp **WebSocket STOMP** (SockJS) broadcast sơ đồ ghế realtime.
  * Tích hợp **html5-qrcode** (2.3.8) để quét vé qua camera trình duyệt tại quầy soát vé.
* **Xác thực & Bảo mật**:
  * Spring Security 6 kết hợp JWT (`jjwt 0.12.6`).
  * Token lưu trong `HttpOnly Cookie` cho Web MVC và hỗ trợ header `Authorization: Bearer <token>` cho REST API.
  * Phân quyền 3 cấp: `CUSTOMER`, `CINEMA_MANAGER`, `ADMIN`.
* **Cổng thanh toán (Sandbox)**:
  * MoMo API V2 (Chữ ký HMAC-SHA256).
  * VNPAY Sandbox (Chữ ký HMAC-SHA512).
* **Dịch vụ bổ trợ**:
  * ZXing Core 3.5.4 sinh ảnh PNG mã QR cho vé.
  * Cloudinary SDK 1.39.0 lưu trữ poster phim và ảnh combo bắp nước.

### 1.2. Mô hình kiến trúc hiện tại
Dự án áp dụng mô hình **Layered Architecture (N-Tier)**:
$$\text{Client (Thymeleaf / REST)} \longrightarrow \text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{Database}$$

---

## 2. MA TRẬN TÍNH NĂNG (FEATURE MATRIX)

| STT | Chức năng nghiệp vụ | Hiện trạng | Mức hoàn thiện | Đánh giá & Rủi ro tồn đọng |
| :---: | :--- | :---: | :---: | :--- |
| 1 | **Quản lý Phim (Movie)** | Có | **75%** | Thiếu Enum trạng thái (`COMING_SOON`, `NOW_SHOWING`, `ENDED`). Thiếu validate độ tuổi chuẩn. |
| 2 | **Cụm rạp & Phòng chiếu** | Có | **70%** | Phòng chiếu cố định dạng ma trận chữ nhật. Ký tự hàng ghế sinh bằng mã ASCII `('A'+r)` dễ lỗi nếu phòng > 26 hàng. |
| 3 | **Tách biệt Ghế vật lý & Tồn kho** | Có | **90%** | Thiết kế chuẩn: `Seat` vật lý tách rời `ShowtimeSeat` theo suất chiếu. Có `@Version` kiểm soát version. |
| 4 | **Quản lý Suất chiếu (Showtime)** | Có | **65%** | Đã cộng 15 phút dọn phòng. **Tuy nhiên**, logic overlap đặt tại Controller, cửa sổ tìm kiếm hardcode `-6h / +1h` dễ bỏ sót. Thiếu lưu trường `endTime`. |
| 5 | **Giữ ghế tạm thời (Hold Seat)** | Có | **60%** | Giữ 5 phút, broadcast qua WebSocket. **Lỗi lớn**: Chưa xử lý bắt lỗi `OptimisticLockingFailureException`. Thanh toán online làm ghế bị set `BOOKED` ngay lập tức dù chưa trả tiền. |
| 6 | **Kiểm soát Concurrency & Lock** | Một phần | **50%** | Có `@Version` ở `ShowtimeSeat`. Nhưng `Booking` và `CheckIn` không có version/lock, gây nguy cơ Duplicate Webhook và Double Check-in. |
| 7 | **Quy tắc Atomic Hold** | Có | **80%** | Bọc `@Transactional` nên lỗi 1 ghế sẽ rollback cả cụm. Nhưng giao diện hoàn tác còn rời rạc. |
| 8 | **Chống ghế trống đơn lẻ (Orphan Seat)**| Chưa có | **0%** | Chưa có bất kỳ thuật toán nào ngăn chặn việc để lại 1 ghế trống đơn lẻ kẹp giữa hoặc ở đầu hàng. |
| 9 | **Quản lý Bắp nước (F&B)** | Có | **45%** | Hoàn toàn chưa có quản lý tồn kho (`stock_quantity`). Thiếu phân loại (`COMBO`, `POPCORN`, `DRINK`). |
| 10 | **Snapshot giá vé & hóa đơn** | Có | **85%** | Rất tốt: `BookingSeat.price`, `BookingCombo.unitPrice` đều là snapshot. Dùng `BigDecimal`. Backend tự tính giá cuối. |
| 11 | **Cổng thanh toán & Webhook** | Có | **70%** | Đã verify chữ ký HMAC và đối soát số tiền. Thiếu Idempotency kiểm soát gửi trùng Webhook; chưa có cơ chế xử lý hoàn tiền khi thanh toán sau khi hết hạn. |
| 12 | **Tự động nhả ghế (Auto Expire)** | Có | **60%** | Chạy bằng `@Scheduled(fixedRate = 60000)`. Thiếu lock đồng bộ giữa Scheduled Job và Payment Webhook. |
| 13 | **Vé điện tử & Mã QR** | Có | **65%** | Sinh QR PNG từ chuỗi ngẫu nhiên bí mật. Nhược điểm: QR gắn theo cả Booking (1 mã cho cả đoàn), chưa tách theo từng vé riêng lẻ. |
| 14 | **Quét vé vào cổng (Check-in)** | Có | **70%** | Quét camera realtime, validate giờ chiếu + 30 phút, chống quét lại. Thiếu Atomic Update chống 2 cổng quét đồng thời. |
| 15 | **Bảo mật & Phân quyền** | Một phần | **50%** | Phân quyền URL tốt. Lỗ hổng lớn: `csrf().disable()` trong khi dùng xác thực Cookie; thiếu `@Valid` trên DTO. |

---

## 3. KIỂM ĐỊNH CHUYÊN SÂU CONCURRENCY & RACE CONDITION

### 3.1. Tranh chấp giữ ghế (Concurrent Seat Hold)
* **Vị trí**: `BookingService.java` (`holdSeats()`) & `BookingController.java` (`hold()`).
* **Cơ chế hiện tại**: JPA Optimistic Locking (`@Version` trên entity `ShowtimeSeat`).
* **Phân tích rủi ro**:
  * Khi User A và User B cùng bấm giữ ghế A5 cùng một thời điểm, cả 2 luồng đọc bản ghi có `version = 0`.
  * Cả 2 cùng thấy ghế `AVAILABLE`.
  * Thread A commit trước, cập nhật `version = 1`, `status = HELD`.
  * Thread B commit sau, Hibernate so sánh version và ném `ObjectOptimisticLockingFailureException`.
  * Controller chỉ bắt `Exception` chung chung và trả về chuỗi kỹ thuật của Hibernate: `"Row was updated or deleted by another transaction... [ShowtimeSeat#105]"`. Người dùng không hiểu nguyên nhân.
* **Giải pháp khắc phục**:
  * Với hệ thống bán vé rạp phim có độ cạnh tranh cao, khuyến nghị dùng **Pessimistic Write Locking** (`SELECT ... FOR UPDATE`):
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.id IN :ids")
    List<ShowtimeSeat> findByIdInWithLock(@Param("ids") List<Long> ids);
    ```
  * Thread B sẽ phải chờ Thread A giải phóng khóa. Khi Thread B đọc dữ liệu mới nhất, nó thấy ngay `status == SeatStatus.HELD` và trả về thông báo lỗi nghiệp vụ rõ ràng: *"Ghế này vừa được người khác giữ"*.

### 3.2. Đánh dấu ghế "BOOKED" sớm trước khi khách thanh toán thành công
* **Vị trí**: `BookingService.java` (`confirm()`, dòng 118–137).
* **Lỗi nghiệp vụ**:
  ```java
  boolean online = method == PaymentMethod.MOMO_SANDBOX || method == PaymentMethod.VNPAY_SANDBOX;
  booking.setStatus(online ? BookingStatus.PENDING : BookingStatus.CONFIRMED);
  // ...
  for (ShowtimeSeat seat : list) {
      seat.setStatus(SeatStatus.BOOKED); // <-- LỖI TẠI ĐÂY
      seat.setHeldByUserId(null);
      seat.setHoldUntil(null);
  }
  ```
* **Hậu quả**:
  * Khi khách chọn thanh toán qua MoMo/VNPAY và bấm xác nhận, hệ thống redirect khách sang trang cổng thanh toán. Khách **chưa hề trả tiền**.
  * Nhưng ghế đã bị đổi thành `BOOKED`. Nếu khách tắt trình duyệt hoặc không nhập mã OTP, ghế này sẽ bị "treo" ở trạng thái đã bán trong suốt **15 phút** cho đến khi Scheduled Job quét qua. Người khác nhìn vào tưởng vé đã bán mất.
* **Giải pháp**:
  * Khi `online == true`, ghế phải giữ trạng thái `HELD` (hoặc `RESERVED`), gán `holdUntil = now.plusMinutes(15)`.
  * Chỉ chuyển sang `BOOKED` khi nhận được Webhook IPN xác nhận đã thu tiền thành công.

### 3.3. Race Condition tại Webhook thanh toán (Thiếu Idempotency)
* **Vị trí**: `PaymentController.java` & `BookingService.java` (`completeOnlinePayment()`).
* **Phân tích rủi ro**:
  * Khi thanh toán thành công, MoMo/VNPAY kích hoạt cả 2 luồng: Trình duyệt redirect về Return URL và Cổng thanh toán gọi IPN Webhook đồng thời.
  * Phương thức `completeOnlinePayment()` gọi `findByBookingCode(bookingCode)` mà không có khóa dòng (`PESSIMISTIC_WRITE`) và bảng `Booking` không có `@Version`.
  * Nếu 2 request chạm server cùng mili-giây, cả 2 đều thấy `status == PENDING`, dẫn đến:
    1. Voucher bị cộng số lượt dùng 2 lần (`incrementVoucher`).
    2. Điểm tích lũy (`loyaltyPoints`, `lifetimePoints`) bị cộng gấp đôi cho User.
* **Giải pháp**:
  * Bổ sung khóa Pessimistic Write Lock khi load Booking trong `completeOnlinePayment()`:
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingCode = :code")
    Optional<Booking> findByBookingCodeWithLock(@Param("code") String code);
    ```

### 3.4. Xung đột giữa Scheduled Job Auto-Expire và Webhook thanh toán
* **Kịch bản**:
  * Khách thao tác thanh toán ở phút 14:59. Đến phút 15:00, Scheduled Job chạy, hủy đơn (`CANCELLED`) và nhả ghế về `AVAILABLE`.
  * Đến phút 15:01, Webhook từ MoMo/VNPAY gửi về báo khách đã trừ tiền thành công.
  * Hiện tại, `completeOnlinePayment()` thấy trạng thái không còn `PENDING` sẽ ném lỗi 409/99 và từ chối xử lý.
  * Khách bị mất tiền thật nhưng đơn bị hủy, ghế đã bị người khác mua mất.
* **Giải pháp**:
  * Thêm trạng thái `REFUND_PENDING`. Nếu Webhook báo thành công mà đơn đã bị `CANCELLED`, hệ thống không ném lỗi mà cập nhật đơn sang `REFUND_PENDING`, lưu lại mã giao dịch ngân hàng và cảnh báo Quản trị viên để tiến hành hoàn tiền (refund) cho khách.

---

## 4. ĐỐI CHIẾU MÔ HÌNH DỮ LIỆU & QUAN HỆ ENTITY

### 4.1. Bảng đối chiếu thực thể
```text
CURRENT ARCHITECTURE                       RECOMMENDED TARGET ARCHITECTURE
====================                       ===============================
Cinema (id, name, city, address)           Cinema (id, name, city, address)
  └── Auditorium (totalRows, seatsPerRow)    └── Room (cinema_id, name, total_seats, type)
        └── Seat (row, number, type)               └── Seat (room_id, row, col, type, active)

Movie (duration, active, String ageRating) Movie (duration, age_rating [Enum], status [Enum])
  └── Showtime (startTime, basePrice)        └── Showtime (movie_id, room_id, start_time, end_time, base_price)
        └── ShowtimeSeat (version, price)          └── ShowtimeSeat (showtime_id, seat_id, status, version, price)

User (email, role, loyaltyPoints)          User (email, role, loyaltyPoints)
  └── Booking (bookingCode, qrToken)         └── Booking (booking_code, status, expires_at, total_amount)
        ├── BookingSeat (price, label)             ├── Payment (booking_id, method, amount, trans_id, status)
        └── BookingCombo (qty, unitPrice)          ├── Ticket (booking_id, showtime_seat_id, ticket_code, qr_token)
                                                   └── BookingSnack (booking_id, snack_id, qty, unit_price)
                                           Snack (name, type [Enum], price, stock_quantity, active)
```

### 4.2. Những thiếu sót cốt lõi trong Entity hiện tại
1. **Thiếu `endTime` trong `Showtime`**: Khiến mọi phép tính kiểm tra trùng lịch phải tính on-the-fly, không thể lập chỉ mục (index) hoặc viết câu query range SQL tối ưu.
2. **Thiếu quản lý tồn kho F&B**: `ComboProduct` không có cột `stock_quantity`.
3. **Vé (`Ticket`) bị gộp vào `Booking`**: Check-in và mã QR hiện nằm trên bảng `Booking`. Nếu một khách đặt vé cho 4 người trong gia đình thì bắt buộc cả 4 người phải cùng vào cổng một lúc; không thể tách vé cho từng người vào trước/sau.

---

## 5. KIỂM ĐỊNH CÁC NGHIỆP VỤ CỐT LÕI & THUẬT TOÁN

### 5.1. Thuật toán kiểm tra trùng lịch chiếu (Showtime Overlap)
* **Hiện trạng**: Nằm tại `AdminController.java` (`validateShowtimeSlot()`).
* **Lỗi**:
  * Logic nằm ở Controller là sai tầng kiến trúc.
  * Cửa sổ query `startTime.minusHours(6)` đến `end.plusHours(1)` là dạng heuristic, có thể bỏ sót những suất chiếu đặc biệt kéo dài hơn bình thường.
  * Chưa có lock ngăn chặn 2 Admin cùng tạo 2 lịch chiếu trùng nhau tại cùng một giây.
* **Công thức chuẩn**:
  Hai khoảng thời gian $[S_1, E_1]$ và $[S_2, E_2]$ bị trùng nhau khi và chỉ khi:
  $$\text{newStart} < \text{existingEnd} \quad \text{VÀ} \quad \text{newEnd} > \text{existingStart}$$
  Với $\text{end} = \text{start} + \text{movie.durationMinutes} + 15\text{ phút (dọn rạp)}$.

### 5.2. Thuật toán chống ghế trống đơn lẻ (Orphan Seat Rule)
Hệ thống hiện tại chưa có. Dưới đây là thiết kế thuật toán chuẩn trước khi áp dụng:
1. Sắp xếp toàn bộ ghế trong hàng theo số thứ tự cột tăng dần ($1, 2, \dots, N$).
2. Xác định trạng thái của từng ghế sau khi user chọn:
   * `OCCUPIED`: Đã bán (`BOOKED`), đang được người khác giữ (`HELD`), hoặc user đang chọn.
   * `EMPTY`: Ghế trống còn lại.
3. Quét chuỗi các ghế `EMPTY` liên tiếp:
   * Nếu phát hiện có đúng **1 ghế trống đơn lẻ** mà hai bên cạnh nó đều là tường/lối đi hoặc ghế `OCCUPIED`:
     $$\dots [\text{OCCUPIED}] \quad [\text{EMPTY (1 ghế duy nhất)}] \quad [\text{OCCUPIED}] \dots$$
   * $\longrightarrow$ **Từ chối thao tác** và thông báo: *"Không được để lại ghế đơn lẻ trống kẹp giữa. Vui lòng chọn ghế liền kề."*

---

## 6. ĐÁNH GIÁ AN TOÀN BẢO MẬT (SECURITY AUDIT)

1. **Vấn đề CSRF (Cross-Site Request Forgery)**:
   * `SecurityConfig.java` cấu hình `http.csrf(csrf -> csrf.disable())`.
   * Hệ thống sử dụng Cookie để lưu `access_token`. Khi CSRF bị tắt, nếu người dùng truy cập một website giả mạo, website đó có thể ngầm kích hoạt request `POST /booking/confirm` hoặc các endpoint admin.
   * **Khắc phục**: Bật CSRF protection cho các form MVC Thymeleaf hoặc chuyển sang xác thực hoàn toàn qua header `Authorization: Bearer` cho REST client.
2. **Kiểm tra tính toàn vẹn giá (Price Integrity)**:
   * **Đánh giá**: **RẤT TỐT**. Backend hoàn toàn tự tính lại tổng tiền từ DB (giá ghế snapshot + giá combo + voucher). Không hề tin tưởng tổng tiền gửi từ Client.
3. **Phân quyền & Kiểm soát truy cập (IDOR)**:
   * Endpoint xem vé `/bookings/{id}` đã kiểm tra `booking.getUser().getId().equals(currentUser.getId())`. Khách không xem trộm được vé của nhau.
4. **Kiểm soát tính hợp lệ của ghế**:
   * Code đã kiểm tra `!seat.getShowtime().getId().equals(showtimeId)`. Khách không thể hack ghế của phòng chiếu khác hay suất chiếu khác.

---

## 7. RÀ SOÁT API & CHẤT LƯỢNG CODE (CODE QUALITY)

### 7.1. Hiện tượng God Class & Trách nhiệm đơn lẻ
* `BookingService.java` hiện dài hơn 400 dòng, đảm nhận tới 7 nghiệp vụ khác nhau (Hold, Confirm, Payment, Points, Voucher, QR, CheckIn, Expiry Job). Cần tái cấu trúc thành các service chuyên biệt: `SeatReservationService`, `PaymentService`, `CheckInService`.

### 7.2. Lỗi hiệu năng N+1 Query
* Phương thức `BookingRepository.findAllDetailed()` join fetch `user`, `showtime`, `movie`, `auditorium`, `cinema`, `combos` nhưng **không join fetch `seats`**. Khi view `admin/bookings.html` hiển thị danh sách ghế, Hibernate phải phát sinh thêm N câu query phụ.

### 7.3. Tiền tệ và tính toán tài chính
* **Điểm sáng xuất sắc**: Toàn bộ hệ thống sử dụng `BigDecimal` với scale 2 và `RoundingMode` rõ ràng. Không dùng `float` hay `double`, đảm bảo độ chính xác tuyệt đối trong giao dịch tiền tệ.

---

## 8. BẢNG ĐỐI CHIẾU 25 KỊCH BẢN BIÊN (EDGE CASES)

| STT | Kịch bản biên | Hiện trạng code | Kết luận & Rủi ro |
| :---: | :--- | :--- | :--- |
| 1 | Hai user cùng giữ một ghế | Chặn bằng `@Version` nhưng văng lỗi 500 Hibernate | ⚠️ Cần chuyển sang Pessimistic Lock |
| 2 | User giữ lại ghế của chính mình | Code có check `ownHold` và gia hạn 5 phút | ✅ Xử lý tốt |
| 3 | User chọn ghế của Showtime khác | Chặn tại `!seat.getShowtime().getId().equals(id)` | ✅ An toàn |
| 4 | User chọn ghế đã BOOKED | Chặn tại dòng 66 `BookingService.java` | ✅ An toàn |
| 5 | Ghế HELD đã hết hạn | Tự động reset về `AVAILABLE` rồi cấp hold mới | ✅ Xử lý linh hoạt |
| 6 | Booking hết hạn đúng lúc thanh toán thành công | Cổng báo thành công nhưng DB đã CANCELLED, từ chối xử lý | ❌ Lỗi: Mất tiền của khách, thiếu flow refund |
| 7 | Webhook gọi lặp lại nhiều lần | Cùng đọc `PENDING` -> cộng điểm & trừ voucher 2 lần | ❌ Lỗi Race Condition nghiêm trọng |
| 8 | User refresh trang payment return | Đã CONFIRMED thì return booking | ✅ Khá an toàn |
| 9 | User mở 2 tab đặt 2 ghế khác nhau | Xử lý độc lập bình thường | ✅ Hoạt động tốt |
| 10 | User đặt F&B vượt quá tồn kho | Không kiểm tra do chưa có trường `stock_quantity` | ❌ Thiếu kiểm soát kho |
| 11 | Admin giảm stock lúc user checkout | Chưa có trường tồn kho | ❌ Chưa đáp ứng |
| 12 | Admin xóa Showtime đang có Booking | Chỉ set `active = false`, không xử lý vé đã bán | ⚠️ Cần cảnh báo & flow hoàn tiền |
| 13 | Admin đổi phòng của suất đã bán vé | Đã có validate `existsByShowtimeId...` chặn lại | ✅ Rất tốt |
| 14 | Admin sửa sơ đồ ghế vật lý của phòng | Suất chiếu dùng snapshot `ShowtimeSeat` riêng | ✅ Không ảnh hưởng suất cũ |
| 15 | Suất bắt đầu nhưng booking còn PENDING | Không cho confirm nếu `startTime <= now` | ✅ An toàn |
| 16 | User cố đặt suất đã chiếu | Bị chặn ngay từ đầu vào | ✅ An toàn |
| 17 | Khách thanh toán sai lệch số tiền | MoMo & VNPAY verify số tiền tới từng đồng | ✅ Rất chặt chẽ |
| 18 | Quét vé check-in 2 lần | Chặn tuần tự tốt; nhưng 2 cổng quét cùng lúc có thể lọt | ⚠️ Cần Atomic Update |
| 19 | QR bị chụp lén mang vào trước | Ai quét trước được vào, người sau bị báo `ALREADY_CHECKED_IN` | ✅ Đúng nghiệp vụ |
| 20 | Scheduled Job và Webhook chạy cùng lúc | Xung đột ghi dữ liệu (cùng hủy và cùng nhận) | ❌ Nguy cơ mất tính nhất quán |
| 21 | Xóa Phim đang có Suất chiếu | Set `active = false`, không làm vỡ quan hệ | ✅ An toàn |
| 22 | Xóa Phòng đang có Suất chiếu | Hiện chưa hỗ trợ xóa phòng | ⚠️ Cần thêm ràng buộc |
| 23 | Xóa Ghế đã từng bán vé | Khóa ngoại DB chặn xóa cứng | ✅ Toàn vẹn dữ liệu |
| 24 | F&B bị ẩn sau khi đã bán | Booking đã snapshot tên và giá độc lập | ✅ Rất tốt |
| 25 | Giá F&B tăng sau khi bán | Không ảnh hưởng đơn cũ do đã snapshot giá | ✅ Tuyệt đối an toàn |

---

## 9. LỘ TRÌNH NÂNG CẤP & KHUYẾN NGHỊ TRIỂN KHAI

### Giai đoạn 1: Khắc phục lỗi Concurrency & Trạng thái thanh toán (Ưu tiên P0)
1. **Không set BOOKED sớm**: Giữ nguyên trạng thái `HELD` khi khởi tạo thanh toán online; chỉ chuyển `BOOKED` khi Webhook báo thành công.
2. **Khóa bản ghi Booking khi nhận Webhook**: Dùng `PESSIMISTIC_WRITE` hoặc cơ chế Idempotency dựa trên `transaction_id` duy nhất để ngăn nhân đôi điểm tích lũy và voucher.
3. **Chuyển cơ chế giữ ghế sang Pessimistic Lock**: Thêm `@Lock(LockModeType.PESSIMISTIC_WRITE)` vào truy vấn tìm ghế để triệt tiêu lỗi Optimistic Locking và trả về lỗi thân thiện cho client.
4. **Xử lý tình huống thanh toán sau khi hết hạn**: Thêm trạng thái `REFUND_PENDING` lưu dấu vết giao dịch để hoàn tiền khi cần.

### Giai đoạn 2: Hoàn thiện tính năng & Ràng buộc nghiệp vụ (Ưu tiên P1)
1. **Chuẩn hóa kiểm tra Overlap lịch chiếu**: Đưa logic vào Service, lưu trường `end_time` vào bảng `showtimes` và viết câu query SQL kiểm tra giao thoa khoảng thời gian.
2. **Cài đặt thuật toán Orphan Seat**: Chặn các lượt chọn ghế để lại 1 vị trí trống đơn lẻ.
3. **Bổ sung quản lý tồn kho F&B**: Thêm `stock_quantity` vào `ComboProduct` và trừ kho nguyên tử khi thanh toán thành công.
4. **Tách vé điện tử độc lập (Ticket)**: Cho phép mỗi ghế trong booking có mã vé và QR riêng để khách đi theo nhóm có thể check-in độc lập.

### Giai đoạn 3: Tối ưu hiệu năng, Kiến trúc & Bảo mật (Ưu tiên P2)
1. Thêm chỉ mục Database (Index) cho các cột: `showtime_seats(showtime_id, status)`, `showtime_seats(status, hold_until)`, `bookings(status, created_at)`.
2. Khắc phục N+1 query tại trang Admin Bookings bằng `join fetch b.seats`.
3. Bật bảo vệ CSRF cho form MVC. Tách nhỏ `BookingService` để code sạch và dễ bảo trì.
