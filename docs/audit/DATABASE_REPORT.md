# Báo cáo MySQL local

## Danh tính và backup

- Host `localhost`, port `3306`, database `rex_chain_cinema`, MySQL `8.0.46`.
- Backup tạo trước audit: `backups/mysql/rex-cinemas-rex_chain_cinema-20261030-184123.sql`, 98,701 byte; đọc được, chứa 36 lệnh `CREATE TABLE` và 15 `INSERT`. Chưa thử restore độc lập.
- Backup có dữ liệu nhạy cảm theo bản chất database; thư mục backup bị ignore, không đưa vào Git.

## Đọc dữ liệu

- 36 bảng, 31 cột foreign key được đếm. Index `bookings(status, expires_at)` hiện diện. Không phát hiện booking code trùng.
- Bảy tài khoản kiểm thử có mặt, enabled, role/cinema khớp bảng phân quyền: ADMIN, CINEMA_MANAGER, 2 SUPERVISOR ở cinema 1, STAFF ở cinema 1 và 2, CUSTOMER. Hai supervisor ở Q1; dưới ngưỡng ba theo yêu cầu. Mật khẩu được xác nhận bằng BCrypt đối chiếu ứng viên trong memory, không lộ giá trị.
- Không có booking, booking seat, combo booking, payment transaction, loyalty ledger, hold session hoặc booking pending/confirmed. Sáu `showtime_seats` trạng thái `BOOKED` là seed hiển thị seat map trong `database/mysql/sample-data.sql`; không có booking thực tương ứng. Không đánh dấu là orphan booking.
- Không sửa schema hay nghiệp vụ/data booking. Đăng nhập/đăng xuất chạy thật có thể ghi token revocation theo luồng ứng dụng; không reset hoặc sửa tài khoản.

## Giới hạn

Trừ luồng xác thực trên tài khoản test, mọi kiểm tra SQL chỉ đọc. Không chạy migration, Hibernate DDL, chỉnh dữ liệu hay destructive query. Chưa restore backup trên server phụ, chưa chạy MySQL concurrency suite hoặc mọi consistency query trên toàn bảng. Maven suite dùng H2; kết quả H2 không thay thế MySQL integration test.
