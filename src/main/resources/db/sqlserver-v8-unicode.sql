/*
   Convert application text to Unicode without deleting or resetting rows.
   SQL Server requires dependent indexes to be recreated around ALTER COLUMN.
*/

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_cinemas_name_address' AND object_id = OBJECT_ID('dbo.cinemas') AND is_unique_constraint = 1)
    ALTER TABLE dbo.cinemas DROP CONSTRAINT uk_cinemas_name_address;
ELSE IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_cinemas_name_address' AND object_id = OBJECT_ID('dbo.cinemas'))
    DROP INDEX uk_cinemas_name_address ON dbo.cinemas;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_movies_title_release_date' AND object_id = OBJECT_ID('dbo.movies') AND is_unique_constraint = 1)
    ALTER TABLE dbo.movies DROP CONSTRAINT uk_movies_title_release_date;
ELSE IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_movies_title_release_date' AND object_id = OBJECT_ID('dbo.movies'))
    DROP INDEX uk_movies_title_release_date ON dbo.movies;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_auditoriums_cinema_name' AND object_id = OBJECT_ID('dbo.auditoriums') AND is_unique_constraint = 1)
    ALTER TABLE dbo.auditoriums DROP CONSTRAINT uk_auditoriums_cinema_name;
ELSE IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_auditoriums_cinema_name' AND object_id = OBJECT_ID('dbo.auditoriums'))
    DROP INDEX uk_auditoriums_cinema_name ON dbo.auditoriums;

IF COL_LENGTH('dbo.app_users', 'full_name') IS NOT NULL
    ALTER TABLE dbo.app_users ALTER COLUMN full_name NVARCHAR(120) NOT NULL;
IF COL_LENGTH('dbo.app_users', 'address') IS NOT NULL
    ALTER TABLE dbo.app_users ALTER COLUMN address NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.articles', 'category') IS NOT NULL
    ALTER TABLE dbo.articles ALTER COLUMN category NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.articles', 'title') IS NOT NULL
    ALTER TABLE dbo.articles ALTER COLUMN title NVARCHAR(220) NOT NULL;
IF COL_LENGTH('dbo.articles', 'summary') IS NOT NULL
    ALTER TABLE dbo.articles ALTER COLUMN summary NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.articles', 'content') IS NOT NULL
    ALTER TABLE dbo.articles ALTER COLUMN content NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.auditoriums', 'name') IS NOT NULL
    ALTER TABLE dbo.auditoriums ALTER COLUMN name NVARCHAR(80) NOT NULL;
IF COL_LENGTH('dbo.booking_combos', 'combo_name') IS NOT NULL
    ALTER TABLE dbo.booking_combos ALTER COLUMN combo_name NVARCHAR(120) NOT NULL;
IF COL_LENGTH('dbo.cinema_events', 'label') IS NOT NULL
    ALTER TABLE dbo.cinema_events ALTER COLUMN label NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.cinema_events', 'title') IS NOT NULL
    ALTER TABLE dbo.cinema_events ALTER COLUMN title NVARCHAR(180) NOT NULL;
IF COL_LENGTH('dbo.cinema_events', 'description') IS NOT NULL
    ALTER TABLE dbo.cinema_events ALTER COLUMN description NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.cinemas', 'name') IS NOT NULL
    ALTER TABLE dbo.cinemas ALTER COLUMN name NVARCHAR(120) NOT NULL;
IF COL_LENGTH('dbo.cinemas', 'city') IS NOT NULL
    ALTER TABLE dbo.cinemas ALTER COLUMN city NVARCHAR(100) NOT NULL;
IF COL_LENGTH('dbo.cinemas', 'address') IS NOT NULL
    ALTER TABLE dbo.cinemas ALTER COLUMN address NVARCHAR(255) NOT NULL;
IF COL_LENGTH('dbo.combo_products', 'name') IS NOT NULL
    ALTER TABLE dbo.combo_products ALTER COLUMN name NVARCHAR(120) NOT NULL;
