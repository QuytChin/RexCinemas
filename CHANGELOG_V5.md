# Rex Chain Cinema V5 — Independent Seat E-Tickets + Refund Management + Print Styling

Bản V5 hoàn thiện **Phân đoạn 4**, kế thừa nền tảng V4, nâng cấp hệ thống vé điện tử độc lập cho từng người xem, quy trình quản lý hoàn tiền cho Admin và giao diện in vé PDF chuyên nghiệp.

---

## 1. Tách vé điện tử độc lập từng ghế (`Independent Seat Tickets`)
- **Mã vé & QR riêng cho từng ghế**:
  - Mỗi ghế (`BookingSeat`) trong một đơn đặt vé đều sở hữu một mã vé độc lập (`ticketCode`, định dạng ví dụ: `TKT-B4-9F2A1C`) và một mã token bảo mật riêng (`qrToken` 64 hex ký tự).
  - Tự động sinh `ticketCode` và `qrToken` ngay khi đơn đặt vé thanh toán thành công (`BookingStatus.CONFIRMED`).
  - Hỗ trợ tương thích ngược: đối với các booking cũ, hệ thống tự động khởi tạo bổ sung `ticketCode` và `qrToken` cho từng ghế ngay lần đầu xem vé.
- **Endpoint ảnh QR riêng**:
  - `GET /bookings/{id}/seats/{seatId}/qr`: Trả về ảnh PNG mã QR riêng cho từng ghế.
  - Cho phép người đặt vé tải hoặc gửi link ảnh QR riêng này cho bạn bè qua Zalo, Messenger, Email mà không cần chia sẻ cả đơn hàng.

---

## 2. Check-in thông minh đa chế độ (`Multi-mode QR Gate Check-in`)
- **Quét vé đơn lẻ (Từng người vào cổng)**:
  - Khách quét mã QR của từng ghế hoặc nhập mã vé `TKT-...`.
  - Hệ thống chỉ check-in đúng ghế đó (`checkedIn = true`, lưu thời gian `checkedInAt` và nhân viên `checkedInBy`).
  - Khi tất cả các ghế trong đơn đều đã check-in, đơn hàng tự động chuyển sang trạng thái check-in hoàn tất.
- **Quét vé theo đoàn (Cả nhóm cùng vào)**:
  - Trưởng nhóm xuất trình mã QR booking tổng (`REXCINEMAS:TICKET:<booking_token>`) hoặc mã đơn `REX...`.
  - Hệ thống tự động xác nhận check-in toàn bộ các ghế còn lại chưa quét của đơn đó trong một thao tác duy nhất.
- **Bảo mật & Kiểm toán**:
  - Lưu chi tiết ai quét ghế nào, thời điểm check-in của từng ghế.
  - Chặn check-in trùng lặp (trả về trạng thái `ALREADY_CHECKED_IN` kèm thời gian ghế đã vào cổng).

---

## 3. Quy trình Quản lý Hoàn tiền (`Refund Management Flow`)
- **Trạng thái `REFUNDED`**:
  - Bổ sung trạng thái `REFUNDED` vào `BookingStatus`.
  - Bổ sung các trường kiểm toán vào `Booking`: `refundedAt`, `refundedBy`, `refundNote`.
- **Nghiệp vụ hoàn tiền an toàn**:
  - Cung cấp phương thức `processRefund(...)` trong `PaymentService` & `BookingService`.
  - Tự động giải phóng ghế đã đặt (`ShowtimeSeat.isBooked = false`) để rạp có thể mở bán lại ghế đó cho khách khác.
  - Đơn đã hoàn tiền sẽ bị vô hiệu hóa mã QR (không thể check-in vào rạp).
- **Giao diện & Quyền Admin**:
  - Endpoint quản trị: `POST /admin/bookings/{id}/refund`.
  - Giao diện Admin (`/admin/bookings`) bổ sung:
    - Badge hiển thị trạng thái `ĐÃ HOÀN TIỀN`.
    - Nút và modal duyệt hoàn tiền kèm trường nhập lý do hoàn tiền.
    - Hiển thị thông tin kiểm toán: ai duyệt hoàn tiền, vào lúc nào và lý do gì.

---

## 4. In vé & Lưu PDF chuyên nghiệp (`Print & PDF E-Ticket Layout`)
- **Trang vé điện tử (`/bookings/{id}/ticket`)**:
  - Bổ sung khối "Vé điện tử từng ghế (Chia sẻ cho bạn bè)": hiển thị từng thẻ vé con với vị trí ghế, loại ghế, mã vé, trạng thái check-in và mã QR riêng biệt.
  - Nút tải QR và chia sẻ cho từng ghế.
- **CSS `@media print` chuyên biệt**:
  - Tự động ẩn thanh điều hướng, các nút bấm, footer khi in.
  - Căn chỉnh bố cục vừa vặn trang giấy hoặc khổ in vé cinema, hiển thị sắc nét các thông tin phim, suất chiếu, phòng chiếu, vị trí ghế và mã QR để lưu PDF hoặc in trực tiếp.

---

## 5. Tương thích cơ sở dữ liệu (`Database Compatibility`)
- Giữ nguyên toàn bộ dữ liệu người dùng, rạp, phim và suất chiếu hiện có.

---

## 6. Chuẩn hóa Model Phim (`MovieStatus & AgeRating Enums`)
- **`MovieStatus`**: Bổ sung enum `NOW_SHOWING` (Đang chiếu), `COMING_SOON` (Sắp chiếu), `ENDED` (Đã kết thúc). Trang chủ và danh mục phim lọc chuẩn xác theo trạng thái chiếu rạp.
- **`AgeRating`**: Chuẩn hóa phân loại độ tuổi thành enum điện ảnh `P`, `K`, `C13`, `C16`, `C18` thay vì chuỗi String tự do, hỗ trợ alias tương thích ngược với dữ liệu cũ (`T13`, `T16`).
- **Quản trị Phim Admin**: Cập nhật form thêm/sửa phim có dropdown chọn độ tuổi và trạng thái chiếu rạp, danh sách phim hiển thị trực quan các badge trạng thái.

---

## 7. Hoàn thiện Giai đoạn 4 — Security, Validation DTO & Database Indexes
- **Validation DTO**: Chuẩn hóa toàn bộ request DTO với Jakarta Validation (`HoldRequest`, `LoginRequest`, `RegisterRequest`, `CheckInRequest`, `CheckoutRequest`, `ComboLine`). Bắt buộc `@Valid` tại tất cả REST & form endpoints.
- **Global Exception Handling**: Mở rộng `GlobalExceptionHandler` hỗ trợ song song cả REST API (trả JSON status/errors chi tiết) và Web MVC (render view error). Bắt lỗi validation, lỗi concurrency (dịch biệt ngữ Hibernate Optimistic/Pessimistic lock thành thông điệp thân thiện) và lỗi nghiệp vụ.
- **Database Composite Indexes**: Bổ sung đầy đủ các composite index quan trọng theo khuyến nghị:
  - `bookings(user_id, created_at)`
  - `showtimes(auditorium_id, start_time)`
  - `showtimes(movie_id, start_time)`
