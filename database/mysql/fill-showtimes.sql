-- ========================================================
-- REXCINEMAS - SCRIPT TỰ ĐỘNG FILL SUẤT CHIẾU VÀ VÉ ĐÃ BÁN
-- ========================================================
-- Script này tạo:
-- 1. Suất chiếu trong 7 ngày qua (có vé đã bán & doanh thu thực tế để test Bảng phân tích lấp đầy ghế)
-- 2. Suất chiếu hôm nay và 7 ngày tới (đầy đủ các khung giờ để khách đặt vé)
-- 3. Tự động sinh toàn bộ sơ đồ ghế (Standard, VIP, Couple) cho từng suất
-- ========================================================

USE `rex_chain_cinema`;

SET NAMES utf8mb4;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- 1. ĐẢM BẢO CÓ ĐỦ GHẾ TRONG PHÒNG CHIẾU (Auditorium 1, 2, 3, 4, 5)
-- Nếu phòng chưa có ghế, tự động tạo ma trận ghế
INSERT IGNORE INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 1, r.row_letter, c.col_num,
       CASE WHEN r.row_letter = 'F' THEN 'COUPLE' WHEN r.row_letter IN ('D', 'E') THEN 'VIP' ELSE 'STANDARD' END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c;

INSERT IGNORE INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 2, r.row_letter, c.col_num,
       CASE WHEN r.row_letter = 'G' THEN 'COUPLE' WHEN r.row_letter IN ('D', 'E', 'F') THEN 'VIP' ELSE 'STANDARD' END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F' UNION SELECT 'G') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c;

INSERT IGNORE INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 3, r.row_letter, c.col_num,
       CASE WHEN r.row_letter = 'F' THEN 'COUPLE' WHEN r.row_letter IN ('D', 'E') THEN 'VIP' ELSE 'STANDARD' END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) c;

-- 2. XÓA CÁC SUẤT CHIẾU CŨ ĐỂ TẠO LẠI BỘ SUẤT CHIẾU MỚI TINH KHÔI
DELETE FROM `booking_seats` WHERE `booking_id` IN (SELECT `id` FROM `bookings` WHERE `showtime_id` > 0);
DELETE FROM `booking_combos` WHERE `booking_id` IN (SELECT `id` FROM `bookings` WHERE `showtime_id` > 0);
DELETE FROM `bookings` WHERE `showtime_id` > 0;
DELETE FROM `showtime_seats` WHERE `showtime_id` > 0;
DELETE FROM `showtimes` WHERE `id` > 0;

