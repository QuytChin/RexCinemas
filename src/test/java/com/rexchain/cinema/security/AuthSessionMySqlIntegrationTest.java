package com.rexchain.cinema.security;

import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.RevokedAccessTokenRepository;
import com.rexchain.cinema.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in, local MySQL-only session integration test. Enable with -Drex.auth.mysql-it=true. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}",
        "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.auth.mysql-it", matches = "true")
class AuthSessionMySqlIntegrationTest {
    private static final String OLD_PASSWORD = "RexTestOld123";
    private static final String NEW_PASSWORD = "RexTestNew456";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RevokedAccessTokenRepository revokedTokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") String datasourceUrl;
    @MockBean JavaMailSender mailSender;

    private String email;
    private User user;
    private String currentToken;
    private String parallelToken;
    private String replacementToken;

    @BeforeEach
    void createIsolatedUserOnlyOnLocalDevelopmentSchema() {
        String jdbcUrl = jdbc.queryForObject("SELECT DATABASE()", String.class);
        String host = jdbc.queryForObject("SELECT @@hostname", String.class);
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/.*")).isTrue();
        assertThat(host).isNotBlank();
        assertThat(jdbcUrl).isEqualTo("rex_chain_cinema");

        String requestedEmail = System.getProperty("rex.auth.fixture.email", "").trim();
        if (!requestedEmail.isBlank()) {
            assertThat(requestedEmail.toLowerCase()).startsWith("rex_test_");
            assertThat(users.existsByEmailIgnoreCase(requestedEmail)).isFalse();
            email = requestedEmail;
        } else {
            email = "rex_test_auth_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                    + "@example.invalid";
        }
        user = new User();
        user.setEmail(email);
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.setFullName("REX_TEST auth session");
        user.setPassword(encoder.encode(OLD_PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        users.saveAndFlush(user);
    }

    @Test
    void loginLogoutAndPasswordChangeRevokeOnlyExpectedTokens() throws Exception {
        currentToken = login(OLD_PASSWORD);
        parallelToken = login(OLD_PASSWORD);
        mvc.perform(get("/profile").cookie(cookie(currentToken))).andExpect(status().isOk());
        mvc.perform(get("/admin/users").cookie(cookie(currentToken))).andExpect(status().isForbidden());

        mvc.perform(post("/auth/logout").with(csrf()).cookie(cookie(currentToken)))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/profile").cookie(cookie(currentToken))).andExpect(status().is3xxRedirection());
        mvc.perform(get("/profile").cookie(cookie(parallelToken))).andExpect(status().isOk());

        replacementToken = login(OLD_PASSWORD);
        mvc.perform(post("/profile/password").with(csrf()).cookie(cookie(replacementToken))
                        .param("currentPassword", OLD_PASSWORD)
                        .param("newPassword", NEW_PASSWORD)
                        .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/profile").cookie(cookie(replacementToken))).andExpect(status().is3xxRedirection());

        mvc.perform(post("/auth/login").with(csrf())
                        .param("identifier", email).param("password", OLD_PASSWORD))
                .andExpect(status().isOk());
        String postChangeToken = login(NEW_PASSWORD);
        mvc.perform(get("/profile").cookie(cookie(postChangeToken))).andExpect(status().isOk());

        assertThat(users.findByEmailIgnoreCase(email)).get().extracting(User::getCredentialVersion).isEqualTo(1);
    }

    private String login(String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("identifier", email).param("password", password).param("remember", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        return result.getResponse().getCookie("access_token").getValue();
    }

    private MockCookie cookie(String value) {
        MockCookie cookie = new MockCookie("access_token", value);
        cookie.setHttpOnly(true);
        return cookie;
    }

    @AfterEach
    @Transactional
    void removeOnlyThisTestUserAndRevocationRows() throws Exception {
        if (currentToken != null) revokedTokens.deleteByTokenHash(fingerprint(currentToken));
        if (parallelToken != null) revokedTokens.deleteByTokenHash(fingerprint(parallelToken));
        if (replacementToken != null) revokedTokens.deleteByTokenHash(fingerprint(replacementToken));
        if (Boolean.getBoolean("rex.auth.fixture.keep")) return;
        if (email != null) users.findByEmailIgnoreCase(email).ifPresent(users::delete);
        users.flush();
    }

    private String fingerprint(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