IF COL_LENGTH('dbo.combo_products', 'description') IS NOT NULL
    ALTER TABLE dbo.combo_products ALTER COLUMN description NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.movies', 'title') IS NOT NULL
    ALTER TABLE dbo.movies ALTER COLUMN title NVARCHAR(180) NOT NULL;
IF COL_LENGTH('dbo.movies', 'description') IS NOT NULL
    ALTER TABLE dbo.movies ALTER COLUMN description NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.movies', 'genre') IS NOT NULL
    ALTER TABLE dbo.movies ALTER COLUMN genre NVARCHAR(120) NULL;
IF COL_LENGTH('dbo.movies', 'language') IS NOT NULL
    ALTER TABLE dbo.movies ALTER COLUMN language NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.reviews', 'comment') IS NOT NULL
    ALTER TABLE dbo.reviews ALTER COLUMN comment NVARCHAR(1200) NOT NULL;
IF COL_LENGTH('dbo.media_items', 'title') IS NOT NULL
    ALTER TABLE dbo.media_items ALTER COLUMN title NVARCHAR(180) NOT NULL;
IF COL_LENGTH('dbo.vouchers', 'name') IS NOT NULL
    ALTER TABLE dbo.vouchers ALTER COLUMN name NVARCHAR(160) NOT NULL;

