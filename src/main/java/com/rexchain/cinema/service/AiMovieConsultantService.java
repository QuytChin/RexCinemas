package com.rexchain.cinema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.MovieStatus;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiMovieConsultantService {

    private static final Logger log = LoggerFactory.getLogger(AiMovieConsultantService.class);

    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.grok.enabled:true}")
    private boolean grokEnabled = true;

    @Value("${app.grok.api-key:}")
    private String grokApiKey = "";

    @Value("${app.grok.api-url:https://api.x.ai/v1/chat/completions}")
    private String grokApiUrl = "https://api.x.ai/v1/chat/completions";

    @Value("${app.grok.model:grok-2-latest}")
    private String grokModel = "grok-2-latest";

    @Value("${app.grok.timeout-seconds:15}")
    private int grokTimeoutSeconds = 15;

    @Value("${app.grok.temperature:0.7}")
    private double grokTemperature = 0.7;

    @Value("${app.grok.max-tokens:800}")
    private int grokMaxTokens = 800;

    @Autowired
    public AiMovieConsultantService(MovieRepository movies,
                                    CinemaRepository cinemas,
                                    ObjectMapper objectMapper) {
        this(movies, cinemas, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build());
    }

    AiMovieConsultantService(MovieRepository movies,
                             CinemaRepository cinemas,
                             ObjectMapper objectMapper,
                             HttpClient httpClient) {
        this.movies = movies;
        this.cinemas = cinemas;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public boolean isGrokConfigured() {
        return grokEnabled && grokApiKey != null && !grokApiKey.trim().isBlank();
    }

    public void setGrokConfig(boolean enabled, String apiKey, String model) {
        this.grokEnabled = enabled;
        this.grokApiKey = apiKey != null ? apiKey.trim() : "";
        if (model != null && !model.isBlank()) {
            this.grokModel = model;
        }
    }

    public String consult(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Dạ chào bạn! Em là **Trợ lý Rex AI** 🤖. Bạn muốn tìm phim thuộc thể loại nào (hành động, kinh dị, hoạt hình, tình cảm...) hay xem phim theo độ tuổi nào để em gợi ý nhé!";
        }

        List<Movie> allActiveMovies = movies.findAll().stream()
                .filter(Movie::isActive)
                .toList();

        // 1. Nếu Grok API đã được cấu hình API Key, ưu tiên gọi Grok AI
        if (isGrokConfigured()) {
            try {
                String grokAnswer = callGrok(prompt.trim(), allActiveMovies);
                if (grokAnswer != null && !grokAnswer.isBlank()) {
                    return grokAnswer;
                }
            } catch (Exception ex) {
                log.warn("Không thể kết nối đến Grok API (xAI). Tự động chuyển sang bộ tư vấn mặc định. Lý do: {}", ex.getMessage());
            }
        }

        // 2. Fallback: Bộ tư vấn nội bộ bằng Rule-Based Heuristics
        return consultRuleBased(prompt, allActiveMovies);
    }

    /**
     * Gửi yêu cầu tư vấn đến xAI Grok API với Grounding dữ liệu thời gian thực từ database RexCinemas.
     */
    private String callGrok(String userPrompt, List<Movie> activeMovies) throws Exception {
        List<Cinema> activeCinemas = cinemas.findByActiveTrueOrderByCityAscNameAsc();
        String systemPrompt = buildSystemPrompt(activeMovies, activeCinemas);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", grokModel);
        payload.put("temperature", grokTemperature);
        payload.put("max_tokens", grokMaxTokens);

        ArrayNode messagesArray = payload.putArray("messages");

        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        ObjectNode userMsg = messagesArray.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        String jsonBody = objectMapper.writeValueAsString(payload);

        int timeoutSec = grokTimeoutSeconds > 0 ? grokTimeoutSeconds : 15;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(grokApiUrl))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + grokApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode messageNode = choices.get(0).path("message");
                String content = messageNode.path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }
            throw new IllegalStateException("Phản hồi từ Grok API không có nội dung hợp lệ: " + response.body());
        } else {
            throw new IllegalStateException("Grok API trả về mã lỗi HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Tạo System Prompt ngữ cảnh (Domain Grounding) cho Grok để trả lời chính xác thông tin RexCinemas.
     */
    private String buildSystemPrompt(List<Movie> activeMovies, List<Cinema> activeCinemas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là \"Trợ lý Rex AI\" 🤖 - chuyên viên tư vấn điện ảnh ảo thông minh, thân thiện, am hiểu của cụm rạp RexCinemas (Việt Nam).\n\n");

        sb.append("=== THÔNG TIN HỆ THỐNG REXCINEMAS ===\n");

        sb.append("1. CÁC CỤM RẠP ĐANG HOẠT ĐỘNG:\n");
        if (activeCinemas.isEmpty()) {
            sb.append("- RexCinemas hiện có mặt tại TP.HCM và các thành phố lớn.\n");
        } else {
            for (Cinema c : activeCinemas) {
                sb.append("- ").append(c.getName()).append(" (")
                  .append(c.getAddress() != null ? c.getAddress() : "")
                  .append(", ").append(c.getCity() != null ? c.getCity() : "").append(")\n");
            }
        }

        sb.append("\n2. BẢNG GIÁ VÉ THAM KHẢO:\n");
        sb.append("- Ghế Standard (Thường): 75.000đ - 95.000đ tùy khung giờ & ngày chiếu.\n");
        sb.append("- Ghế VIP (Vị trí trung tâm đẹp nhất): Phụ thu +10.000đ - 15.000đ.\n");
        sb.append("- Ghế Sweetbox (Ghế đôi lãng mạn): 160.000đ - 210.000đ / cặp.\n");
        sb.append("- Hội viên Rex Member: Tích điểm đổi vé & ưu đãi giá bắp nước.\n");

        sb.append("\n3. COMBO BẮP NƯỚC ƯU ĐÃI:\n");
        sb.append("- Solo Combo: 1 Bắp ngọt lớn + 1 Nước ngọt (65.000đ)\n");
        sb.append("- Couple Combo: 1 Bắp lớn 2 vị + 2 Nước ngọt (95.000đ)\n");
        sb.append("- Family Combo: 2 Bắp lớn + 3 Nước ngọt + 1 Snack (145.000đ)\n");

        sb.append("\n4. DANH SÁCH PHIM ĐANG CHIẾU & SẮP CHIẾU TẠI RẠP (Dữ liệu thực tế):\n");
        if (activeMovies.isEmpty()) {
            sb.append("(Hiện tại rạp đang cập nhật danh sách phim mới)\n");
        } else {
            for (Movie m : activeMovies) {
                sb.append("- [ID: ").append(m.getId()).append("] ")
                  .append(m.getTitle())
                  .append(" | Thể loại: ").append(m.getGenre() != null ? m.getGenre() : "Đang cập nhật")
                  .append(" | Thời lượng: ").append(m.getDurationMinutes()).append(" phút")
                  .append(" | Độ tuổi: ").append(m.getAgeRating() != null ? m.getAgeRating().name() : "P")
                  .append(" | Trạng thái: ").append(m.getStatus() == MovieStatus.NOW_SHOWING ? "Đang chiếu" : "Sắp chiếu")
                  .append(" | Tóm tắt: ").append(shortSummary(m.getDescription()))
                  .append(" | Link đặt vé: /movies/").append(m.getId()).append("\n");
            }
        }

        sb.append("\n=== NGUYÊN TẮC PHẢN HỒI (RẤT QUAN TRỌNG) ===\n");
        sb.append("1. Luôn trả lời bằng tiếng Việt lịch sự, trẻ trung, nhiệt tình, có chèn emojis (🎬, 🍿, 🎟️, ⭐, 🤖) phù hợp.\n");
        sb.append("2. Sử dụng định dạng Markdown rõ ràng: in đậm tên phim, giá tiền, gạch đầu dòng các ý chính.\n");
        sb.append("3. Khi giới thiệu hay nhắc đến phim trong danh mục, BẮT BUỘC chèn liên kết Markdown theo đúng mẫu: [Xem chi tiết & Đặt vé](/movies/{id}).\n");
        sb.append("4. Khi khách hỏi về lịch chiếu hoặc đặt vé chung, hướng dẫn truy cập: [LỊCH CHIẾU](/schedule).\n");
        sb.append("5. TUYỆT ĐỐI KHÔNG bịa đặt phim không có trong danh sách RexCinemas. Nếu khách hỏi phim không có, hãy lịch sự thông báo rạp chưa chiếu và gợi ý phim cùng thể loại hiện có tại rạp.\n");
        sb.append("6. Nếu khách muốn đổi vé, hủy vé, hoàn tiền hoặc cần hỗ trợ sự cố khẩn cấp, hướng dẫn khách chuyển sang tab \"Chat với Rạp\" bên cạnh để trò chuyện trực tiếp với Ban Quản lý rạp.\n");
        sb.append("7. Trả lời súc tích, đi thẳng vào trọng tâm, không lan man, tối ưu hiển thị trên khung chatbox.\n");

        return sb.toString();
    }

    /**
     * Bộ máy tư vấn dự phòng (Rule-based Heuristics) khi chưa cấu hình Grok API hoặc khi mất mạng ngoài.
     */
    public String consultRuleBased(String prompt, List<Movie> allActiveMovies) {
        String normalized = removeAccents(prompt.toLowerCase(Locale.ROOT));

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
