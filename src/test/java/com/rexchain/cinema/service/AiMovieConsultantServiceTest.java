package com.rexchain.cinema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexchain.cinema.config.GrokProperties;
import com.rexchain.cinema.entity.AgeRating;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.MovieStatus;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiMovieConsultantServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private GrokProperties grokProperties;
    private ObjectMapper objectMapper;
    private AiMovieConsultantService consultantService;

    @BeforeEach
    void setUp() {
        grokProperties = new GrokProperties();
        objectMapper = new ObjectMapper();

        Movie m1 = new Movie();
        m1.setId(1L);
        m1.setTitle("Avatar 3: Tro Tàn");
        m1.setGenre("Hành động, Phiêu lưu, Khoa học viễn tưởng");
        m1.setDurationMinutes(180);
        m1.setAgeRating(AgeRating.P);
        m1.setStatus(MovieStatus.NOW_SHOWING);
        m1.setActive(true);
        m1.setReleaseDate(LocalDate.now());

        Cinema c1 = new Cinema();
        c1.setId(10L);
        c1.setName("Rex Cinema Quận 1");
        c1.setCity("TP.HCM");
        c1.setAddress("123 Lê Lợi, Q1");
        c1.setActive(true);

        lenient().when(movieRepository.findAll()).thenReturn(List.of(m1));
        lenient().when(cinemaRepository.findByActiveTrueOrderByCityAscNameAsc()).thenReturn(List.of(c1));

        consultantService = new AiMovieConsultantService(
                movieRepository,
                cinemaRepository,
                grokProperties,
                objectMapper,
                httpClient
        );
    }

    @Test
    @DisplayName("Khi Grok chưa cấu hình API key -> Tự động dùng bộ quy tắc (Rule-based) trả lời giá vé")
    void shouldFallbackToRuleBasedWhenGrokNotConfigured() {
        grokProperties.setApiKey("");

        String reply = consultantService.consult("Giá vé xem phim bao nhiêu tiền?");

        assertThat(reply).contains("Bảng giá vé tham khảo tại RexCinemas");
        assertThat(reply).contains("Standard");
        assertThat(reply).contains("Sweetbox");
    }

    @Test
    @DisplayName("Khi hỏi về bắp nước combo -> Trả lời menu combo bắp nước")
    void shouldAnswerPopcornCombo() {
        grokProperties.setApiKey("");

        String reply = consultantService.consult("Có combo bắp nước nào ngon không?");

        assertThat(reply).contains("Combo Bắp Nước Siêu Hời");
        assertThat(reply).contains("Solo Combo");
        assertThat(reply).contains("Couple Combo");
    }

    @Test
    @DisplayName("Khi Grok đã cấu hình API key -> Gửi request tới Grok API và trả về nội dung AI tạo")
    void shouldCallGrokApiWhenConfigured() throws Exception {
        grokProperties.setEnabled(true);
        grokProperties.setApiKey("xai-test-api-key-12345");
        grokProperties.setModel("grok-2-latest");

        String grokJsonResponse = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Chào bạn! Hiện tại RexCinemas đang chiếu siêu phẩm **Avatar 3: Tro Tàn** [Xem chi tiết & Đặt vé](/movies/1) cực hấp dẫn! Bạn muốn xem vào khung giờ nào ạ? 🍿🎬"
                  }
                }
              ]
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(grokJsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        String reply = consultantService.consult("Gợi ý cho mình phim hành động hay đang chiếu với");

        assertThat(reply).contains("Avatar 3: Tro Tàn");
        assertThat(reply).contains("/movies/1");
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Khi Grok API bị lỗi mạng hoặc HTTP 500 -> Tự động fallback sang Rule-based mượt mà")
    void shouldFallbackToRuleBasedOnGrokApiError() throws Exception {
        grokProperties.setEnabled(true);
        grokProperties.setApiKey("xai-test-api-key-12345");

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        String reply = consultantService.consult("Giá vé ghế VIP bao nhiêu vậy shop?");

        assertThat(reply).isNotNull();
        assertThat(reply).contains("Bảng giá vé tham khảo tại RexCinemas");
    }

    @Test
    @DisplayName("Khi Grok HttpClient ném ngoại lệ kết nối -> Fallback sang Rule-based không làm gián đoạn")
    void shouldFallbackToRuleBasedOnHttpClientException() throws Exception {
        grokProperties.setEnabled(true);
        grokProperties.setApiKey("xai-test-api-key-12345");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection timed out"));

        String reply = consultantService.consult("Có phim kinh dị nào không?");

        assertThat(reply).isNotNull();
        assertThat(reply).isNotEmpty();
    }
}
