package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opt-in integration coverage against local MySQL DEV only. It never sends mail,
 * calls SePay, or issues a bank-transfer command. All rows are REX_TEST fixtures
 * and are removed in {@link #cleanUp()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}",
        "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.refund.mysql-it", matches = "true")
class ManualRefundMySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BookingRepository bookings;
    @Autowired ShowtimeRepository showtimes;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockBean JavaMailSender mailSender;
    @Value("${spring.datasource.url}") String datasourceUrl;

    private User customer;
    private User administrator;
    private User manager;
    private Booking booking;

    @BeforeEach
    void createIsolatedRefundFixtureOnLocalDevelopmentSchema() {
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/.*")).isTrue();
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("rex_chain_cinema");
        Showtime showtime = showtimes.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("MySQL DEV needs an existing showtime for the REX_TEST refund fixture"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        customer = saveUser("rex_test_refund_customer_" + suffix + "@example.invalid", Role.CUSTOMER);
        administrator = saveUser("rex_test_refund_admin_" + suffix + "@example.invalid", Role.ADMIN);
        manager = saveUser("rex_test_refund_manager_" + suffix + "@example.invalid", Role.CINEMA_MANAGER);

        booking = new Booking();
        booking.setBookingCode("REX_TEST_R_" + suffix);
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.REFUND_PENDING);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setTotalAmount(BigDecimal.valueOf(31_000));
        booking.setPaidAt(java.time.LocalDateTime.now());
        booking = bookings.saveAndFlush(booking);
    }

    @Test
    void manualRefundIsAdminOnlyAndMovesFromPendingToConfirmedExactlyOnce() throws Exception {
        String customerToken = tokenFor(customer);
        String adminToken = tokenFor(administrator);
        String managerToken = tokenFor(manager);

        mvc.perform(get("/bookings/{id}", booking.getId()).cookie(cookie(customerToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Chưa có xác nhận tiền đã về tài khoản")));

        mvc.perform(post("/admin/bookings/{id}/refund", booking.getId()).with(csrf()).cookie(cookie(managerToken))
                        .param("refundReference", "REX_TEST_TRANSFER")
                        .param("refundAmount", "31000")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().isForbidden());
        assertThat(bookings.findById(booking.getId())).get().extracting(Booking::getStatus)
                .isEqualTo(BookingStatus.REFUND_PENDING);

        mvc.perform(post("/admin/bookings/{id}/refund", booking.getId()).with(csrf()).cookie(cookie(adminToken))
                        .param("refundReference", "")
                        .param("refundAmount", "31000")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(bookings.findById(booking.getId())).get().extracting(Booking::getStatus)
                .isEqualTo(BookingStatus.REFUND_PENDING);

        mvc.perform(post("/admin/bookings/{id}/refund", booking.getId()).with(csrf()).cookie(cookie(adminToken))
                        .param("refundReference", "REX_TEST_TRANSFER_001")
                        .param("refundAmount", "31000")
                        .param("refundEvidence", "REX_TEST receipt")
                        .param("note", "REX_TEST manual reconciliation")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().is3xxRedirection());

        Booking confirmed = bookings.findById(booking.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(confirmed.getRefundAmount()).isEqualByComparingTo("31000");
        assertThat(confirmed.getRefundReference()).isEqualTo("REX_TEST_TRANSFER_001");
        assertThat(confirmed.getRefundedByUserId()).isEqualTo(administrator.getId());
        assertThat(confirmed.getRefundedAt()).isNotNull();

        mvc.perform(post("/admin/bookings/{id}/refund", booking.getId()).with(csrf()).cookie(cookie(adminToken))
                        .param("refundReference", "REX_TEST_TRANSFER_SHOULD_NOT_REPLACE")
                        .param("refundAmount", "31000")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(bookings.findById(booking.getId())).get().extracting(Booking::getRefundReference)
                .isEqualTo("REX_TEST_TRANSFER_001");
        Integer confirmationNotifications = jdbc.queryForObject(
                "SELECT COUNT(*) FROM account_notifications WHERE user_id = ? AND deduplication_key = ?",
                Integer.class, customer.getId(), booking.getBookingCode() + ":refunded");
        assertThat(confirmationNotifications).isEqualTo(1);

        mvc.perform(get("/bookings/{id}", booking.getId()).cookie(cookie(customerToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Rạp đã xác nhận hoàn tiền thủ công")));
    }

    @AfterEach
    void cleanUp() {
        if (customer != null) {
            jdbc.update("DELETE FROM account_notifications WHERE user_id IN (?, ?, ?)",
                    customer.getId(), administrator.getId(), manager.getId());
        }
        if (booking != null && booking.getId() != null) jdbc.update("DELETE FROM bookings WHERE id = ?", booking.getId());
        if (customer != null) jdbc.update("DELETE FROM app_users WHERE id IN (?, ?, ?)",
                customer.getId(), administrator.getId(), manager.getId());
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.setFullName("REX_TEST manual refund");
        user.setPassword(encoder.encode("RexTestPassword123"));
        user.setRole(role);
        user.setEnabled(true);
        return users.saveAndFlush(user);
    }

    private String tokenFor(User user) {
        return jwt.generate(user.getEmail(), user.getRole().name(), false, user.getCredentialVersion());
    }

    private org.springframework.mock.web.MockCookie cookie(String token) {
        org.springframework.mock.web.MockCookie cookie = new org.springframework.mock.web.MockCookie("access_token", token);
        cookie.setHttpOnly(true);
        return cookie;
    }
}
