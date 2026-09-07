# Rex Chain Cinema V4 — Booking Schedule + QR Gate Check-in

Bản V4 được ghép trực tiếp trên **V3 local-ready** để giữ nguyên cấu hình MySQL/IntelliJ đã chạy trên máy người dùng.

## 1. Lịch chiếu đặt vé trực tiếp
- `/schedule` hiển thị số ghế còn trống / tổng ghế cho từng suất.
- Suất còn ghế có nút **ĐẶT VÉ** trực tiếp.
- Khách chưa đăng nhập bấm đặt vé sẽ được chuyển sang login, sau login quay lại đúng `/booking/{showtimeId}`.
- Trang chi tiết phim cũng hiển thị số ghế còn và đặt trực tiếp.
- Trang chủ có dải **Đặt nhanh – Suất chiếu sắp tới**.
- API showtime trả thêm `availableSeats`, `totalSeats`, `bookingUrl`.

## 2. QR vé điện tử thật
- Thêm ZXing Core 3.5.4.
- Booking mới được cấp `qrToken` ngẫu nhiên 64 hex ký tự.
- QR chứa token bí mật dạng `REXCINEMAS:TICKET:<token>`, không dùng ID database.
- Endpoint ảnh QR: `GET /bookings/{id}/qr`.
- Chỉ booking `CONFIRMED` mới sinh QR hợp lệ.
- Booking cũ từ V3 vẫn dùng được: token sẽ được tạo tự động lần đầu mở QR.
- Vé điện tử có nút in/lưu PDF và hiển thị trạng thái check-in.

## 3. Máy quét QR / Check-in tại cổng
- Trang mới: `/admin/checkin` cho `ADMIN` và `CINEMA_MANAGER`.
- Quét bằng camera hoặc ảnh QR qua html5-qrcode.
- Có ô nhập mã booking thủ công nếu camera không dùng được.
- Backend kiểm tra:
  - booking tồn tại;
  - đã `CONFIRMED`;
  - chưa bị hủy;
  - chưa check-in trước đó;
  - suất chiếu chưa kết thúc quá 30 phút.
- Check-in thành công lưu `checkedInAt`, `checkedInBy`.
- Quét lần hai trả trạng thái `ALREADY_CHECKED_IN`.
- Vé pending/cancelled/expired bị từ chối.
- Danh sách 20 check-in gần nhất ngay trên màn scanner.

## 4. Nâng cấp quản trị suất chiếu
- Tạo suất chiếu tự sinh ghế và mở bán.
- Chặn tạo/sửa suất bị chồng giờ trong cùng phòng.
- Khoảng chống trùng = thời lượng phim + 15 phút dọn phòng.
- Suất đã có vé bán ra không cho đổi giờ/giá; admin có thể ẩn để ngừng bán thêm.
- Seeder tự bổ sung lịch demo mới nếu database cũ không còn suất tương lai trong 8 ngày.

## 5. Booking/Admin/Dashboard
- Admin booking hiển thị `CHƯA CHECK-IN` / `ĐÃ CHECK-IN`.
- Có nút mở vé/QR từ bảng booking.
- Booking đã check-in không thể bị hủy.
- Dashboard thêm KPI số booking đã check-in QR.
- API booking trả thêm `checkedIn`, `checkedInAt`, `ticketUrl`, `qrImageUrl`.

## 6. Login UX
- Spring Security redirect route cần đăng nhập về `/auth/login?returnUrl=...`.
- Login thành công quay lại đúng màn hình người dùng đang muốn mở.
- API chưa đăng nhập vẫn trả HTTP 401 thay vì redirect HTML.

## 7. Database migration
Không cần xóa database V3. Với `spring.jpa.hibernate.ddl-auto=update`, JPA sẽ thêm vào `bookings`:
- `qr_token`
- `checked_in_at`
- `checked_in_by`

Cấu hình `application-mysql.properties` từ bản local-ready được giữ nguyên byte-for-byte.
