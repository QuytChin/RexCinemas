-- Replace legacy SVG demo-art references with the generated project assets.
-- Every statement is idempotent and preserves user-uploaded/custom image URLs.
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.movies SET poster_url = '/images/movies/hanh-trinh-anh-sang.webp' WHERE poster_url = '/images/poster-1.svg';
    UPDATE dbo.movies SET poster_url = '/images/movies/mua-he-khong-ten.webp' WHERE poster_url = '/images/poster-2.svg';
    UPDATE dbo.movies SET poster_url = '/images/movies/ke-gac-dem.webp' WHERE poster_url = '/images/poster-3.svg';
    UPDATE dbo.movies SET poster_url = '/images/movies/nha-co-hai-nguoi.webp' WHERE poster_url = '/images/poster-4.svg';
    UPDATE dbo.movies SET poster_url = '/images/movies/thanh-pho-sau-mua.webp' WHERE poster_url = '/images/poster-5.svg';
    UPDATE dbo.movies SET poster_url = '/images/movies/mat-ma-dai-duong.webp' WHERE poster_url = '/images/poster-6.svg';
END;

IF OBJECT_ID('dbo.cinema_events', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.cinema_events SET image_url = '/images/events/member-wednesday.webp' WHERE image_url = '/images/event-member.svg';
    UPDATE dbo.cinema_events SET image_url = '/images/events/weekend-combo.webp' WHERE image_url = '/images/event-combo.svg';
END;

IF OBJECT_ID('dbo.articles', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.articles SET image_url = '/images/articles/chon-ghe-xem-phim.webp' WHERE image_url = '/images/article-seat.svg';
    UPDATE dbo.articles SET image_url = '/images/articles/rex-member.webp' WHERE image_url = '/images/article-member.svg';
    UPDATE dbo.articles SET image_url = '/images/articles/booking-realtime.webp' WHERE image_url = '/images/article-realtime.svg';
END;

IF OBJECT_ID('dbo.media_items', 'U') IS NOT NULL
BEGIN
    UPDATE dbo.media_items SET thumbnail_url = '/images/media/trailer-tuyen-chon.webp' WHERE thumbnail_url = '/images/media-trailer.svg';
    UPDATE dbo.media_items SET thumbnail_url = '/images/media/trai-nghiem-rex.webp' WHERE thumbnail_url = '/images/media-clip.svg';
END;

IF OBJECT_ID('dbo.combo_products', 'U') IS NOT NULL AND COL_LENGTH('dbo.combo_products', 'image_url') IS NOT NULL
BEGIN
    UPDATE dbo.combo_products SET image_url = '/images/combos/combo-solo.webp' WHERE name = N'Combo Solo' AND (image_url IS NULL OR image_url = '');
    UPDATE dbo.combo_products SET image_url = '/images/combos/combo-couple.webp' WHERE name = N'Combo Couple' AND (image_url IS NULL OR image_url = '');
    UPDATE dbo.combo_products SET image_url = '/images/combos/combo-family.webp' WHERE name = N'Combo Family' AND (image_url IS NULL OR image_url = '');
    UPDATE dbo.combo_products SET image_url = '/images/combos/bap-caramel.webp' WHERE name LIKE N'%Caramel%' AND (image_url IS NULL OR image_url = '');
END;
