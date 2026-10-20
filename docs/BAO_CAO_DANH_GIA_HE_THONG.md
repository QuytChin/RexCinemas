# Báo cáo rà soát hệ thống RexCinemas

## 1. Phạm vi và cách đọc báo cáo

Báo cáo này tổng hợp chức năng có dấu vết trong source repository, không phải chứng nhận an toàn, hiệu năng hay khả năng sẵn sàng production. Tích hợp với nhà cung cấp bên ngoài chỉ được xem là đã xác minh khi có kiểm thử riêng trên môi trường được phép; build hoặc unit test không chứng minh giao dịch thật đã thành công.

## 2. Nền tảng

- Java 21, Spring Boot 3.3.5, MVC/Thymeleaf, Spring Data JPA/Hibernate.
- Spring Security/JWT; giao diện cập nhật trạng thái ghế qua WebSocket STOMP/SockJS.
- Có cấu hình kết nối MySQL, PostgreSQL và SQL Server. Cần xác nhận schema và migration tương ứng trước khi chạy trên từng hệ quản trị.
- ZXing tạo QR; luồng quét camera phía trình duyệt phụ thuộc quyền camera, HTTPS/localhost và thư viện client.
- SMTP và Cloudinary là tích hợp cấu hình theo môi trường.

## 3. Ma trận trạng thái chức năng

| Phân hệ | Trạng thái theo source | Giới hạn cần lưu ý |
|---|---|---|
| Phim, rạp, phòng, ghế, suất chiếu | Có trang và nghiệp vụ quản trị | Cần kiểm tra dữ liệu/schema phù hợp khi triển khai |
| Đặt vé và giữ ghế | Có giữ chỗ có thời hạn, kiểm tra trạng thái và phát cập nhật ghế | Không có căn cứ từ đây để tuyên bố đã kiểm thử tải đồng thời ở production |
| Lịch chiếu | Có kiểm tra trùng phòng theo thời lượng phim và 15 phút dọn phòng | Không đồng nghĩa đã bảo đảm lịch trên nhiều instance/server đồng thời |
| Combo | Có trường tồn kho và kiểm tra/cập nhật trong luồng booking | Không tuyên bố có đồng bộ kho với hệ thống bán hàng bên ngoài |
| Thanh toán | Checkout hiện dùng `BANK_QR`/VietQR qua SePay | MoMo, VNPAY và thanh toán tại quầy không được hỗ trợ trong checkout hiện tại |
| Webhook và giao dịch | Có xác thực callback theo cấu hình, đối chiếu và ghi nhận giao dịch | Chưa coi là tích hợp thật đã được xác nhận nếu chưa kiểm thử với tài khoản/endpoint nhà cung cấp |
| Hoàn tiền | Có trạng thái chờ xử lý và thao tác ADMIN ghi nhận kết quả | Chuyển tiền hoàn là thao tác thủ công ngoài ứng dụng; không có hoàn tiền tự động |
| QR/check-in | Có mã QR/token và luồng check-in, gồm ngăn quét trùng | Camera cần quyền trình duyệt và localhost/HTTPS; cần thử trên thiết bị thực tế |
| Rex Rewards | Có điểm, hạng, cấu hình và ledger | Chính sách/mức điểm phải đọc theo cấu hình và code hiện hành; không mặc định mọi con số trong tài liệu cũ là quy tắc cố định |
| Role quản lý rạp | Không hoạt động như role quản trị hiện tại | `CINEMA_MANAGER` chỉ còn trong enum tương thích; `/admin/**` yêu cầu `ADMIN`; chưa có phân quyền theo từng rạp |
| Email | Có luồng gửi OTP/đặt lại mật khẩu qua SMTP | Tính năng gửi thư phụ thuộc cấu hình và khả năng gửi SMTP; chưa xác minh bằng tài khoản production |

## 4. Luồng nghiệp vụ chính

### Đặt vé

Khách chọn suất, giữ ghế trong thời hạn, chọn combo/voucher và tạo booking. Checkout hiện chỉ cho phép `BANK_QR`. Booking được xác nhận theo kết quả xử lý giao dịch phù hợp; booking quá hạn giải phóng ghế theo nghiệp vụ hiện có.

### Thanh toán và giao dịch đến muộn

VietQR tạo thông tin chuyển khoản; callback SePay được kiểm tra theo cấu hình và đối chiếu giao dịch với booking. Giao dịch không khớp hoặc cần xem xét được lưu trạng thái để quản trị viên kiểm tra. Với khoản cần hoàn, ứng dụng ghi nhận trạng thái chờ; việc chuyển tiền cho khách diễn ra bên ngoài và phải được ADMIN xác nhận sau đối soát.

### Vé và check-in

Vé/booking có payload QR/token phục vụ tra cứu. Trang Admin check-in cho phép quét QR hoặc nhập mã và kiểm tra trạng thái trước khi đánh dấu đã vào cổng. Chức năng camera cần điều kiện bảo mật của trình duyệt; nên xác minh trực tiếp trên thiết bị mục tiêu.

## 5. Phân quyền hiện hành

`CUSTOMER` dùng các chức năng người dùng. `ADMIN` truy cập trang quản trị và check-in. Mặc dù enum còn giá trị `CINEMA_MANAGER`, code hiện tại không cấp quyền này cho trang Admin; tài liệu không được hiểu đây là role vận hành đang được hỗ trợ.

## 6. Chưa hỗ trợ hoặc chưa được xác minh

- Thanh toán MoMo, VNPAY, thanh toán tại quầy.
- Tự động chuyển tiền hoàn cho khách.
- Quyền nhân viên/quản lý giới hạn theo cụm rạp.
- Tích hợp hệ thống kho, kế toán hoặc POS bên ngoài.
- Kiểm thử tải, đánh giá SLA/độ sẵn sàng production và xác minh giao dịch thật với SePay/ngân hàng.
- Kiểm thử UI camera trên đầy đủ thiết bị/trình duyệt, trừ khi có báo cáo test riêng.

Các mục trên chỉ nên được ghi là định hướng hoặc công việc cần bổ sung, không ghi là chức năng đã hoàn thành.

## 7. Kiểm thử

Kết quả cần ghi kèm ngày chạy, lệnh, môi trường và số test từ log thực tế. Unit/MVC test (thường dùng H2) không chứng minh migration chạy trên database thật, dữ liệu kinh doanh an toàn, email đến hộp thư, webhook từ nhà cung cấp được gửi, hay giao diện hoạt động trên mọi kích thước màn hình. Không đưa tỷ lệ hoàn thiện hoặc kết luận “production-ready” nếu không có tiêu chí và bằng chứng đo lường.
