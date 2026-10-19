package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.ManualRefundConfirmation;
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
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in real-MySQL coverage for late-payment idempotency, audit history and admin-only refund. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}",
        "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.late.mysql-it", matches = "true")
class LatePaymentCaseMySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired LatePaymentCaseService latePaymentCases;
    @Autowired BookingRepository bookings;
    @Autowired ShowtimeRepository showtimes;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") String datasourceUrl;
    @MockBean JavaMailSender mailSender;

    private User customer;
    private User administrator;
    private User manager;
    private Booking booking;
    private Long caseId;
    private String providerId;

    @BeforeEach
    void createFixtureOnLocalDevelopmentSchema() {
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(localhost|127\\.0\\.1):3306/.*")).isTrue();
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("rex_chain_cinema");
        Showtime showtime = showtimes.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("MySQL DEV needs an existing showtime"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        customer = saveUser("rex_test_late_c_" + suffix + "@example.invalid", Role.CUSTOMER);
        administrator = saveUser("rex_test_late_a_" + suffix + "@example.invalid", Role.ADMIN);
        manager = saveUser("rex_test_late_m_" + suffix + "@example.invalid", Role.CINEMA_MANAGER);

        booking = new Booking();
        booking.setBookingCode("REX_TEST_LATE_" + suffix);
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setPaymentReference("REX" + suffix);
        booking.setTotalAmount(BigDecimal.valueOf(42_000));
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        booking = bookings.saveAndFlush(booking);
    }

    @Test
    void lateCaseIsIdempotentAuditedAndAdminRefundIsSafe() throws Exception {
        providerId = "REX_TEST_TX_" + booking.getBookingCode();
        var first = latePaymentCases.open(booking, providerId, booking.getTotalAmount(),
                booking.getExpiresAt().plusSeconds(30), "REX_TEST late payment");
        caseId = first.getId();
        var duplicate = latePaymentCases.open(booking, providerId, booking.getTotalAmount(),
                booking.getExpiresAt().plusSeconds(30), "REX_TEST duplicate");

        assertThat(duplicate.getId()).isEqualTo(caseId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM late_payment_cases WHERE provider_transaction_id = ?",
                Integer.class, providerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM late_payment_case_audits WHERE late_payment_case_id = ?",
                Integer.class, caseId)).isEqualTo(1);

        latePaymentCases.markRefundFailed(caseId, administrator.getId(), administrator.getEmail(), "REX_TEST bank rejected");
        latePaymentCases.markRefundFailed(caseId, administrator.getId(), administrator.getEmail(), "REX_TEST retry rejected");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM late_payment_case_audits WHERE late_payment_case_id = ? AND action = 'REFUND_FAILED'",
                Integer.class, caseId)).isEqualTo(2);

        String managerToken = jwt.generate(manager.getEmail(), manager.getRole().name(), false, manager.getCredentialVersion());
        mvc.perform(post("/admin/late-payments/{id}/refund", caseId).with(csrf()).cookie(cookie(managerToken))
                        .param("refundReference", "REX_TEST_MANAGER_BLOCKED")
                        .param("refundAmount", "42000")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().isForbidden());

        String adminToken = jwt.generate(administrator.getEmail(), administrator.getRole().name(), false, administrator.getCredentialVersion());
        mvc.perform(post("/admin/late-payments/{id}/refund", caseId).with(csrf()).cookie(cookie(adminToken))
                        .param("refundReference", "REX_TEST_LATE_REFUND")
                        .param("refundAmount", "42000")
                        .param("refundEvidence", "REX_TEST evidence")
                        .param("manualTransferConfirmed", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(jdbc.queryForObject("SELECT status FROM late_payment_cases WHERE id = ?", String.class, caseId))
                .isEqualTo("REFUNDED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM late_payment_case_audits WHERE late_payment_case_id = ?",
                Integer.class, caseId)).isEqualTo(4);
        assertThat(bookings.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_notifications WHERE user_id = ? AND deduplication_key LIKE ?",
                Integer.class, customer.getId(), booking.getBookingCode() + ":late-payment-pending:%")).isEqualTo(1);
    }

    @AfterEach
    void cleanUpOnlyTestRows() {
        if (caseId != null) jdbc.update("DELETE FROM late_payment_case_audits WHERE late_payment_case_id = ?", caseId);
        if (caseId != null) jdbc.update("DELETE FROM late_payment_cases WHERE id = ?", caseId);
        if (customer != null) jdbc.update("DELETE FROM account_notifications WHERE user_id IN (?, ?, ?)",
                customer.getId(), administrator.getId(), manager.getId());
        if (providerId != null) jdbc.update("DELETE FROM account_notifications WHERE deduplication_key LIKE ?",
                "%" + providerId);
        if (booking != null && booking.getId() != null) jdbc.update("DELETE FROM bookings WHERE id = ?", booking.getId());
        if (customer != null) jdbc.update("DELETE FROM app_users WHERE id IN (?, ?, ?)",
                customer.getId(), administrator.getId(), manager.getId());
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.setFullName("REX_TEST late payment");
        user.setPassword(encoder.encode("RexTestPassword123"));
        user.setRole(role);
        user.setEnabled(true);
        return users.saveAndFlush(user);
    }

    private MockCookie cookie(String token) {
        MockCookie cookie = new MockCookie("access_token", token);
        cookie.setHttpOnly(true);
        return cookie;
    }
}
