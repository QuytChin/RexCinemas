package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.MovieStatus;
import com.rexchain.cinema.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiMovieConsultantService {

    private final MovieRepository movies;

    public AiMovieConsultantService(MovieRepository movies) {
        this.movies = movies;
    }

    public String consult(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Dạ chào bạn! Em là **Trợ lý AI RexCinemas** 🤖. Bạn muốn tìm phim thuộc thể loại nào (hành động, kinh dị, hoạt hình, tình cảm...) hay xem phim theo độ tuổi nào để em gợi ý nhé!";
        }

        String normalized = removeAccents(prompt.toLowerCase(Locale.ROOT));
        List<Movie> allActiveMovies = movies.findAll().stream()
                .filter(Movie::isActive)
                .toList();

        // 1. Kiểm tra câu hỏi về giá vé / bắp nước / cách đặt vé
        if (normalized.contains("gia ve") || normalized.contains("bao nhieu tien") || normalized.contains("bang gia")) {
            return """
                🎟️ **Bảng giá vé tham khảo tại RexCinemas:**
                - **Ghế thường (Standard):** Từ 75.000đ - 95.000đ tùy khung giờ & ngày chiếu.
                - **Ghế VIP (Vị trí đẹp trung tâm):** Phụ thu +10.000đ - 15.000đ.
                - **Ghế đôi (Sweetbox/Couple):** Từ 160.000đ - 210.000đ / cặp.
                
                💡 *Mẹo:* Thành viên **Rex Member** được tích điểm đổi vé miễn phí và giảm giá bắp nước! Bạn có thể chọn suất chiếu tại mục [LỊCH CHIẾU](/schedule) để xem giá chi tiết của từng ghế nhé!
                """;
        }

        if (normalized.contains("bap nuoc") || normalized.contains("combo") || normalized.contains("popcorn")) {
            return """
                🍿 **Combo Bắp Nước Siêu Hời tại RexCinemas:**
                - **Solo Combo:** 1 Bắp ngọt lớn + 1 Nước ngọt mát lạnh (65.000đ).
                - **Couple Combo:** 1 Bắp lớn 2 vị (Phô mai/Caramel) + 2 Nước ngọt (95.000đ).
                - **Family Combo:** 2 Bắp lớn + 3 Nước ngọt + 1 Snack (145.000đ).
                
                👉 Bạn có thể chọn thêm combo bắp nước ngay trong bước chọn ghế đặt vé online đấy!
                """;
        }

        if (normalized.contains("doi ve") || normalized.contains("huy ve") || normalized.contains("hoan tien") || normalized.contains("gap nhan vien")) {
            return """
                👨‍💼 **Hỗ trợ Đổi/Hủy vé & Chăm sóc khách hàng:**
                Đối với các yêu cầu can thiệp vé, kiểm tra phòng chiếu hoặc sự cố thanh toán, bạn vui lòng chuyển sang **Tab "Chat với Rạp"** bên cạnh để nhắn tin trực tiếp với Ban Quản lý rạp gần nhất nhé! Đội ngũ nhân viên rạp sẽ phản hồi bạn ngay.
                """;
        }

        // 2. Kiểm tra xem có nhắc đích danh tên phim cụ thể không
        for (Movie m : allActiveMovies) {
            String movieNorm = removeAccents(m.getTitle().toLowerCase(Locale.ROOT));
            if (normalized.contains(movieNorm) || movieNorm.contains(normalized)) {
                return buildMovieDetailResponse(m);
            }
        }

        // 3. Phân tích tiêu chí: Thể loại, Độ tuổi, Trạng thái chiếu
        List<Movie> matched = new ArrayList<>(allActiveMovies);

        // Lọc theo trạng thái
        if (normalized.contains("sap chieu") || normalized.contains("coming soon")) {
            matched = matched.stream().filter(m -> m.getStatus() == MovieStatus.COMING_SOON).toList();
        } else if (normalized.contains("dang chieu") || normalized.contains("hom nay") || normalized.contains("ngay bay gio")) {
            matched = matched.stream().filter(m -> m.getStatus() == MovieStatus.NOW_SHOWING).toList();
        }

        // Lọc theo độ tuổi
        if (normalized.contains("tre em") || normalized.contains("gia dinh") || normalized.contains("p") || normalized.contains("moi lua tuoi")) {
            matched = matched.stream().filter(m -> m.getAgeRating() != null && (m.getAgeRating().name().equals("P") || m.getAgeRating().name().equals("K"))).toList();
        } else if (normalized.contains("18+") || normalized.contains("t18") || normalized.contains("nguoi lon")) {
            matched = matched.stream().filter(m -> m.getAgeRating() != null && m.getAgeRating().name().equals("T18")).toList();
        } else if (normalized.contains("16+") || normalized.contains("t16")) {
            matched = matched.stream().filter(m -> m.getAgeRating() != null && (m.getAgeRating().name().equals("T16") || m.getAgeRating().name().equals("T13"))).toList();
        }

        // Lọc / Xếp hạng theo thể loại
        String detectedGenre = detectGenre(normalized);
        if (detectedGenre != null) {
            String genNorm = removeAccents(detectedGenre);
            List<Movie> genreMatched = matched.stream()
                    .filter(m -> m.getGenre() != null && removeAccents(m.getGenre().toLowerCase(Locale.ROOT)).contains(genNorm))
                    .toList();
            if (!genreMatched.isEmpty()) {
                matched = genreMatched;
            }
        }

        if (matched.isEmpty()) {
            matched = allActiveMovies.stream()
                    .filter(m -> m.getStatus() == MovieStatus.NOW_SHOWING)
                    .limit(3)
                    .toList();
        }

        // Giới hạn gợi ý tối đa 3 phim nổi bật nhất
        List<Movie> topMovies = matched.stream().limit(3).toList();
        StringBuilder sb = new StringBuilder();
        if (detectedGenre != null) {
            sb.append("🎬 **Rex AI gợi ý cho bạn các phim thể loại *").append(detectedGenre).append("* cực hấp dẫn:**\n\n");
        } else {
            sb.append("🌟 **Dưới đây là những bộ phim đang được yêu thích nhất tại cụm rạp RexCinemas:**\n\n");
        }

        for (int i = 0; i < topMovies.size(); i++) {
            Movie m = topMovies.get(i);
            sb.append(i + 1).append(". **").append(m.getTitle()).append("**\n")
              .append("   - 🏷️ **Thể loại:** ").append(m.getGenre() != null ? m.getGenre() : "Đang cập nhật").append("\n")
              .append("   - ⏳ **Thời lượng:** ").append(m.getDurationMinutes()).append(" phút")
              .append(" | 🔞 **Độ tuổi:** ").append(m.getAgeRating() != null ? m.getAgeRating().name() : "P").append("\n")
              .append("   - 📖 ").append(shortSummary(m.getDescription())).append("\n")
              .append("   - 👉 [Xem chi tiết & Đặt vé](/movies/").append(m.getId()).append(")\n\n");
        }

        sb.append("💡 *Bạn muốn tìm thêm suất chiếu theo cụm rạp nào? Hãy bấm sang Tab 'Chat với Rạp' hoặc tra cứu tại [LỊCH CHIẾU TOÀN HỆ THỐNG](/schedule) nhé!*");
        return sb.toString();
    }

    private String detectGenre(String text) {
        if (text.contains("kinh di") || text.contains("ma") || text.contains("so hai")) return "Kinh dị";
        if (text.contains("hanh dong") || text.contains("ban sung") || text.contains("danh nhau")) return "Hành động";
        if (text.contains("tinh cam") || text.contains("lang man") || text.contains("yeu")) return "Tình cảm";
        if (text.contains("hai") || text.contains("hai huoc") || text.contains("vui ve")) return "Hài";
        if (text.contains("hoat hinh") || text.contains("anime") || text.contains("cartoon")) return "Hoạt hình";
        if (text.contains("phieu luu") || text.contains("kham pha")) return "Phiêu lưu";
        if (text.contains("vien tuong") || text.contains("khoa hoc")) return "Khoa học viễn tưởng";
        if (text.contains("tam ly") || text.contains("giat gan")) return "Tâm lý";
        return null;
    }

    private String buildMovieDetailResponse(Movie m) {
        return """
            🎬 **%s**
            - 🏷️ **Thể loại:** %s
            - ⏳ **Thời lượng:** %d phút | 🔞 **Độ tuổi:** %s
            - 📅 **Khởi chiếu:** %s (%s)
            - 📖 **Nội dung:** %s
            
            👉 [BẤM VÀO ĐÂY ĐỂ ĐẶT VÉ XEM PHIM NÀY](/movies/%d)
            """.formatted(
                m.getTitle(),
                m.getGenre() != null ? m.getGenre() : "Đang cập nhật",
                m.getDurationMinutes(),
                m.getAgeRating() != null ? m.getAgeRating().name() : "P",
                m.getReleaseDate() != null ? m.getReleaseDate().toString() : "Đang chiếu",
                m.getStatus() == MovieStatus.NOW_SHOWING ? "Đang chiếu" : "Sắp chiếu",
                m.getDescription() != null ? m.getDescription() : "Chưa có tóm tắt",
                m.getId()
        );
    }

    private String shortSummary(String desc) {
        if (desc == null || desc.isBlank()) return "Trải nghiệm điện ảnh đỉnh cao tại phòng chiếu RexCinemas.";
        if (desc.length() <= 120) return desc;
        return desc.substring(0, 117) + "...";
    }

    private static String removeAccents(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}
