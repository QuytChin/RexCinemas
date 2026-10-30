-- Notification and support fixtures for the account/admin experience. No mail or push is sent.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `account_notifications`
  (`id`, `user_id`, `title`, `message`, `type`, `link`, `read_at_present`, `read_at`, `deduplication_key`, `created_at`)
VALUES
  (1, 7, 'Chào mừng đến Rex Cinemas', 'Tài khoản demo đã sẵn sàng để khảo sát các chức năng khách hàng.', 'WELCOME', '/movies', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 'DEMO:WELCOME:USER7', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (2, 7, 'Đặt vé thành công', 'Booking REXDEMO0001 đã được ghi nhận trong dữ liệu minh họa.', 'PAYMENT_CONFIRMED', '/bookings/1', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 'REXDEMO0001:confirmed', DATE_SUB(NOW(), INTERVAL 7 DAY)),
  (3, 7, 'Thanh toán tại quầy đã xác nhận', 'Booking REXDEMO0002 có vé điện tử và đã check-in trong dữ liệu demo.', 'PAYMENT_CONFIRMED', '/bookings/2', 0, NULL, 'REXDEMO0002:confirmed', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (4, 7, 'Điểm thưởng được điều chỉnh', 'Điểm của booking REXDEMO0006 đã được thu hồi sau khi ghi nhận hoàn vé demo.', 'LOYALTY_REVERSED', '/rewards', 0, NULL, 'DEMO:REVERSE:REXDEMO0006', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  (5, 8, 'Có booking đang chờ xử lý', 'REXDEMO0003 là fixture chờ hết hạn; không có giao dịch ngân hàng hoặc thanh toán thật.', 'BOOKING_PENDING', '/bookings/3', 0, NULL, 'REXDEMO0003:pending', NOW()),
  (6, 13, 'Voucher Rex Rewards đã sử dụng', 'Voucher đổi điểm trong lịch sử demo đã được đánh dấu sử dụng.', 'REWARD_REDEMPTION', '/rewards', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), 'DEMO:REWARD:USED:USER13', DATE_SUB(NOW(), INTERVAL 15 DAY)),
  (7, 5, 'Nhắc nhở kiểm tra vé tại Quận 1', 'Tài khoản nhân viên demo được gán tại Rex Chain Quận 1.', 'STAFF_NOTICE', '/admin/checkin', 0, NULL, 'DEMO:STAFF:Q1:001', NOW());

INSERT INTO `support_messages`
  (`id`, `session_id`, `customer_id`, `customer_name`, `customer_email`, `cinema_id`, `sender_type`, `message`, `is_read`, `created_at`)
VALUES
  (1, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'CUSTOMER', 'Tôi muốn hỏi về cách xem lại vé đã đặt.', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  (2, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'STAFF', 'Bạn có thể đăng nhập rồi mở mục Đặt vé của tôi để xem lịch sử.', 1, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
  (3, 'demo-support-session-001', 7, 'Khách hàng Rex', 'user@rexchain.vn', 1, 'CUSTOMER', 'Cảm ơn, mình đã tìm thấy rồi.', 0, DATE_SUB(NOW(), INTERVAL 100 MINUTE));

INSERT INTO `reviews` (`id`, `movie_id`, `user_id`, `rating`, `comment`, `created_at`, `updated_at`) VALUES
  (1, 1, 7, 5, 'Hình ảnh đẹp, nhịp phim cuốn hút.', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
  (2, 2, 7, 4, 'Câu chuyện nhẹ nhàng, phù hợp để xem cùng bạn bè.', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
