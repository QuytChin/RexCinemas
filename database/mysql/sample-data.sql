-- ========================================================
-- REXCINEMAS - COMPREHENSIVE SAMPLE DATA FOR DEVELOPMENT
-- ========================================================
SET NAMES utf8mb4;

-- Toan bo du lieu mau phu hop 100% voi Frontend UI va Backend Entity
-- Bao gom: Users, Cinemas, Auditoriums, Seats, Movies, Showtimes,
-- ShowtimeSeats, Combos, Vouchers, Events, Articles, Media, Reviews.
-- ========================================================

USE `rex_chain_cinema`;

SET NAMES utf8mb4;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- 1. LOYALTY SETTINGS & MEMBERSHIP POLICIES
INSERT INTO `loyalty_settings`
    (`id`, `version`, `amount_per_point`, `minimum_positive_order_points`, `voucher_stacking_allowed`, `redemption_enabled`, `updated_by`)
VALUES
    (1, 0, 10000.00, 1, 0, 1, 'system')
ON DUPLICATE KEY UPDATE `amount_per_point` = VALUES(`amount_per_point`);

INSERT INTO `membership_tier_policies`
    (`id`, `version`, `tier`, `qualifying_threshold`, `ticket_discount_percent`, `point_multiplier`, `active`, `updated_by`)
VALUES
    (1, 0, 'MEMBER', 0, 0.0000, 1.0000, 1, 'system'),
    (2, 0, 'SILVER', 250, 3.0000, 1.1000, 1, 'system'),
    (3, 0, 'GOLD', 600, 5.0000, 1.2000, 1, 'system'),
    (4, 0, 'DIAMOND', 1200, 8.0000, 1.5000, 1, 'system')
ON DUPLICATE KEY UPDATE `qualifying_threshold` = VALUES(`qualifying_threshold`);

