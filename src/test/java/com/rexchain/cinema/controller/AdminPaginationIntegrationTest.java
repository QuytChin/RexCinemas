package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.RequiredSecretsValidator;
import com.rexchain.cinema.entity.Movie;
import com.rexchain.cinema.entity.MovieStatus;
import com.rexchain.cinema.entity.MembershipTier;
import com.rexchain.cinema.entity.MembershipTierPolicy;
import com.rexchain.cinema.entity.LoyaltySettings;
import com.rexchain.cinema.dto.MembershipStatus;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Auditorium;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.repository.MovieRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.AuditoriumRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.OtpService;
import com.rexchain.cinema.service.PasswordResetService;
import com.rexchain.cinema.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPaginationIntegrationTest {
    @Autowired AdminListSupport lists;
    @Autowired MovieRepository movies;
    @Autowired UserRepository users;
    @Autowired BookingRepository bookings;
    @Autowired CinemaRepository cinemas;
    @Autowired AuditoriumRepository auditoriums;
    @Autowired ShowtimeRepository showtimes;
    @Autowired MockMvc mvc;
    @MockBean JavaMailSender mailSender;
    @MockBean RequiredSecretsValidator requiredSecretsValidator;
    @MockBean JwtService jwtService;
    @MockBean OtpService otpService;
    @MockBean PasswordResetService passwordResetService;
    @MockBean MembershipService membershipService;

    @BeforeEach
    void authAccountForTemplate() {
        when(membershipService.statusFor(anyInt())).thenReturn(
                new MembershipStatus(MembershipTier.MEMBER, null, 0, null, null, 0, 100));
        MembershipTierPolicy member = new MembershipTierPolicy();
        member.setTier(MembershipTier.MEMBER);
        when(membershipService.tierPolicies()).thenReturn(List.of(member));
        when(membershipService.settings()).thenReturn(new LoyaltySettings());
        if (users.findByEmailIgnoreCase("user").isPresent()) return;
        User user = new User();
        user.setEmail("user");
        user.setFullName("REX_TEST_ADMIN");
        user.setPassword("unused-test-hash");
        users.saveAndFlush(user);
    }

    @Test
    void validatesPageSizeAndSortWhitelist() {
        assertThat(lists.pageable(Map.of(), new String[]{"createdAt", "title"}).getPageSize()).isEqualTo(20);
        assertThat(lists.pageable(Map.of("page", "-4", "size", "1000", "sort", "passwordHash"),
                new String[]{"createdAt", "title"}).getPageNumber()).isZero();
        assertThat(lists.pageable(Map.of("page", "-4", "size", "1000", "sort", "passwordHash"),
                new String[]{"createdAt", "title"}).getSort().getOrderFor("createdAt")).isNotNull();
        for (int size : new int[]{10, 20, 50, 100}) {
            assertThat(lists.pageable(Map.of("size", String.valueOf(size)), new String[]{"id"}).getPageSize()).isEqualTo(size);
        }
        assertThat(lists.pageable(Map.of("size", "101"), new String[]{"id"}).getPageSize()).isEqualTo(20);
    }

    @Test
    void linksPreserveSearchFilterSizeAndSort() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/bookings");
        request.addParameter("q", "REX_TEST_BOOK");
        request.addParameter("status", "CONFIRMED");
        request.addParameter("size", "50");
        request.addParameter("sort", "totalAmount");
        request.addParameter("page", "1");
        String link = lists.link(request, "page", 2);
        assertThat(link).contains("q=REX_TEST_BOOK", "status=CONFIRMED", "size=50",
                "sort=totalAmount", "page=2").doesNotContain("page=1");
    }

    @Test
    void pagesStableAndCombinesSearchFilterSortOnIsolatedH2() {
        var batch = new ArrayList<Movie>();
        for (int i = 0; i < 45; i++) {
            Movie movie = new Movie();
            movie.setTitle("REX_TEST_PAGINATION_" + i);
            movie.setDescription("isolated H2 test");
            movie.setDurationMinutes(100);
            movie.setGenre("REX_TEST_GENRE");
            movie.setStatus(i < 42 ? MovieStatus.NOW_SHOWING : MovieStatus.COMING_SOON);
            movie.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
            batch.add(movie);
        }
        movies.saveAllAndFlush(batch);
        var options = new AdminListSupport.Options("title", "status", "createdAt", "id");
        var ids = new HashSet<Long>();
        for (int p = 0; p < 3; p++) {
            Map<String, String> params = new HashMap<>(Map.of("page", String.valueOf(p), "size", "20",
                    "q", "REX_TEST_PAGINATION_", "status", "NOW_SHOWING", "sort", "createdAt"));
            var page = lists.query(movies, params, options);
            assertThat(page.getTotalElements()).isEqualTo(42);
            assertThat(page.getContent()).hasSize(p == 2 ? 2 : 20);
            page.forEach(movie -> assertThat(ids.add(movie.getId())).isTrue());
        }
        assertThat(ids).hasSize(42);
        assertThat(lists.query(movies, Map.of("q", "REX_TEST_NO_SUCH_MOVIE"), options).isEmpty()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bookingPagesLoadAssociationsWithoutPaginatingFetchedCollections() throws Exception {
        Cinema cinema = new Cinema(); cinema.setName("REX_TEST_PAGE_CINEMA"); cinema.setCity("Test"); cinema.setAddress("Test");
        cinemas.saveAndFlush(cinema);
        Auditorium room = new Auditorium(); room.setName("REX_TEST_PAGE_ROOM"); room.setCinema(cinema);
        room.setTotalRows(1); room.setSeatsPerRow(1); auditoriums.saveAndFlush(room);
        Movie movie = new Movie(); movie.setTitle("REX_TEST_PAGE_MOVIE"); movie.setDescription("Test");
        movie.setDurationMinutes(100); movies.saveAndFlush(movie);
        Showtime showtime = new Showtime(); showtime.setMovie(movie); showtime.setAuditorium(room);
        showtime.setStartTime(LocalDateTime.now().plusDays(1)); showtime.setBasePrice(BigDecimal.TEN);
        showtimes.saveAndFlush(showtime);
        User user = users.findByEmailIgnoreCase("user").orElseThrow();
        var batch = new ArrayList<Booking>();
        for (int i = 0; i < 25; i++) {
            Booking booking = new Booking(); booking.setBookingCode("REX_TEST_PAGEBOOK_" + i);
            booking.setUser(user); booking.setShowtime(showtime); booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentMethod(PaymentMethod.BANK_QR); booking.setTotalAmount(BigDecimal.TEN);
            booking.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0)); batch.add(booking);
        }
        bookings.saveAllAndFlush(batch);
        var options = new AdminListSupport.Options("bookingCode", "status",
                new String[]{"createdAt", "id"}, new String[]{"user", "showtime.movie", "showtime.auditorium.cinema"});
        var first = lists.query(bookings, Map.of("q", "REX_TEST_PAGEBOOK", "size", "20", "page", "0"), options);
        var second = lists.query(bookings, Map.of("q", "REX_TEST_PAGEBOOK", "size", "20", "page", "1"), options);
        assertThat(first.getTotalElements()).isEqualTo(25);
        assertThat(first.getContent()).hasSize(20);
        assertThat(second.getContent()).hasSize(5);
        assertThat(first.getContent().stream().map(Booking::getId).toList())
                .doesNotContainAnyElementsOf(second.getContent().stream().map(Booking::getId).toList());
        assertThat(bookings.loadSeatsForPage(first.getContent().stream().map(Booking::getId).toList())).hasSize(20);
        mvc.perform(get("/admin/bookings").param("q", "REX_TEST_PAGEBOOK").param("page", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRenderListWithPaginationControls() throws Exception {
        mvc.perform(get("/admin/movies").param("size", "10").param("sort", "title"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CINEMA_MANAGER")
    void managerCanRenderMoviesButNotUsers() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().isOk());
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cinemaListUsesVietnameseNameAndCityFiltersWithoutSortControls() throws Exception {
        Cinema cinema = new Cinema();
        cinema.setName("REX_TEST_CITY_FILTER");
        cinema.setCity("Thành phố thử nghiệm");
        cinema.setAddress("Địa chỉ thử nghiệm");
        cinemas.saveAndFlush(cinema);

        var result = mvc.perform(get("/admin/cinemas").param("status", "Thành phố thử nghiệm"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Nhập tên rạp", "Thành phố", "Thành phố thử nghiệm", "REX_TEST_CITY_FILTER")
                .doesNotContain("Sắp xếp theo", "name=\"sort\"", "name=\"dir\"");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminLists() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/bookings")).andExpect(status().isForbidden());
    }

    @Test
    void actuatorHealthIsPublicAndMinimal() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allAdminListTemplatesRenderWithEmptyResults() throws Exception {
        for (String path : List.of("users", "bookings", "payments", "transactions", "late-payments", "checkin",
                "rewards", "showtimes", "movies", "cinemas", "auditoriums", "combos", "vouchers",
                "reviews", "events", "articles", "media")) {
            mvc.perform(get("/admin/" + path).param("q", "REX_TEST_NO_MATCH"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movieCreateEditAndVisibilityActionsRemainAvailable() throws Exception {
        mvc.perform(get("/admin/movies/new")).andExpect(status().isOk());
        mvc.perform(post("/admin/movies/save").with(csrf())
                .param("title", "REX_TEST_ADMIN_ACTION")
                .param("description", "Isolated H2 action test")
                .param("durationMinutes", "100"))
                .andExpect(status().is3xxRedirection());
        Movie saved = movies.findByTitleIgnoreCase("REX_TEST_ADMIN_ACTION").orElseThrow();
        mvc.perform(get("/admin/movies/" + saved.getId() + "/edit")).andExpect(status().isOk());
        mvc.perform(post("/admin/movies/" + saved.getId() + "/toggle").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(movies.findById(saved.getId()).orElseThrow().isActive()).isFalse();
        mvc.perform(post("/admin/movies/" + saved.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
