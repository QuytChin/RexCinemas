package com.rexchain.cinema.config;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.CinemaSetupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository users, MovieRepository movies, CinemaRepository cinemas,
                           AuditoriumRepository auditoriums, ShowtimeRepository showtimes,
                           PasswordEncoder passwordEncoder, CinemaSetupService setup,
                           ComboProductRepository combos, VoucherRepository vouchers,
                           EventRepository events, ArticleRepository articles, MediaItemRepository mediaItems,
                           SeatRepository seats, ShowtimeSeatRepository showtimeSeats) {
        return args -> {
            seedUsers(users, passwordEncoder);
            ensureDemoMembership(users);
            seedMovies(movies);
            seedCinemas(cinemas, setup);
            seedShowtimes(movies, auditoriums, showtimes, setup);
            upgradeLegacySeatTypes(auditoriums, seats, showtimeSeats);
            seedCombos(combos);
            seedVouchers(vouchers);
            seedHomeContent(events, articles, mediaItems);
        };
    }

    private void seedUsers(UserRepository users, PasswordEncoder encoder) {
        if (users.count() != 0) return;
        User admin = new User();
        admin.setFullName("Rex Administrator");
        admin.setEmail("admin@rexchain.vn");
        admin.setPassword(encoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        users.save(admin);

        User customer = new User();
        customer.setFullName("Khách hàng Demo");
        customer.setEmail("user@rexchain.vn");
        customer.setPassword(encoder.encode("123456"));
        customer.setLoyaltyPoints(168);
        customer.setLifetimePoints(168);
        users.save(customer);

        User manager = new User();
        manager.setFullName("Cinema Manager Demo");
        manager.setEmail("manager@rexchain.vn");
        manager.setPassword(encoder.encode("manager123"));
        manager.setRole(Role.CINEMA_MANAGER);
        users.save(manager);
    }

    private void ensureDemoMembership(UserRepository users) {
        users.findByEmailIgnoreCase("user@rexchain.vn").ifPresent(user -> {
            if (user.getLoyaltyPoints() == 0 && user.getLifetimePoints() == 0) {
                user.setLoyaltyPoints(168);
                user.setLifetimePoints(168);
                users.save(user);
            }
        });
    }

    private void upgradeLegacySeatTypes(AuditoriumRepository auditoriums, SeatRepository seats,
                                        ShowtimeSeatRepository showtimeSeats) {
        boolean alreadyUpgraded = seats.findAll().stream().anyMatch(seat -> seat.getType() == SeatType.COUPLE);
        if (alreadyUpgraded) return;

        for (Auditorium auditorium : auditoriums.findAll()) {
            int rows = auditorium.getTotalRows();
            for (Seat seat : seats.findByAuditoriumIdOrderBySeatRowAscSeatNumberAsc(auditorium.getId())) {
                int rowIndex = Math.max(0, seat.getSeatRow().charAt(0) - 'A');
                if (rowIndex == rows - 1) seat.setType(SeatType.COUPLE);
                else if (rowIndex >= Math.max(0, rows - 3)) seat.setType(SeatType.VIP);
                else seat.setType(SeatType.STANDARD);
                seats.save(seat);
            }
        }

        for (ShowtimeSeat showtimeSeat : showtimeSeats.findAllWithShowtimeAndSeat()) {
            BigDecimal price = showtimeSeat.getShowtime().getBasePrice();
            if (showtimeSeat.getSeat().getType() == SeatType.VIP) price = price.add(new BigDecimal("20000"));
            if (showtimeSeat.getSeat().getType() == SeatType.COUPLE) price = price.add(new BigDecimal("40000"));
            showtimeSeat.setPrice(price);
            showtimeSeats.save(showtimeSeat);
        }
    }

    private void seedMovies(MovieRepository movies) {
        addMovieIfMissing(movies, "Hành Trình Ánh Sáng", "Một phi hành đoàn trẻ bước vào chuyến đi vượt ngoài giới hạn của Trái Đất.",
                125, "T13", "Khoa học viễn tưởng, Hành động", "Tiếng Việt", LocalDate.now().minusDays(14), "/images/poster-1.svg", "https://www.youtube.com/");
        addMovieIfMissing(movies, "Mùa Hè Không Tên", "Câu chuyện thanh xuân, tình bạn và những lựa chọn trước ngưỡng cửa trưởng thành.",
                112, "P", "Tâm lý, Tình cảm", "Tiếng Việt", LocalDate.now().minusDays(8), "/images/poster-2.svg", "https://www.youtube.com/");
        addMovieIfMissing(movies, "Kẻ Gác Đêm", "Một vụ án bí ẩn kéo người bảo vệ rạp phim vào cuộc truy tìm xuyên đêm.",
                118, "T16", "Hành động, Bí ẩn", "Tiếng Việt", LocalDate.now().minusDays(4), "/images/poster-3.svg", "https://www.youtube.com/");
        addMovieIfMissing(movies, "Nhà Có Hai Người", "Bộ phim gia đình nhẹ nhàng về sự kết nối giữa hai thế hệ.",
                105, "P", "Gia đình, Hài", "Tiếng Việt", LocalDate.now().minusDays(2), "/images/poster-4.svg", "https://www.youtube.com/");
        addMovieIfMissing(movies, "Thành Phố Sau Mưa", "Một câu chuyện tình yêu và ký ức diễn ra giữa thành phố sau cơn mưa dài.",
                110, "T13", "Tình cảm, Tâm lý", "Tiếng Việt", LocalDate.now().plusDays(10), "/images/poster-5.svg", "https://www.youtube.com/");
        addMovieIfMissing(movies, "Mật Mã Đại Dương", "Đội nghiên cứu biển sâu phát hiện một tín hiệu bí ẩn có thể thay đổi lịch sử.",
                132, "T13", "Phiêu lưu, Khoa học viễn tưởng", "Tiếng Việt", LocalDate.now().plusDays(24), "/images/poster-6.svg", "https://www.youtube.com/");
    }

    private void addMovieIfMissing(MovieRepository movies, String title, String description, int duration,
                                   String age, String genre, String language, LocalDate release,
                                   String poster, String trailer) {
        if (movies.findByTitleIgnoreCase(title).isPresent()) return;
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationMinutes(duration);
        movie.setAgeRating(age);
        movie.setGenre(genre);
        movie.setLanguage(language);
        movie.setReleaseDate(release);
        movie.setPosterUrl(poster);
        movie.setTrailerUrl(trailer);
        movies.save(movie);
    }

    private void seedCinemas(CinemaRepository cinemas, CinemaSetupService setup) {
        if (cinemas.count() != 0) return;
        Cinema c1 = new Cinema();
        c1.setName("Rex Chain Quận 1");
        c1.setCity("TP. Hồ Chí Minh");
        c1.setAddress("135 Nguyễn Huệ, Quận 1");
        c1.setPhone("028 3829 0001");
        c1 = cinemas.save(c1);

        Cinema c2 = new Cinema();
        c2.setName("Rex Chain Gò Vấp");
        c2.setCity("TP. Hồ Chí Minh");
        c2.setAddress("18 Phan Văn Trị, Gò Vấp");
        c2.setPhone("028 3895 0002");
        c2 = cinemas.save(c2);

        Cinema c3 = new Cinema();
        c3.setName("Rex Chain Thủ Đức");
        c3.setCity("TP. Hồ Chí Minh");
        c3.setAddress("210 Võ Văn Ngân, Thủ Đức");
        c3.setPhone("028 3722 0003");
        c3 = cinemas.save(c3);

        setup.createAuditorium(c1, "Phòng 01", 6, 10);
        setup.createAuditorium(c1, "Phòng 02", 7, 10);
        setup.createAuditorium(c2, "Phòng 01", 6, 8);
        setup.createAuditorium(c2, "Phòng 02", 6, 10);
        setup.createAuditorium(c3, "Phòng 01", 7, 10);
    }

    private void seedShowtimes(MovieRepository movies, AuditoriumRepository auditoriums,
                               ShowtimeRepository showtimes, CinemaSetupService setup) {
        LocalDateTime now = LocalDateTime.now();
        // Existing local databases can outlive their initial 7-day demo schedule.
        // Only seed a fresh rolling schedule when there is no active future showtime in the next week.
        if (showtimes.countByActiveTrueAndStartTimeBetween(now, now.plusDays(8)) > 0) return;
        List<Movie> activeMovies = movies.findByActiveTrueAndReleaseDateLessThanEqualOrderByReleaseDateDesc(LocalDate.now());
        List<Auditorium> rooms = auditoriums.findAll();
        if (rooms.isEmpty()) return;
        int index = 0;
        int[] hours = {10, 13, 16, 19, 21};
        for (int day = 0; day < 7; day++) {
            for (Movie movie : activeMovies) {
                for (int slot = 0; slot < 2; slot++) {
                    Showtime showtime = new Showtime();
                    showtime.setMovie(movie);
                    showtime.setAuditorium(rooms.get((index++) % rooms.size()));
                    LocalDateTime candidate = LocalDate.now().plusDays(day)
                            .atTime(hours[(index + slot) % hours.length], slot == 0 ? 0 : 30);
                    if (!candidate.isAfter(now.plusMinutes(20))) continue;
                    showtime.setStartTime(candidate);
                    showtime.setBasePrice(new BigDecimal((slot == 0 ? "75000" : "90000")));
                    showtime = showtimes.save(showtime);
                    setup.createShowtimeSeats(showtime);
                }
            }
        }
    }

    private void seedCombos(ComboProductRepository combos) {
        if (combos.count() != 0) return;
        String[][] data = {
                {"Combo Solo", "1 bắp rang vừa + 1 nước ngọt", "69000"},
                {"Combo Couple", "1 bắp rang lớn + 2 nước ngọt", "119000"},
                {"Combo Family", "2 bắp rang lớn + 4 nước ngọt", "219000"},
                {"Bắp Caramel", "1 bắp caramel size lớn", "59000"}
        };
        for (String[] row : data) {
            ComboProduct combo = new ComboProduct();
            combo.setName(row[0]);
            combo.setDescription(row[1]);
            combo.setPrice(new BigDecimal(row[2]));
            combos.save(combo);
        }
    }

    private void seedVouchers(VoucherRepository vouchers) {
        if (vouchers.count() != 0) return;
        Voucher voucher = new Voucher();
        voucher.setCode("REX10");
        voucher.setName("Giảm 10% thành viên Rex");
        voucher.setDiscountType(DiscountType.PERCENT);
        voucher.setDiscountValue(new BigDecimal("10"));
        voucher.setMinOrderAmount(new BigDecimal("100000"));
        voucher.setMaxDiscountAmount(new BigDecimal("50000"));
        voucher.setValidFrom(LocalDateTime.now().minusDays(1));
        voucher.setValidUntil(LocalDateTime.now().plusMonths(6));
        voucher.setUsageLimit(500);
        vouchers.save(voucher);
    }

    private void seedHomeContent(EventRepository events, ArticleRepository articles, MediaItemRepository mediaItems) {
        if (events.count() == 0) {
            Event e1 = new Event();
            e1.setLabel("ƯU ĐÃI THÀNH VIÊN");
            e1.setTitle("Thứ Tư Thành Viên – Giá vé nhẹ hơn, trải nghiệm vẫn trọn vẹn");
            e1.setDescription("Đăng nhập thành viên Rex Chain để nhận ưu đãi chọn lọc và tích điểm trên mỗi booking thành công.");
            e1.setImageUrl("/images/event-member.svg");
            e1.setSortOrder(1);
            events.save(e1);

            Event e2 = new Event();
            e2.setLabel("WEEKEND COMBO");
            e2.setTitle("Cuối tuần xem phim – Combo đôi giá tốt");
            e2.setDescription("Chọn Combo Couple trong bước đặt vé và thưởng thức trọn vẹn buổi chiếu cuối tuần.");
            e2.setImageUrl("/images/event-combo.svg");
            e2.setSortOrder(2);
            events.save(e2);
        }

        if (articles.count() == 0) {
            Article a1 = new Article();
            a1.setCategory("TIN ĐIỆN ẢNH");
            a1.setTitle("5 mẹo chọn vị trí ghế xem phim phù hợp với từng trải nghiệm");
            a1.setSummary("Khoảng cách với màn hình, góc nhìn và hệ thống âm thanh đều ảnh hưởng đến cảm giác khi xem phim.");
            a1.setContent("Vị trí giữa phòng thường mang lại góc nhìn cân bằng. Nếu thích cảm giác màn hình bao trọn tầm mắt, bạn có thể chọn các hàng giữa phía trước; nếu ưu tiên sự thoải mái và quan sát tổng thể, hãy chọn các hàng giữa phía sau. Với phim hành động hoặc âm thanh mạnh, ghế gần trung tâm phòng thường là lựa chọn dễ chịu. Rex Chain hiển thị trạng thái ghế theo thời gian thực để bạn dễ chọn vị trí phù hợp trước khi xác nhận.");
            a1.setImageUrl("/images/article-seat.svg");
            a1.setPublishDate(LocalDate.now());
            a1.setSortOrder(1);
            articles.save(a1);

            Article a2 = new Article();
            a2.setCategory("REX MEMBER");
            a2.setTitle("Tích điểm Rex Member hoạt động như thế nào?");
            a2.setSummary("Booking thành công giúp bạn tích điểm và nâng hạng thành viên theo tổng điểm đã nhận.");
            a2.setContent("Mỗi 10.000 đồng trên booking thành công tương đương 1 điểm Rex. Điểm được cộng sau khi đơn được xác nhận thanh toán. Hệ thống có bốn hạng MEMBER, SILVER, GOLD và DIAMOND. Hạng thành viên được tính theo tổng điểm tích lũy trọn đời, giúp bạn dễ theo dõi tiến độ ngay trên trang hồ sơ.");
            a2.setImageUrl("/images/article-member.svg");
            a2.setPublishDate(LocalDate.now().minusDays(2));
            a2.setSortOrder(2);
            articles.save(a2);

            Article a3 = new Article();
            a3.setCategory("HẬU TRƯỜNG");
            a3.setTitle("Từ lịch chiếu đến ghế ngồi: một booking được xử lý ra sao?");
            a3.setSummary("WebSocket giữ ghế realtime giúp hạn chế việc hai người cùng đặt một vị trí.");
            a3.setContent("Khi khách chọn ghế, hệ thống giữ ghế tạm thời và phát trạng thái mới qua WebSocket đến các trình duyệt đang xem cùng suất chiếu. Nếu booking thành công, ghế chuyển sang đã bán. Nếu hết thời gian giữ hoặc thanh toán online hết hạn, ghế được trả về trạng thái khả dụng.");
            a3.setImageUrl("/images/article-realtime.svg");
            a3.setPublishDate(LocalDate.now().minusDays(4));
            a3.setSortOrder(3);
            articles.save(a3);
        }

        if (mediaItems.count() == 0) {
            MediaItem m1 = new MediaItem();
            m1.setTitle("Trailer tuyển chọn tuần này");
            m1.setType(MediaType.TRAILER);
            m1.setThumbnailUrl("/images/media-trailer.svg");
            m1.setVideoUrl("https://www.youtube.com/");
            m1.setSortOrder(1);
            mediaItems.save(m1);

            MediaItem m2 = new MediaItem();
            m2.setTitle("Clip trải nghiệm Rex Chain");
            m2.setType(MediaType.CLIP);
            m2.setThumbnailUrl("/images/media-clip.svg");
            m2.setVideoUrl("https://www.youtube.com/");
            m2.setSortOrder(2);
            mediaItems.save(m2);
        }
    }
}
