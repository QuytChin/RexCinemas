package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.AccountNotification;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.AccountNotificationRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in real-MySQL coverage using only self-cleaning REX_TEST_ records. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}", "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}", "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.notification.mysql-it", matches = "true")
class NotificationReadMySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountNotificationRepository notifications;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Value("${spring.datasource.url}") String datasourceUrl;
    @MockBean JavaMailSender mailSender;

    private User owner;
    private User other;
    private AccountNotification first;

    @BeforeEach
    void createLocalFixture() {
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/.*")).isTrue();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        owner = saveUser("rex_test_notification_" + suffix + "@example.invalid");
        other = saveUser("rex_test_notification_other_" + suffix + "@example.invalid");
        first = saveNotification(owner, "REX_TEST_NOTIFICATION_1_" + suffix);
        saveNotification(owner, "REX_TEST_NOTIFICATION_2_" + suffix);
        saveNotification(owner, "REX_TEST_NOTIFICATION_3_" + suffix);
        saveNotification(other, "REX_TEST_NOTIFICATION_OTHER_" + suffix);
    }

    @Test
    void readStatePersistsPerUserAcrossRefreshAndDuplicateRequests() throws Exception {
        MockCookie ownerCookie = cookie(owner);
        assertThat(notifications.countByUserIdAndReadAtPresentFalse(owner.getId())).isEqualTo(3);

        mvc.perform(post("/api/notifications/{id}/read", first.getId()).with(csrf()).cookie(ownerCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(2));
        mvc.perform(post("/api/notifications/{id}/read", first.getId()).with(csrf()).cookie(ownerCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(2));
        assertThat(notifications.countByUserIdAndReadAtPresentFalse(owner.getId())).isEqualTo(2);

        mvc.perform(post("/api/notifications/read-all").with(csrf()).cookie(ownerCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        assertThat(notifications.countByUserIdAndReadAtPresentFalse(owner.getId())).isZero();
        mvc.perform(get("/notifications").cookie(ownerCookie)).andExpect(status().isOk());
        assertThat(notifications.countByUserIdAndReadAtPresentFalse(owner.getId())).isZero();
    }

    @Test
    void cannotReadAnotherUsersNotification() throws Exception {
        AccountNotification otherNotification = notifications.findByUserIdOrderByCreatedAtDesc(other.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().getFirst();
        mvc.perform(post("/api/notifications/{id}/read", otherNotification.getId()).with(csrf()).cookie(cookie(owner)))
                .andExpect(status().isNotFound());
        assertThat(notifications.countByUserIdAndReadAtPresentFalse(other.getId())).isEqualTo(1);
    }

    @AfterEach
    void cleanUpOnlyFixtureRows() {
        if (owner != null) notifications.deleteAll(notifications.findByUserIdOrderByCreatedAtDesc(owner.getId(), org.springframework.data.domain.PageRequest.of(0, 20)).getContent());
        if (other != null) notifications.deleteAll(notifications.findByUserIdOrderByCreatedAtDesc(other.getId(), org.springframework.data.domain.PageRequest.of(0, 20)).getContent());
        if (owner != null) users.deleteById(owner.getId());
        if (other != null) users.deleteById(other.getId());
    }

    private User saveUser(String email) {
        User user = new User(); user.setEmail(email); user.setUsername(email.substring(0, email.indexOf('@')));
        user.setFullName("REX_TEST notification"); user.setPassword(encoder.encode("RexTestPassword123"));
        user.setRole(Role.CUSTOMER); user.setEnabled(true);
        return users.saveAndFlush(user);
    }

    private AccountNotification saveNotification(User user, String key) {
        AccountNotification notification = new AccountNotification();
        notification.setUser(user); notification.setType("REX_TEST"); notification.setTitle(key);
        notification.setMessage("REX_TEST long notification content for persistence coverage.");
        notification.setLink("/notifications"); notification.setDeduplicationKey(key);
        return notifications.saveAndFlush(notification);
    }

    private MockCookie cookie(User user) {
        MockCookie cookie = new MockCookie("access_token", jwt.generate(user.getEmail(), user.getRole().name(), false, user.getCredentialVersion()));
        cookie.setHttpOnly(true);
        return cookie;
    }
}
