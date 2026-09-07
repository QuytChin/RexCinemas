# Rex Chain Cinema — Upgrade V2

## Khách hàng
- Profile: cập nhật họ tên, điện thoại, địa chỉ, avatar Cloudinary và đổi mật khẩu.
- Review phim 1–5 sao; mỗi user có một review/phim và có thể cập nhật lại.
- Combo bắp nước trong checkout.
- Voucher phần trăm/cố định, đơn tối thiểu, giảm tối đa, thời gian áp dụng, giới hạn lượt dùng.
- Lịch sử booking hiển thị trạng thái.
- Vé điện tử có breakdown tiền ghế, combo, voucher và tổng thanh toán.

## Realtime booking
- WebSocket STOMP + SockJS giữ ghế realtime.
- Giữ ghế 5 phút.
- Checkout lỗi sẽ giải phóng hold ngay.
- Booking online khóa ghế tối đa 15 phút; timeout tự hủy và trả ghế.

## Thanh toán
- PAY_AT_COUNTER.
- MoMo sandbox: tạo payment URL + return callback + IPN, HMAC-SHA256.
- VNPAY sandbox v2.1.0: redirect + return callback + IPN, HMAC-SHA512.
- Callback đối chiếu chữ ký, booking, payment method và amount trước khi confirm.

## Admin / Cinema Manager
- Dashboard KPI + biểu đồ doanh thu 6 tháng.
- Quản lý combo.
- Quản lý voucher.
- Quản lý review.
- Quản lý booking.
- ADMIN: quản lý user, đổi role, khóa/mở khóa.

## Cấu hình
- MySQL mặc định.
- Có profile PostgreSQL và SQL Server.
- Cloudinary qua biến môi trường.
- MoMo/VNPAY credentials qua biến môi trường.
