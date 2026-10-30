-- Vietnamese movie catalog reuses image assets shipped in src/main/resources/static/images.
USE `rex_chain_cinema`;
SET NAMES utf8mb4;

INSERT INTO `movies`
  (`id`, `title`, `description`, `duration_minutes`, `age_rating`, `status`, `genre`, `language`, `release_date`, `poster_url`, `trailer_url`, `active`, `created_at`)
VALUES
  (1, 'Hành Trình Ánh Sáng', 'Một phi hành đoàn trẻ lên đường tìm nguồn năng lượng mới cho Trái Đất giữa hành trình khám phá vũ trụ.', 125, 'C13', 'NOW_SHOWING', 'Khoa học viễn tưởng, Hành động', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 14 DAY), '/images/movies/hanh-trinh-anh-sang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (2, 'Mùa Hè Không Tên', 'Nhóm bạn thân trải qua mùa hè cuối cùng bên nhau trước ngưỡng cửa trưởng thành.', 112, 'P', 'NOW_SHOWING', 'Tâm lý, Tình cảm', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 8 DAY), '/images/movies/mua-he-khong-ten.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (3, 'Kẻ Gác Đêm', 'Người bảo vệ ca đêm lần theo manh mối của một vụ án bí ẩn tại rạp chiếu bóng cũ.', 118, 'C16', 'NOW_SHOWING', 'Hành động, Bí ẩn', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '/images/movies/ke-gac-dem.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (4, 'Nhà Có Hai Người', 'Hai thế hệ trong một gia đình học cách thấu hiểu nhau qua những câu chuyện đời thường.', 105, 'P', 'NOW_SHOWING', 'Gia đình, Hài', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '/images/movies/nha-co-hai-nguoi.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (5, 'Thành Phố Sau Mưa', 'Sau cơn mưa mùa thu, hai tâm hồn cô đơn tình cờ gặp nhau giữa lòng thành phố.', 110, 'C13', 'COMING_SOON', 'Tình cảm, Tâm lý', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '/images/movies/thanh-pho-sau-mua.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (6, 'Mật Mã Đại Dương', 'Trạm nghiên cứu biển sâu phát hiện tín hiệu lạ dẫn đến bí mật dưới đáy đại dương.', 132, 'C13', 'COMING_SOON', 'Phiêu lưu, Khoa học viễn tưởng', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 18 DAY), '/images/movies/mat-ma-dai-duong.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (7, 'Đường Chân Trời Mùa Hạ', 'An trở về miền biển quê nhà và tìm lại ước mơ trong một mùa hè đáng nhớ.', 108, 'P', 'NOW_SHOWING', 'Tình cảm, Gia đình', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 12 DAY), '/images/movies/collection/duong-chan-troi-mua-ha.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (8, 'Bí Mật Ga Cuối', 'Chiếc va-li bỏ quên ở sân ga mở ra bí mật gắn với chuyến tàu đêm.', 116, 'C13', 'NOW_SHOWING', 'Bí ẩn, Tâm lý', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 9 DAY), '/images/movies/collection/bi-mat-ga-cuoi.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (9, 'Bếp Nhà Có Nắng', 'Ba thế hệ trong gia đình tìm thấy niềm vui bên những bữa cơm nhà.', 101, 'P', 'NOW_SHOWING', 'Gia đình, Hài', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 5 DAY), '/images/movies/collection/bep-nha-co-nang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (10, 'Thành Phố Trong Mưa', 'Một kiến trúc sư trẻ đi tìm ký ức và lựa chọn mới giữa thành phố ngày mưa.', 121, 'C13', 'COMING_SOON', 'Tình cảm, Chính kịch', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '/images/movies/collection/thanh-pho-trong-mua.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (11, 'Hành Tinh Thứ Chín', 'Đoàn thám hiểm phát hiện dấu vết lạ trên hành tinh xa ngoài rìa hệ Mặt Trời.', 130, 'C13', 'COMING_SOON', 'Khoa học viễn tưởng, Phiêu lưu', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 8 DAY), '/images/movies/collection/hanh-tinh-thu-chin.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (12, 'Chuyến Tàu Đêm', 'Những hành khách xa lạ lần theo manh mối về vụ mất tích trên chuyến tàu đêm.', 114, 'C16', 'COMING_SOON', 'Bí ẩn, Giật gân', 'Tiếng Việt', DATE_ADD(CURDATE(), INTERVAL 15 DAY), '/images/movies/collection/chuyen-tau-dem.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (13, 'Vườn Sao Nhỏ', 'Cô bé Linh khám phá khu vườn kỳ diệu chỉ xuất hiện khi màn đêm buông xuống.', 96, 'P', 'ENDED', 'Gia đình, Kỳ ảo', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 120 DAY), '/images/movies/collection/vuon-sao-nho.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (14, 'Ngày Gió Trở Về', 'Trở về quê biển, Huy tìm cách hàn gắn tình thân và bắt đầu lại.', 107, 'P', 'ENDED', 'Chính kịch, Gia đình', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 90 DAY), '/images/movies/collection/ngay-gio-tro-ve.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (15, 'Mật Mã Ánh Trăng', 'Một mật mã cổ dưới đài thiên văn dẫn nữ khảo cổ đến bí ẩn gia đình.', 123, 'C13', 'ENDED', 'Phiêu lưu, Bí ẩn', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 70 DAY), '/images/movies/collection/mat-ma-anh-trang.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (16, 'Tiệm Sách Cuối Phố', 'Hiệu sách nhỏ trở thành nơi những người xa lạ tìm thấy niềm vui và khởi đầu mới.', 99, 'P', 'ENDED', 'Tình cảm, Chính kịch', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 55 DAY), '/images/movies/collection/tien-sach-cuoi-pho.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (17, 'Rừng Thì Thầm', 'Hai người đi rừng lạc vào khu rừng cổ và phát hiện bí mật bị chôn giấu.', 119, 'C16', 'ENDED', 'Phiêu lưu, Kinh dị', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 40 DAY), '/images/movies/collection/rung-thi-tham.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW()),
  (18, 'Một Ngày Rực Rỡ', 'Một nghệ sĩ đường phố nhận ra niềm vui trong những điều giản dị quanh mình.', 103, 'P', 'ENDED', 'Hài, Tình cảm', 'Tiếng Việt', DATE_SUB(CURDATE(), INTERVAL 25 DAY), '/images/movies/collection/mot-ngay-ruc-ro.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, NOW());

INSERT INTO `combo_products` (`id`, `name`, `description`, `price`, `image_url`, `stock_quantity`, `type`, `active`, `version`, `created_at`) VALUES
  (1, 'Combo Solo', '1 bắp rang vừa và 1 nước ngọt', 69000.00, '/images/combos/combo-solo.webp', 499, 'COMBO', 1, 0, NOW()),
  (2, 'Combo Couple', '1 bắp rang lớn và 2 nước ngọt', 119000.00, '/images/combos/combo-couple.webp', 500, 'COMBO', 1, 0, NOW()),
  (3, 'Combo Family', '2 bắp rang lớn và 4 nước ngọt', 219000.00, '/images/combos/combo-family.webp', 300, 'COMBO', 1, 0, NOW()),
  (4, 'Bắp Caramel', 'Bắp caramel cỡ lớn', 59000.00, '/images/combos/bap-caramel.webp', 400, 'POPCORN', 1, 0, NOW());

INSERT INTO `cinema_events` (`id`, `title`, `label`, `description`, `image_url`, `link_url`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Thứ Tư Thành Viên', 'ƯU ĐÃI THÀNH VIÊN', 'Ưu đãi dành cho hội viên Rex và chương trình tích điểm theo booking hợp lệ.', '/images/events/member-wednesday.webp', '/movies', 1, 1, NOW()),
  (2, 'Cuối tuần xem phim', 'WEEKEND COMBO', 'Gợi ý combo dùng tại rạp cho buổi xem phim cuối tuần.', '/images/events/weekend-combo.webp', '/schedule', 2, 1, NOW());

INSERT INTO `articles` (`id`, `title`, `category`, `summary`, `content`, `image_url`, `link_url`, `publish_date`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Mẹo chọn ghế xem phim phù hợp', 'TIN ĐIỆN ẢNH', 'Một vài lưu ý về khoảng cách, góc nhìn và âm thanh khi chọn ghế.', 'Sơ đồ ghế hiển thị trạng thái theo thời gian thực. Hãy chọn vị trí phù hợp với số người và trải nghiệm mong muốn trước khi tiếp tục đặt vé.', '/images/articles/chon-ghe-xem-phim.webp', '/schedule', CURDATE(), 1, 1, NOW()),
  (2, 'Tích điểm Rex Rewards', 'REX MEMBER', 'Điểm thưởng và hạng thành viên được ghi nhận theo chính sách trong hệ thống.', 'Điểm đủ điều kiện xét hạng được cộng từ booking hợp lệ. Lịch sử ghi nhận trong ledger để người dùng theo dõi.', '/images/articles/rex-member.webp', '/rewards', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 1, NOW()),
  (3, 'Từ lịch chiếu đến ghế ngồi', 'HẬU TRƯỜNG', 'Tìm hiểu cách hệ thống đồng bộ sơ đồ ghế trong một suất chiếu.', 'Seat map được tạo theo phòng chiếu; hệ thống chỉ cho phép một bản ghi ghế trên mỗi suất chiếu.', '/images/articles/booking-realtime.webp', '/schedule', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 3, 1, NOW());

INSERT INTO `media_items` (`id`, `title`, `type`, `thumbnail_url`, `video_url`, `sort_order`, `active`, `created_at`) VALUES
  (1, 'Trailer tuyển chọn Rex', 'TRAILER', '/images/media/trailer-tuyen-chon.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 1, 1, NOW()),
  (2, 'Không gian Rex Cinemas', 'CLIP', '/images/media/trai-nghiem-rex.webp', 'https://www.youtube.com/watch?v=ttDbSrXnCH4', 2, 1, NOW());