IF OBJECT_ID('dbo.cinemas', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_cinemas_name_address' AND object_id = OBJECT_ID('dbo.cinemas'))
    CREATE UNIQUE INDEX uk_cinemas_name_address ON dbo.cinemas(name, address);
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_movies_title_release_date' AND object_id = OBJECT_ID('dbo.movies'))
    CREATE UNIQUE INDEX uk_movies_title_release_date ON dbo.movies(title, release_date);
IF OBJECT_ID('dbo.auditoriums', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_auditoriums_cinema_name' AND object_id = OBJECT_ID('dbo.auditoriums'))
    CREATE UNIQUE INDEX uk_auditoriums_cinema_name ON dbo.auditoriums(cinema_id, name);

/* Restore only rows identified as bundled seed data by stable asset URLs/phones. */
UPDATE dbo.cinemas SET name = N'Rex Chain Quận 1', city = N'TP. Hồ Chí Minh', address = N'135 Nguyễn Huệ, Quận 1'
WHERE phone = N'028 3829 0001';
UPDATE dbo.cinemas SET name = N'Rex Chain Gò Vấp', city = N'TP. Hồ Chí Minh', address = N'18 Phan Văn Trị, Gò Vấp'
WHERE phone = N'028 3895 0002';
UPDATE dbo.cinemas SET name = N'Rex Chain Thủ Đức', city = N'TP. Hồ Chí Minh', address = N'210 Võ Văn Ngân, Thủ Đức'
WHERE phone = N'028 3722 0003';

UPDATE dbo.movies SET title = N'Hành Trình Ánh Sáng', description = N'Một phi hành đoàn trẻ bước vào chuyến đi vượt ngoài giới hạn của Trái Đất.', genre = N'Khoa học viễn tưởng, Hành động', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-1.svg';
UPDATE dbo.movies SET title = N'Mùa Hè Không Tên', description = N'Câu chuyện thanh xuân, tình bạn và những lựa chọn trước ngưỡng cửa trưởng thành.', genre = N'Tâm lý, Tình cảm', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-2.svg';
UPDATE dbo.movies SET title = N'Kẻ Gác Đêm', description = N'Một vụ án bí ẩn kéo người bảo vệ rạp phim vào cuộc truy tìm xuyên đêm.', genre = N'Hành động, Bí ẩn', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-3.svg';
UPDATE dbo.movies SET title = N'Nhà Có Hai Người', description = N'Bộ phim gia đình nhẹ nhàng về sự kết nối giữa hai thế hệ.', genre = N'Gia đình, Hài', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-4.svg';
UPDATE dbo.movies SET title = N'Thành Phố Sau Mưa', description = N'Một câu chuyện tình yêu và ký ức diễn ra giữa thành phố sau cơn mưa dài.', genre = N'Tình cảm, Tâm lý', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-5.svg';
UPDATE dbo.movies SET title = N'Mật Mã Đại Dương', description = N'Đội nghiên cứu biển sâu phát hiện một tín hiệu bí ẩn có thể thay đổi lịch sử.', genre = N'Phiêu lưu, Khoa học viễn tưởng', language = N'Tiếng Việt'
WHERE poster_url = '/images/poster-6.svg';

UPDATE dbo.articles SET category = N'TIN ĐIỆN ẢNH', title = N'5 mẹo chọn vị trí ghế xem phim phù hợp với từng trải nghiệm', summary = N'Khoảng cách với màn hình, góc nhìn và hệ thống âm thanh đều ảnh hưởng đến cảm giác khi xem phim.', content = N'Vị trí giữa phòng thường mang lại góc nhìn cân bằng. Nếu thích cảm giác màn hình bao trọn tầm mắt, bạn có thể chọn các hàng giữa phía trước; nếu ưu tiên sự thoải mái và quan sát tổng thể, hãy chọn các hàng giữa phía sau. Với phim hành động hoặc âm thanh mạnh, ghế gần trung tâm phòng thường là lựa chọn dễ chịu. Rex Chain hiển thị trạng thái ghế theo thời gian thực để bạn dễ chọn vị trí phù hợp trước khi xác nhận.'
WHERE image_url = '/images/article-seat.svg';
UPDATE dbo.articles SET category = N'REX MEMBER', title = N'Tích điểm Rex Member hoạt động như thế nào?', summary = N'Booking thành công giúp bạn tích điểm và nâng hạng thành viên theo tổng điểm đã nhận.', content = N'Mỗi 10.000 đồng trên booking thành công tương đương 1 điểm Rex. Điểm được cộng sau khi đơn được xác nhận thanh toán. Hệ thống có bốn hạng MEMBER, SILVER, GOLD và DIAMOND. Hạng thành viên được tính theo tổng điểm tích lũy trọn đời, giúp bạn dễ theo dõi tiến độ ngay trên trang hồ sơ.'
WHERE image_url = '/images/article-member.svg';
UPDATE dbo.articles SET category = N'HẬU TRƯỜNG', title = N'Từ lịch chiếu đến ghế ngồi: một booking được xử lý ra sao?', summary = N'WebSocket giữ ghế realtime giúp hạn chế việc hai người cùng đặt một vị trí.', content = N'Khi khách chọn ghế, hệ thống giữ ghế tạm thời và phát trạng thái mới qua WebSocket đến các trình duyệt đang xem cùng suất chiếu. Nếu booking thành công, ghế chuyển sang đã bán. Nếu hết thời gian giữ hoặc thanh toán online hết hạn, ghế được trả về trạng thái khả dụng.'
WHERE image_url = '/images/article-realtime.svg';

UPDATE dbo.cinema_events SET label = N'ƯU ĐÃI THÀNH VIÊN', title = N'Thứ Tư Thành Viên – Giá vé nhẹ hơn, trải nghiệm vẫn trọn vẹn', description = N'Đăng nhập thành viên Rex Chain để nhận ưu đãi chọn lọc và tích điểm trên mỗi booking thành công.'
WHERE image_url = '/images/event-member.svg';
UPDATE dbo.cinema_events SET label = N'WEEKEND COMBO', title = N'Cuối tuần xem phim – Combo đôi giá tốt', description = N'Chọn Combo Couple trong bước đặt vé và thưởng thức trọn vẹn buổi chiếu cuối tuần.'
WHERE image_url = '/images/event-combo.svg';