-- 3. TẠO SUẤT CHIẾU TRONG 7 NGÀY QUA (CURDATE() - 7 đến CURDATE() - 1)
-- Suất quá khứ để Bảng Tần suất lấp đầy ghế & Doanh thu 7 ngày qua có dữ liệu thật
INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`)
VALUES
    -- 6 ngày trước
    (101, 1, 1, CONCAT(CURDATE() - INTERVAL 6 DAY, ' 14:00:00'), CONCAT(CURDATE() - INTERVAL 6 DAY, ' 16:05:00'), 85000.00, 1),
    (102, 1, 2, CONCAT(CURDATE() - INTERVAL 6 DAY, ' 19:30:00'), CONCAT(CURDATE() - INTERVAL 6 DAY, ' 21:35:00'), 95000.00, 1),
    (103, 3, 1, CONCAT(CURDATE() - INTERVAL 6 DAY, ' 20:00:00'), CONCAT(CURDATE() - INTERVAL 6 DAY, ' 21:58:00'), 95000.00, 1),

    -- 5 ngày trước
    (104, 1, 1, CONCAT(CURDATE() - INTERVAL 5 DAY, ' 10:30:00'), CONCAT(CURDATE() - INTERVAL 5 DAY, ' 12:35:00'), 85000.00, 1),
    (105, 2, 2, CONCAT(CURDATE() - INTERVAL 5 DAY, ' 15:00:00'), CONCAT(CURDATE() - INTERVAL 5 DAY, ' 16:52:00'), 85000.00, 1),
    (106, 1, 1, CONCAT(CURDATE() - INTERVAL 5 DAY, ' 19:00:00'), CONCAT(CURDATE() - INTERVAL 5 DAY, ' 21:05:00'), 95000.00, 1),
    (107, 3, 2, CONCAT(CURDATE() - INTERVAL 5 DAY, ' 20:15:00'), CONCAT(CURDATE() - INTERVAL 5 DAY, ' 22:13:00'), 95000.00, 1),

    -- 4 ngày trước
    (108, 4, 3, CONCAT(CURDATE() - INTERVAL 4 DAY, ' 13:30:00'), CONCAT(CURDATE() - INTERVAL 4 DAY, ' 15:15:00'), 85000.00, 1),
    (109, 1, 2, CONCAT(CURDATE() - INTERVAL 4 DAY, ' 18:30:00'), CONCAT(CURDATE() - INTERVAL 4 DAY, ' 20:35:00'), 95000.00, 1),
    (110, 2, 1, CONCAT(CURDATE() - INTERVAL 4 DAY, ' 19:45:00'), CONCAT(CURDATE() - INTERVAL 4 DAY, ' 21:37:00'), 90000.00, 1),

    -- 3 ngày trước
    (111, 1, 1, CONCAT(CURDATE() - INTERVAL 3 DAY, ' 14:30:00'), CONCAT(CURDATE() - INTERVAL 3 DAY, ' 16:35:00'), 85000.00, 1),
    (112, 3, 2, CONCAT(CURDATE() - INTERVAL 3 DAY, ' 17:30:00'), CONCAT(CURDATE() - INTERVAL 3 DAY, ' 19:28:00'), 90000.00, 1),
    (113, 1, 2, CONCAT(CURDATE() - INTERVAL 3 DAY, ' 20:00:00'), CONCAT(CURDATE() - INTERVAL 3 DAY, ' 22:05:00'), 95000.00, 1),
    (114, 4, 1, CONCAT(CURDATE() - INTERVAL 3 DAY, ' 20:30:00'), CONCAT(CURDATE() - INTERVAL 3 DAY, ' 22:15:00'), 90000.00, 1),

    -- 2 ngày trước
    (115, 2, 1, CONCAT(CURDATE() - INTERVAL 2 DAY, ' 11:00:00'), CONCAT(CURDATE() - INTERVAL 2 DAY, ' 12:52:00'), 85000.00, 1),
    (116, 1, 2, CONCAT(CURDATE() - INTERVAL 2 DAY, ' 15:30:00'), CONCAT(CURDATE() - INTERVAL 2 DAY, ' 17:35:00'), 90000.00, 1),
    (117, 1, 1, CONCAT(CURDATE() - INTERVAL 2 DAY, ' 19:15:00'), CONCAT(CURDATE() - INTERVAL 2 DAY, ' 21:20:00'), 95000.00, 1),
    (118, 3, 2, CONCAT(CURDATE() - INTERVAL 2 DAY, ' 20:30:00'), CONCAT(CURDATE() - INTERVAL 2 DAY, ' 22:28:00'), 95000.00, 1),

    -- 1 ngày trước (Hôm qua)
    (119, 4, 1, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 13:00:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 14:45:00'), 85000.00, 1),
    (120, 1, 2, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 16:00:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 18:05:00'), 90000.00, 1),
    (121, 1, 1, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 19:00:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 21:05:00'), 95000.00, 1),
    (122, 2, 2, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 20:00:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 21:52:00'), 90000.00, 1),
    (123, 3, 1, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 21:30:00'), CONCAT(CURDATE() - INTERVAL 1 DAY, ' 23:28:00'), 90000.00, 1);

-- 4. TẠO SUẤT CHIẾU TƯƠNG LAI (HÔM NAY ĐẾN 6 NGÀY TỚI)
-- Để khách hàng vào website là thấy đầy đủ lịch chiếu tất cả các khung giờ
INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`)
VALUES
    -- HÔM NAY (CURDATE())
    (201, 1, 1, CONCAT(CURDATE(), ' 09:30:00'), CONCAT(CURDATE(), ' 11:35:00'), 80000.00, 1),
    (202, 2, 2, CONCAT(CURDATE(), ' 11:00:00'), CONCAT(CURDATE(), ' 12:52:00'), 80000.00, 1),
    (203, 3, 1, CONCAT(CURDATE(), ' 13:30:00'), CONCAT(CURDATE(), ' 15:28:00'), 85000.00, 1),
    (204, 4, 3, CONCAT(CURDATE(), ' 14:15:00'), CONCAT(CURDATE(), ' 16:00:00'), 85000.00, 1),
    (205, 1, 2, CONCAT(CURDATE(), ' 16:30:00'), CONCAT(CURDATE(), ' 18:35:00'), 90000.00, 1),
    (206, 2, 1, CONCAT(CURDATE(), ' 18:00:00'), CONCAT(CURDATE(), ' 19:52:00'), 95000.00, 1),
    (207, 1, 1, CONCAT(CURDATE(), ' 20:30:00'), CONCAT(CURDATE(), ' 22:35:00'), 95000.00, 1),
    (208, 3, 2, CONCAT(CURDATE(), ' 21:00:00'), CONCAT(CURDATE(), ' 22:58:00'), 95000.00, 1),

    -- NGÀY MAI (CURDATE() + 1)
    (209, 1, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:00:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 12:05:00'), 85000.00, 1),
    (210, 4, 2, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 13:00:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 14:45:00'), 85000.00, 1),
    (211, 2, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 15:30:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 17:22:00'), 90000.00, 1),
    (212, 1, 2, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 18:30:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 20:35:00'), 95000.00, 1),
    (213, 3, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 20:00:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 21:58:00'), 95000.00, 1),

    -- NGÀY KIA (CURDATE() + 2)
    (214, 1, 1, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 10:30:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 12:35:00'), 85000.00, 1),
    (215, 2, 2, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 14:00:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 15:52:00'), 85000.00, 1),
    (216, 1, 2, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 19:15:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 21:20:00'), 95000.00, 1),
    (217, 4, 1, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 20:30:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 22:15:00'), 95000.00, 1),

    -- 3 NGÀY TỚI (CURDATE() + 3)
    (218, 3, 1, CONCAT(CURDATE() + INTERVAL 3 DAY, ' 14:00:00'), CONCAT(CURDATE() + INTERVAL 3 DAY, ' 15:58:00'), 85000.00, 1),
    (219, 1, 2, CONCAT(CURDATE() + INTERVAL 3 DAY, ' 18:45:00'), CONCAT(CURDATE() + INTERVAL 3 DAY, ' 20:50:00'), 95000.00, 1),
    (220, 2, 1, CONCAT(CURDATE() + INTERVAL 3 DAY, ' 20:15:00'), CONCAT(CURDATE() + INTERVAL 3 DAY, ' 22:07:00'), 95000.00, 1),

    -- 4 NGÀY TỚI (CURDATE() + 4)
    (221, 1, 1, CONCAT(CURDATE() + INTERVAL 4 DAY, ' 15:00:00'), CONCAT(CURDATE() + INTERVAL 4 DAY, ' 17:05:00'), 90000.00, 1),
    (222, 4, 2, CONCAT(CURDATE() + INTERVAL 4 DAY, ' 19:00:00'), CONCAT(CURDATE() + INTERVAL 4 DAY, ' 20:45:00'), 95000.00, 1),

    -- 5 NGÀY TỚI (CURDATE() + 5)
    (223, 2, 1, CONCAT(CURDATE() + INTERVAL 5 DAY, ' 16:30:00'), CONCAT(CURDATE() + INTERVAL 5 DAY, ' 18:22:00'), 90000.00, 1),
    (224, 1, 2, CONCAT(CURDATE() + INTERVAL 5 DAY, ' 19:30:00'), CONCAT(CURDATE() + INTERVAL 5 DAY, ' 21:35:00'), 95000.00, 1);

-- 5. SINH TỰ ĐỘNG TOÀN BỘ SƠ ĐỒ GHẾ CHO MỖI SUẤT CHIẾU VỚI GIÁ CHUẨN
-- Ghế Thường = basePrice
-- Ghế VIP = basePrice + 20.000đ
-- Ghế Đôi (Couple) = basePrice + 40.000đ
INSERT INTO `showtime_seats` (`showtime_id`, `seat_id`, `price`, `status`, `version`)
SELECT 
    st.id AS showtime_id,
    s.id AS seat_id,
    CASE 
        WHEN s.type = 'VIP' THEN st.base_price + 20000.00
        WHEN s.type = 'COUPLE' THEN st.base_price + 40000.00
        ELSE st.base_price
    END AS price,
    'AVAILABLE' AS status,
    0 AS version
FROM `showtimes` st
JOIN `seats` s ON s.auditorium_id = st.auditorium_id;

-- 6. TẠO CÁC BOOKING MẪU (CONFIRMED) CHO CÁC SUẤT TRONG 7 NGÀY QUA
-- Để bảng Thống kê Tỷ lệ lấp đầy & Doanh thu có số liệu thực tế tuyệt đẹp:
-- Suất 102 (Hành Trình Ánh Sáng - Giờ tối): Lấp đầy 50 ghế (tỷ lệ 71.4% - Phim rất hot!)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 102 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 2 LIMIT 50) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1001, 'REX-HTAS-102', 3, 102, 5250000.00, 5250000.00, 0, 0, 'CONFIRMED', 'VNPAY', CONCAT(CURDATE() - INTERVAL 6 DAY, ' 18:00:00'), 0, 1, 525, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1001, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK102-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 102 AND ss.status = 'BOOKED';

-- Suất 106 (Hành Trình Ánh Sáng - Giờ tối): Lấp đầy 48 ghế (tỷ lệ 80.0%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 106 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 1 LIMIT 48) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1002, 'REX-HTAS-106', 3, 106, 5040000.00, 5040000.00, 0, 0, 'CONFIRMED', 'SEPAY', CONCAT(CURDATE() - INTERVAL 5 DAY, ' 17:30:00'), 0, 1, 504, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1002, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK106-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 106 AND ss.status = 'BOOKED';

-- Suất 113 (Hành Trình Ánh Sáng - Giờ tối): Lấp đầy 55 ghế (tỷ lệ 78.5%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 113 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 2 LIMIT 55) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1003, 'REX-HTAS-113', 3, 113, 5775000.00, 5775000.00, 0, 0, 'CONFIRMED', 'VNPAY', CONCAT(CURDATE() - INTERVAL 3 DAY, ' 19:00:00'), 0, 1, 577, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1003, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK113-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 113 AND ss.status = 'BOOKED';

-- Suất 107 (Kẻ Gác Đêm - Giờ tối): Lấp đầy 45 ghế (tỷ lệ 64.2%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 107 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 2 LIMIT 45) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1004, 'REX-KGD-107', 3, 107, 4725000.00, 4725000.00, 0, 0, 'CONFIRMED', 'SEPAY', CONCAT(CURDATE() - INTERVAL 5 DAY, ' 19:15:00'), 0, 1, 472, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1004, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK107-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 107 AND ss.status = 'BOOKED';

-- Suất 118 (Kẻ Gác Đêm - Giờ tối): Lấp đầy 42 ghế (tỷ lệ 60.0%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 118 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 2 LIMIT 42) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1005, 'REX-KGD-118', 3, 118, 4410000.00, 4410000.00, 0, 0, 'CONFIRMED', 'VNPAY', CONCAT(CURDATE() - INTERVAL 2 DAY, ' 18:30:00'), 0, 1, 441, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1005, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK118-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 118 AND ss.status = 'BOOKED';

-- Suất 110 (Mùa Hè Không Tên): Lấp đầy 22 ghế (tỷ lệ 36.6%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 110 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 1 LIMIT 22) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1006, 'REX-MHKT-110', 3, 110, 2178000.00, 2178000.00, 0, 0, 'CONFIRMED', 'SEPAY', CONCAT(CURDATE() - INTERVAL 4 DAY, ' 18:00:00'), 0, 1, 217, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1006, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK110-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 110 AND ss.status = 'BOOKED';

-- Suất 114 (Nhà Có Hai Người): Lấp đầy 20 ghế (tỷ lệ 33.3%)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 114 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 1 LIMIT 20) t
);

INSERT INTO `bookings` 
    (`id`, `booking_code`, `user_id`, `showtime_id`, `total_amount`, `seat_amount`, `combo_amount`, `discount_amount`, `status`, `payment_method`, `created_at`, `version`, `points_awarded`, `earned_points`, `points_reversed`)
VALUES
    (1007, 'REX-NCHG-114', 3, 114, 1980000.00, 1980000.00, 0, 0, 'CONFIRMED', 'VNPAY', CONCAT(CURDATE() - INTERVAL 3 DAY, ' 19:30:00'), 0, 1, 198, 0);

INSERT INTO `booking_seats` (`booking_id`, `showtime_seat_id`, `seat_label`, `price`, `ticket_code`, `checked_in`)
SELECT 1007, ss.id, CONCAT(s.seat_row, s.seat_number), ss.price, CONCAT('TK114-', ss.id), 1
FROM `showtime_seats` ss
JOIN `seats` s ON s.id = ss.seat_id
WHERE ss.showtime_id = 114 AND ss.status = 'BOOKED';

-- 7. ĐẶT TRƯỚC 1 VÀI GHẾ Ở SUẤT HÔM NAY (ĐỂ SƠ ĐỒ GHẾ SỐNG ĐỘNG KHI KHÁCH ĐẶT)
UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 207 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 1 AND `seat_row` = 'D' AND `seat_number` IN (4, 5, 6, 7)) t
);

UPDATE `showtime_seats` 
SET `status` = 'BOOKED'
WHERE `showtime_id` = 208 AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `auditorium_id` = 2 AND `seat_row` = 'E' AND `seat_number` IN (5, 6)) t
);

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

SELECT 'TỰ ĐỘNG FILL SUẤT CHIẾU VÀ DỮ LIỆU ĐẶT VÉ THÀNH CÔNG 100%!' AS `Notification`;
