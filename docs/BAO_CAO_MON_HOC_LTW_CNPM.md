# BÁO CÁO TỔNG KẾT ĐỒ ÁN
## Hệ thống đặt vé và quản lý rạp chiếu phim RexCinemas

## 1. Mục tiêu và phạm vi

Đồ án xây dựng ứng dụng web hỗ trợ khách xem phim, lịch chiếu, đặt vé, quản lý tài khoản và các nghiệp vụ quản trị rạp. Báo cáo mô tả những phần hiện có trong source; không xem chức năng dự kiến là chức năng đã hoàn thành.

## 2. Công nghệ sử dụng

- Java 21, Spring Boot 3.3.5.
- Spring MVC, Thymeleaf, Spring Security, Spring Data JPA/Hibernate.
- HTML, CSS, JavaScript và thư viện giao diện được khai báo trong repository.
- WebSocket STOMP/SockJS để phát cập nhật trạng thái ghế.
- Có cấu hình database MySQL, PostgreSQL và SQL Server; cần dùng schema/migration phù hợp với profile đã chọn.
- ZXing tạo mã QR. Camera quét mã chạy phía trình duyệt và phụ thuộc quyền camera cùng điều kiện HTTPS/localhost.
- SePay/VietQR, SMTP và Cloudinary là dịch vụ ngoài, cần cấu hình riêng.

## 3. Thiết kế phần mềm

Ứng dụng được tổ chức theo các lớp controller, service, repository và entity. Controller tiếp nhận request và trả view/API; service xử lý nghiệp vụ; repository/JPA truy cập dữ liệu. Giao diện Thymeleaf dùng template và tài nguyên tĩnh trong `src/main/resources`.

Các domain chính gồm người dùng, phim, rạp, phòng chiếu, ghế, suất chiếu, booking, combo, voucher, giao dịch, check-in và Rex Rewards.

## 4. Chức năng đã có trong source

### Phía khách hàng

- Xem phim, chi tiết phim và lịch chiếu; chọn ngày, rạp, phim theo giao diện hiện có.
- Đăng ký/đăng nhập và các luồng OTP/email, đặt lại mật khẩu khi SMTP được cấu hình.
- Chọn ghế, giữ ghế tạm thời, chọn combo/voucher và tạo booking.
- Thanh toán booking bằng `BANK_QR`/VietQR qua tích hợp SePay.
- Xem booking/vé điện tử và dùng QR/token để check-in.
- Xem Rex Rewards theo dữ liệu và cấu hình hiện có.

### Phía quản trị

- Quản lý phim, rạp, phòng chiếu, suất chiếu, booking, người dùng, nội dung, combo/voucher, thanh toán và Rex Rewards.
- Quản lý số lượng tồn combo trong phạm vi ứng dụng.
- Trang Admin check-in cho phép quét QR hoặc nhập mã.
- Các route `/admin/**` yêu cầu quyền `ADMIN`. Role `CINEMA_MANAGER` còn trong enum phục vụ dữ liệu cũ nhưng hiện không được cấp quyền quản trị; chưa có quyền quản lý giới hạn theo rạp.

### Một số quy tắc nghiệp vụ

- Suất chiếu kiểm tra chồng lấn trong cùng phòng dựa trên thời lượng phim và 15 phút dọn phòng.
- Giữ ghế có thời hạn; booking chưa thanh toán hết hạn được xử lý theo luồng giải phóng ghế.
- Giao dịch đến muộn hoặc cần hoàn tiền có thể được ghi nhận trạng thái chờ để ADMIN đối soát.
- Trạng thái hoàn tiền trong ứng dụng chỉ ghi nhận quy trình; tiền được hoàn thủ công ngoài ứng dụng.

## 5. Chức năng không được tuyên bố là đã hỗ trợ

- MoMo, VNPAY và thanh toán tại quầy không nằm trong luồng checkout hiện tại.
- Không có chức năng tự động chuyển tiền hoàn.
- Không có phân quyền nhân viên/quản lý theo từng rạp.
- Không khẳng định tích hợp POS/kho/kế toán ngoài ứng dụng.
- Không khẳng định đã kiểm thử tải, đạt SLA production, hoặc giao dịch thật với SePay/ngân hàng nếu chưa có bằng chứng kiểm thử riêng.

Các nội dung trên có thể được trình bày là giới hạn hoặc hướng phát triển trong tương lai.

## 6. Kiểm thử và giới hạn xác minh

Repository có unit, MVC, regression và một số integration test. Khi báo cáo kết quả cần nêu chính xác lệnh chạy, môi trường và log tại thời điểm thực hiện. Test dùng H2 không thay thế kiểm thử trên MySQL/PostgreSQL/SQL Server. Build thành công cũng không chứng minh camera, SMTP, Cloudinary, webhook bên ngoài hoặc giao dịch ngân hàng đã hoạt động.

## 7. Hướng phát triển

Các hướng có thể xem xét sau khi có yêu cầu và thiết kế riêng: bổ sung nhà cung cấp thanh toán khác; tích hợp quy trình hoàn tiền với đối tác; xây dựng role quản lý theo rạp; tăng quan sát vận hành và kiểm thử tải. Đây là đề xuất, không phải chức năng hiện tại.
