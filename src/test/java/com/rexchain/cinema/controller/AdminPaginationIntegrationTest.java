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
import com.rexchain.cinema.entity.Role;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
    @WithMockUser(username = "manager@rexchain.vn", roles = "CINEMA_MANAGER")
    void cinemaManagerCanViewChainDashboardAndReadOnlyOperationalLists() throws Exception {
        createUser("manager@rexchain.vn", Role.CINEMA_MANAGER, null);
        List<Cinema> chain = createChainBookingData();

        var dashboard = mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("cinemaCount", "revenue", "topCinemas", "recentBookings"))
                .andReturn();
        assertThat(dashboard.getModelAndView().getModel().get("cinemaCount")).isEqualTo(2L);
        assertThat((BigDecimal) dashboard.getModelAndView().getModel().get("revenue"))
                .isEqualByComparingTo("350");
        assertThat((List<?>) dashboard.getModelAndView().getModel().get("topCinemas")).hasSize(2);

        var filtered = mvc.perform(get("/admin").param("cinemaId", chain.get(0).getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((BigDecimal) filtered.getModelAndView().getModel().get("revenue"))
                .isEqualByComparingTo("100");

        for (String path : List.of("/admin/movies", "/admin/cinemas", "/admin/auditoriums",
                "/admin/showtimes", "/admin/showtimes/api/occupancy", "/admin/bookings",
                "/admin/transactions")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/admin/transactions/1")).andExpect(status().isNotFound());

        String moviesHtml = mvc.perform(get("/admin/movies")).andReturn().getResponse().getContentAsString();
        String cinemasHtml = mvc.perform(get("/admin/cinemas")).andReturn().getResponse().getContentAsString();
        String roomsHtml = mvc.perform(get("/admin/auditoriums")).andReturn().getResponse().getContentAsString();
        String showtimesHtml = mvc.perform(get("/admin/showtimes")).andReturn().getResponse().getContentAsString();
        String bookingsHtml = mvc.perform(get("/admin/bookings")).andReturn().getResponse().getContentAsString();
        assertThat(moviesHtml).doesNotContain("/admin/movies/new", "/admin/movies/save", "/toggle");
        assertThat(cinemasHtml).doesNotContain("/admin/cinemas/new", "/admin/cinemas/save", "/toggle");
        assertThat(roomsHtml).doesNotContain("/admin/auditoriums/save", "/rename", "/toggle");
        assertThat(showtimesHtml).doesNotContain("/admin/showtimes/save", "/toggle", "+ Xếp lịch");
        assertThat(bookingsHtml).doesNotContain("/admin/checkin", "/refund", "/cancel", "/bookings/");
    }

    @Test
    @WithMockUser(username = "manager@rexchain.vn", roles = "CINEMA_MANAGER")
    void cinemaManagerCannotInvokeAdminMutationsDirectly() throws Exception {
        createUser("manager@rexchain.vn", Role.CINEMA_MANAGER, null);
        for (String path : List.of("/admin/movies/save", "/admin/cinemas/save", "/admin/auditoriums/save",
                "/admin/showtimes/save", "/admin/bookings/1/cancel", "/admin/bookings/1/refund",
                "/admin/users/1/role", "/admin/users/1/toggle", "/admin/payments/save",
                "/admin/rewards/settings")) {
            mvc.perform(post(path).with(csrf()).param("role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
        for (String path : List.of("/admin/users", "/admin/payments", "/admin/late-payments",
                "/admin/rewards", "/admin/checkin", "/admin/support")) {
            mvc.perform(get(path)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "supervisor@rexchain.vn", roles = "SUPERVISOR")
    void supervisorDashboardAndBookingDataRemainScopedToAssignedCinema() throws Exception {
        List<Cinema> chain = createChainBookingData();
        createUser("supervisor@rexchain.vn", Role.SUPERVISOR, chain.get(0));

        var dashboard = mvc.perform(get("/admin").param("cinemaId", chain.get(1).getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((BigDecimal) dashboard.getModelAndView().getModel().get("revenue"))
                .isEqualByComparingTo("100");

        String html = mvc.perform(get("/admin/bookings")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("REX_TEST_CHAIN_BOOKING_1").doesNotContain("REX_TEST_CHAIN_BOOKING_2");
        mvc.perform(get("/admin/showtimes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminStillHasDashboardAndSensitiveAdministrationRoutes() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
        mvc.perform(get("/admin/users")).andExpect(status().isOk());
        mvc.perform(get("/admin/payments")).andExpect(status().isOk());
        mvc.perform(get("/admin/rewards")).andExpect(status().isOk());
    }

    @Test
    void anonymousCannotOpenAdminDashboard() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "staff@rexchain.vn", roles = "STAFF")
    void staffDashboardStillRedirectsToCheckInOnly() throws Exception {
        createUser("staff@rexchain.vn", Role.STAFF, null);
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/checkin"));
    }

    private User createUser(String email, Role role, Cinema cinema) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("REX_TEST_" + role.name());
        user.setPassword("unused-test-hash");
        user.setRole(role);
        user.setCinema(cinema);
        return users.saveAndFlush(user);
    }

    private List<Cinema> createChainBookingData() {
        Cinema first = new Cinema();
        first.setName("REX_TEST_CHAIN_CINEMA_1");
        first.setCity("Test City");
        first.setAddress("REX_TEST_CHAIN_ADDRESS_1");
        Cinema second = new Cinema();
        second.setName("REX_TEST_CHAIN_CINEMA_2");
        second.setCity("Test City");
        second.setAddress("REX_TEST_CHAIN_ADDRESS_2");
        cinemas.saveAllAndFlush(List.of(first, second));

        Movie movie = new Movie();
        movie.setTitle("REX_TEST_CHAIN_MOVIE");
        movie.setDescription("Isolated chain dashboard test");
        movie.setDurationMinutes(100);
        movies.saveAndFlush(movie);

        User customer = users.findByEmailIgnoreCase("user").orElseThrow();
        List<Cinema> chain = List.of(first, second);
        List<BigDecimal> amounts = List.of(new BigDecimal("100"), new BigDecimal("250"));
        List<Booking> testBookings = new ArrayList<>();
        for (int index = 0; index < chain.size(); index++) {
            Cinema cinema = chain.get(index);
            Auditorium room = new Auditorium();
            room.setName("REX_TEST_CHAIN_ROOM_" + (index + 1));
            room.setCinema(cinema);
            room.setTotalRows(1);
            room.setSeatsPerRow(10);
            auditoriums.saveAndFlush(room);

            Showtime showtime = new Showtime();
            showtime.setMovie(movie);
            showtime.setAuditorium(room);
            showtime.setStartTime(LocalDateTime.now().plusDays(1));
            showtime.setBasePrice(amounts.get(index));
            showtimes.saveAndFlush(showtime);

            Booking booking = new Booking();
            booking.setBookingCode("REX_TEST_CHAIN_BOOKING_" + (index + 1));
            booking.setUser(customer);
            booking.setShowtime(showtime);
            booking.setTotalAmount(amounts.get(index));
            booking.setPaymentMethod(PaymentMethod.BANK_QR);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaidAt(LocalDateTime.now());
            booking.setCreatedAt(LocalDateTime.now());
            testBookings.add(booking);
        }
        bookings.saveAllAndFlush(testBookings);
        return chain;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cinemaListUsesVietnameseNameAndCityFiltersWithoutSortControls() throws Exception {
        Cinema cinema = new Cinema();
        cinema.setName("REX_TEST_CITY_FILTER");
        cinema.setCity("Thành phố thử nghiệm");
        cinema.setAddress("Địa chỉ thử nghiệm");
        cinema.setActive(false);
        cinemas.saveAndFlush(cinema);
        Cinema activeCinema = new Cinema();
        activeCinema.setName("REX_TEST_ACTIVE_CITY_FILTER");
        activeCinema.setCity("Thành phố thử nghiệm");
        activeCinema.setAddress("Địa chỉ đang hoạt động");
        activeCinema.setActive(true);
        cinemas.saveAndFlush(activeCinema);

        var result = mvc.perform(get("/admin/cinemas").param("status", "Thành phố thử nghiệm")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        int cardListStart = html.indexOf("<section class=\"row g-4 admin-cinema-grid\"");
        int cardListEnd = html.indexOf("</section>", cardListStart);
        String cardList = html.substring(cardListStart, cardListEnd);
        assertThat(html).contains("Nhập tên rạp", "Thành phố", "Thành phố thử nghiệm", "REX_TEST_CITY_FILTER",
                        "Trạng thái", "Hoạt động", "Đã ẩn", "name=\"active\"")
                .doesNotContain("Sắp xếp theo", "name=\"sort\"", "name=\"dir\"");
        assertThat(cardList).contains("REX_TEST_CITY_FILTER")
                .doesNotContain("REX_TEST_ACTIVE_CITY_FILTER");
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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void allAdminListTemplatesRenderWithEmptyResults() throws Exception {
        createUser("admin", Role.ADMIN, null);
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