-- 2. CINEMAS (3 cum rap tai TP.HCM)
INSERT INTO `cinemas` (`id`, `name`, `city`, `address`, `phone`, `active`)
VALUES
    (1, 'Rex Chain Quận 1', 'TP. Hồ Chí Minh', '135 Nguyễn Huệ, Phường Bến Nghé, Quận 1', '028 3829 0001', 1),
    (2, 'Rex Chain Gò Vấp', 'TP. Hồ Chí Minh', '18 Phan Văn Trị, Phường 7, Gò Vấp', '028 3895 0002', 1),
    (3, 'Rex Chain Thủ Đức', 'TP. Hồ Chí Minh', '210 Võ Văn Ngân, Phường Bình Thọ, TP. Thủ Đức', '028 3722 0003', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `address` = VALUES(`address`);

-- 3. AUDITORIUMS (Phong chieu)
INSERT INTO `auditoriums` (`id`, `cinema_id`, `name`, `total_rows`, `seats_per_row`)
VALUES
    (1, 1, 'Phòng 01 (Laser 4K)', 6, 10),
    (2, 1, 'Phòng 02 (Dolby Atmos)', 7, 10),
    (3, 2, 'Phòng 01 (Standard)', 6, 8),
    (4, 2, 'Phòng 02 (VIP Cinema)', 6, 10),
    (5, 3, 'Phòng 01 (IMAX Laser)', 7, 10)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 4. USERS (Mat khau mac dinh cho tat ca la '1' hoac '123456')
-- BCrypt hash cua mat khau '1': $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.
INSERT INTO `app_users` 
    (`id`, `email`, `full_name`, `password`, `role`, `cinema_id`, `enabled`, `loyalty_points`, `lifetime_points`, `tier_qualifying_points`, `redeemed_points`, `reversed_points`, `credential_version`, `version`, `created_at`, `favorite_cinema_id`, `phone`)
VALUES
    (1, 'admin@rexchain.vn', 'Rex Administrator', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'ADMIN', NULL, 1, 9999, 9999, 9999, 0, 0, 0, 0, NOW(), 1, '0901234567'),
    (2, 'manager@rexchain.vn', 'Tổng Quản Lý Chuỗi', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'CINEMA_MANAGER', NULL, 1, 500, 500, 500, 0, 0, 0, 0, NOW(), 1, '0902345678'),
    (3, 'user@rexchain.vn', 'Khách hàng Demo', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'CUSTOMER', NULL, 1, 350, 350, 350, 0, 0, 0, 0, NOW(), 1, '0903456789'),
    (4, 'sup1.q1@rexchain.vn', 'Giám Sát 01 - Rex Quận 1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'SUPERVISOR', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, '0904567890'),
    (5, 'sup2.q1@rexchain.vn', 'Giám Sát 02 - Rex Quận 1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'SUPERVISOR', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, '0904567891'),
    (6, 'staff1.q1@rexchain.vn', 'Nhân Viên Soát Vé - Rex Q1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'STAFF', 1, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 1, '0905678901'),
    (7, 'staff2.gv@rexchain.vn', 'Nhân Viên Soát Vé - Rex Gò Vấp', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'STAFF', 2, 1, 0, 0, 0, 0, 0, 0, 0, NOW(), 2, '0906789012')
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`), `password` = VALUES(`password`), `role` = VALUES(`role`), `cinema_id` = VALUES(`cinema_id`);

-- 5. MOVIES (Khop 100% voi thu muc static/images/movies va hero/)
INSERT INTO `movies` 
    (`id`, `title`, `description`, `duration_minutes`, `age_rating`, `status`, `genre`, `language`, `release_date`, `poster_url`, `trailer_url`, `active`, `created_at`)
VALUES
    (1, 'Hành Trình Ánh Sáng', 
     'Một phi hành đoàn trẻ lên đường tìm nguồn năng lượng mới cho Trái Đất giữa hành trình khám phá vũ trụ.',
     125, 'C13', 'NOW_SHOWING', 'Khoa học viễn tưởng, Hành động', 'Tiếng Việt', CURDATE() - INTERVAL 14 DAY, 
     '/images/movies/hanh-trinh-anh-sang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),

    (2, 'Mùa Hè Không Tên', 
     'Nhóm bạn thân trải qua mùa hè cuối cùng bên nhau trước ngưỡng cửa trưởng thành.',
     112, 'P', 'NOW_SHOWING', 'Tâm lý, Tình cảm', 'Tiếng Việt', CURDATE() - INTERVAL 8 DAY, 
     '/images/movies/mua-he-khong-ten.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),

    (3, 'Kẻ Gác Đêm', 
     'Người bảo vệ ca đêm lần theo manh mối của một vụ án bí ẩn tại rạp chiếu bóng cũ.',
     118, 'C16', 'NOW_SHOWING', 'Hành động, Bí ẩn, Giật gân', 'Tiếng Việt', CURDATE() - INTERVAL 4 DAY, 
     '/images/movies/ke-gac-dem.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),

    (4, 'Nhà Có Hai Người', 
     'Hai thế hệ trong một gia đình học cách thấu hiểu nhau qua những câu chuyện đời thường.',
     105, 'P', 'NOW_SHOWING', 'Gia đình, Hài hước', 'Tiếng Việt', CURDATE() - INTERVAL 2 DAY, 
     '/images/movies/nha-co-hai-nguoi.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),

    (5, 'Thành Phố Sau Mưa', 
     'Sau cơn mưa mùa thu, hai tâm hồn cô đơn tình cờ gặp nhau giữa lòng thành phố.',
     110, 'C13', 'COMING_SOON', 'Tình cảm, Tâm lý', 'Tiếng Việt', CURDATE() + INTERVAL 10 DAY, 
     '/images/movies/thanh-pho-sau-mua.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),

    (6, 'Mật Mã Đại Dương', 
     'Trạm nghiên cứu biển sâu phát hiện tín hiệu lạ dẫn đến bí mật dưới đáy đại dương.',
     132, 'C13', 'COMING_SOON', 'Phiêu lưu, Khoa học viễn tưởng', 'Tiếng Việt', CURDATE() + INTERVAL 25 DAY, 
     '/images/movies/mat-ma-dai-duong.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `description` = VALUES(`description`), `poster_url` = VALUES(`poster_url`), `trailer_url` = VALUES(`trailer_url`), `status` = VALUES(`status`);

-- 6. COMBO PRODUCTS (Bap nuoc voi anh tai /images/combos/)
INSERT INTO `combo_products` 
    (`id`, `name`, `description`, `price`, `image_url`, `stock_quantity`, `type`, `active`, `version`, `created_at`)
VALUES
    (1, 'Combo Solo', '1 bắp rang vừa (vị ngọt/mặn) + 1 ly nước ngọt 22oz', 69000.00, '/images/combos/combo-solo.webp', 500, 'COMBO', 1, 0, NOW()),
    (2, 'Combo Couple', '1 bắp rang lớn thơm ngon + 2 ly nước ngọt 22oz', 119000.00, '/images/combos/combo-couple.webp', 500, 'COMBO', 1, 0, NOW()),
    (3, 'Combo Family', '2 bắp rang lớn đặc biệt + 4 ly nước ngọt mát lạnh', 219000.00, '/images/combos/combo-family.webp', 300, 'COMBO', 1, 0, NOW()),
    (4, 'Bắp Caramel', '1 bắp phủ lớp sốt caramel thơm lừng size lớn', 59000.00, '/images/combos/bap-caramel.webp', 400, 'POPCORN', 1, 0, NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `price` = VALUES(`price`);

-- 7. VOUCHERS
INSERT INTO `vouchers` 
    (`id`, `code`, `name`, `discount_type`, `discount_value`, `min_order_amount`, `max_discount_amount`, `valid_from`, `valid_until`, `usage_limit`, `used_count`, `active`, `version`)
VALUES
    (1, 'REX10', 'Giảm 10% thành viên Rex', 'PERCENT', 10.00, 100000.00, 50000.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 180 DAY, 500, 12, 1, 0),
    (2, 'REXVIP', 'Giảm 20% cho hóa đơn từ 200k', 'PERCENT', 20.00, 200000.00, 80000.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 90 DAY, 200, 5, 1, 0),
    (3, 'CHAOXUAN', 'Giảm thẳng 30.000đ đặt vé sớm', 'FIXED', 30000.00, 150000.00, 30000.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY, 300, 25, 1, 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `discount_value` = VALUES(`discount_value`);

-- 8. CINEMA EVENTS (Su kien noi bat trang chu)
INSERT INTO `cinema_events` 
    (`id`, `title`, `label`, `description`, `image_url`, `link_url`, `sort_order`, `active`, `created_at`)
VALUES
    (1, 'Thứ Tư Thành Viên – Giá vé nhẹ hơn, trải nghiệm vẫn trọn vẹn', 'ƯU ĐÃI THÀNH VIÊN', 
     'Đăng nhập thành viên Rex Chain để nhận ưu đãi chọn lọc đồng giá vé 55.000đ và tích lũy điểm thưởng x2 trên mỗi booking thành công vào thứ Tư hàng tuần.', 
     '/images/events/member-wednesday.webp', '/movies', 1, 1, NOW()),
    (2, 'Cuối tuần xem phim – Combo đôi giá cực tốt', 'WEEKEND COMBO', 
     'Thưởng thức trọn vẹn những thước phim bom tấn cùng bạn bè với Combo Couple tiết kiệm 30% khi mua kèm 2 vé xem phim bất kỳ.', 
     '/images/events/weekend-combo.webp', '/schedule', 2, 1, NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 9. ARTICLES (Bai viet & Tin tuc trang chu)
INSERT INTO `articles` 
    (`id`, `title`, `category`, `summary`, `content`, `image_url`, `publish_date`, `sort_order`, `active`, `created_at`)
VALUES
    (1, '5 mẹo chọn vị trí ghế xem phim phù hợp với từng trải nghiệm', 'TIN ĐIỆN ẢNH', 
     'Khoảng cách với màn hình, góc nhìn và hệ thống âm thanh đều ảnh hưởng lớn đến trải nghiệm điện ảnh.', 
     'Vị trí giữa phòng chiếu thường mang lại góc nhìn cân bằng nhất. Với các phòng chiếu Laser hiện đại tại Rex Chain, hàng ghế VIP (F, G) mang đến tầm mắt tự nhiên và độ khuếch đại âm thanh vòm Dolby Atmos chuẩn mực nhất.', 
     '/images/articles/chon-ghe-xem-phim.webp', CURDATE(), 1, 1, NOW()),

    (2, 'Tích điểm Rex Member hoạt động như thế nào?', 'REX MEMBER', 
     'Mỗi booking thành công giúp bạn tích lũy điểm thưởng và thăng hạng thành viên dễ dàng.', 
     'Mỗi 10.000 đồng chi tiêu tương đương với 1 điểm Rex. Tích lũy đạt mốc sẽ tự động đưa bạn lên hạng SILVER, GOLD và DIAMOND với đặc quyền giảm giá vé lên đến 8% cùng voucher bắp nước sinh nhật độc quyền.', 
     '/images/articles/rex-member.webp', CURDATE() - INTERVAL 2 DAY, 2, 1, NOW()),

    (3, 'Từ lịch chiếu đến ghế ngồi: Hệ thống giữ ghế Realtime hoạt động ra sao?', 'HẬU TRƯỜNG', 
     'Công nghệ WebSocket giữ ghế theo thời gian thực giúp ngăn chặn tình trạng đặt trùng ghế.', 
     'Khi một khách hàng chọn vị trí ghế, hệ thống lập tức khóa tạm thời và phát tín hiệu đến tất cả khách hàng đang xem cùng suất chiếu trong vòng chưa đầy 50ms, đảm bảo tính công bằng và mượt mà.', 
     '/images/articles/booking-realtime.webp', CURDATE() - INTERVAL 4 DAY, 3, 1, NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 10. MEDIA ITEMS (Trailer va Clip trai nghiem)
INSERT INTO `media_items` 
    (`id`, `title`, `type`, `thumbnail_url`, `video_url`, `sort_order`, `active`, `created_at`)
VALUES
    (1, 'Trailer tuyển chọn tuần này: Hành Trình Ánh Sáng', 'TRAILER', 
     '/images/media/trailer-tuyen-chon.webp', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 1, 1, NOW()),
    (2, 'Khám phá không gian sang trọng tại Rex Chain Cinemas', 'CLIP', 
     '/images/media/trai-nghiem-rex.webp', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 2, 1, NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 11. REVIEWS (Danh gia phim mau)
INSERT INTO `reviews` (`id`, `movie_id`, `user_id`, `rating`, `comment`, `created_at`, `updated_at`)
VALUES
    (1, 1, 3, 5, 'Kỹ xảo hình ảnh quá mãn nhãn, âm thanh sống động cực kỳ đáng tiền vé!', NOW() - INTERVAL 2 DAY, NOW()),
    (2, 2, 3, 5, 'Phim nhẹ nhàng, nhạc phim hay và diễn xuất của dàn diễn viên trẻ rất tự nhiên.', NOW() - INTERVAL 1 DAY, NOW()),
    (3, 3, 3, 4, 'Cốt truyện kịch tính bất ngờ, cú twist cuối phim rất ấn tượng.', NOW() - INTERVAL 1 DAY, NOW())
ON DUPLICATE KEY UPDATE `comment` = VALUES(`comment`);

-- 12. GENERATE SEATS FOR AUDITORIUM 1 (6 rows A-F x 10 cols)
DELETE FROM `seats` WHERE `auditorium_id` IN (1, 2, 3, 4, 5);

-- Auditorium 1 (6 rows: A-C Standard, D-E VIP, F Couple)
INSERT INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 1, r.row_letter, c.col_num,
       CASE 
           WHEN r.row_letter = 'F' THEN 'COUPLE'
           WHEN r.row_letter IN ('D', 'E') THEN 'VIP'
           ELSE 'STANDARD'
       END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c;

-- Auditorium 2 (7 rows: A-C Standard, D-F VIP, G Couple)
INSERT INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 2, r.row_letter, c.col_num,
       CASE 
           WHEN r.row_letter = 'G' THEN 'COUPLE'
           WHEN r.row_letter IN ('D', 'E', 'F') THEN 'VIP'
           ELSE 'STANDARD'
       END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F' UNION SELECT 'G') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c;

-- Auditorium 3 (6 rows x 8 cols)
INSERT INTO `seats` (`auditorium_id`, `seat_row`, `seat_number`, `type`)
SELECT 3, r.row_letter, c.col_num,
       CASE 
           WHEN r.row_letter = 'F' THEN 'COUPLE'
           WHEN r.row_letter IN ('D', 'E') THEN 'VIP'
           ELSE 'STANDARD'
       END
FROM (SELECT 'A' AS row_letter UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F') r
CROSS JOIN (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) c;

-- 13. SHOWTIMES (Suat chieu tu hom nay den 5 ngay toi, luon moi va active!)
DELETE FROM `showtimes` WHERE `id` > 0;

INSERT INTO `showtimes` (`id`, `movie_id`, `auditorium_id`, `start_time`, `end_time`, `base_price`, `active`)
VALUES
    -- HOM NAY (CURDATE())
    (1, 1, 1, CONCAT(CURDATE(), ' 10:00:00'), CONCAT(CURDATE(), ' 12:05:00'), 85000.00, 1),
    (2, 2, 2, CONCAT(CURDATE(), ' 13:30:00'), CONCAT(CURDATE(), ' 15:22:00'), 85000.00, 1),
    (3, 3, 1, CONCAT(CURDATE(), ' 16:30:00'), CONCAT(CURDATE(), ' 18:28:00'), 95000.00, 1),
    (4, 1, 2, CONCAT(CURDATE(), ' 19:30:00'), CONCAT(CURDATE(), ' 21:35:00'), 95000.00, 1),
    (5, 4, 3, CONCAT(CURDATE(), ' 20:15:00'), CONCAT(CURDATE(), ' 22:00:00'), 85000.00, 1),

    -- NGAY MAI (CURDATE() + 1)
    (6, 1, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 10:30:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 12:35:00'), 85000.00, 1),
    (7, 2, 1, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 14:00:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 15:52:00'), 85000.00, 1),
    (8, 3, 2, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 17:00:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 18:58:00'), 95000.00, 1),
    (9, 4, 2, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 19:45:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 21:30:00'), 95000.00, 1),
    (10, 1, 3, CONCAT(CURDATE() + INTERVAL 1 DAY, ' 20:30:00'), CONCAT(CURDATE() + INTERVAL 1 DAY, ' 22:35:00'), 85000.00, 1),

    -- NGAY KIA (CURDATE() + 2)
    (11, 2, 1, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 13:00:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 14:52:00'), 85000.00, 1),
    (12, 1, 2, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 16:15:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 18:20:00'), 95000.00, 1),
    (13, 3, 1, CONCAT(CURDATE() + INTERVAL 2 DAY, ' 19:00:00'), CONCAT(CURDATE() + INTERVAL 2 DAY, ' 20:58:00'), 95000.00, 1);

-- 14. GENERATE SHOWTIME SEATS (Nap ghe cho tung suat chieu voi gia dung quy tac)
DELETE FROM `showtime_seats` WHERE `id` > 0;

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

-- Danh dau 1 vai ghe da duoc dat truoc de test UI so do ghe sinh dong
UPDATE `showtime_seats` 
SET `status` = 'BOOKED' 
WHERE `showtime_id` IN (1, 2, 4) AND `seat_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `seats` WHERE `seat_row` = 'D' AND `seat_number` IN (4, 5)) tmp
);

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

SELECT 'NAP DU LIEU MAU REXCINEMAS THANH CONG 100%!' AS `Notification`;
