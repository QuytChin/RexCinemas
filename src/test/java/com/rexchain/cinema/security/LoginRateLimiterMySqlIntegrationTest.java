package com.rexchain.cinema.security;

import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in integration test against the local MySQL schema; its fixture transaction is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "mysql"})
@TestPropertySource(properties = {
        "app.jwt.secret=${JWT_SECRET}",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.mail.username=${MAIL_USERNAME}",
        "spring.mail.password=${MAIL_PASSWORD}"
})
@EnabledIfSystemProperty(named = "rex.login.mysql-it", matches = "true")
@Transactional
class LoginRateLimiterMySqlIntegrationTest {
    private static final String PASSWORD = "RexTestLogin123!";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") String datasourceUrl;
    @MockBean JavaMailSender mailSender;

    @Test
    void webAndApiShareTheSqlResolvedAccountLimitAcrossAliasesAndIps() throws Exception {
        assertThat(datasourceUrl.matches("(?i)^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/.*")).isTrue();
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("rex_chain_cinema");
        assertThat(jdbc.queryForObject("SELECT @@hostname", String.class)).isNotBlank();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String email = "rex_test_login_" + suffix + "@example.invalid";
        String username = "rex_test_" + suffix;
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName("REX_TEST login rate limit");
        user.setPassword(encoder.encode(PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        users.saveAndFlush(user);

        for (int attempt = 0; attempt < 5; attempt++) {
            String ip = "192.0.2." + (101 + attempt);
            if (attempt % 2 == 0) {
                mvc.perform(post("/auth/login").with(csrf()).with(remoteAddress(ip))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("identifier", attempt == 0 ? email.toUpperCase(java.util.Locale.ROOT) : email)
                                .param("password", "incorrect-password"))
                        .andExpect(status().isOk());
            } else {
                String alias = attempt == 1 ? username : email;
                mvc.perform(post("/api/auth/login").with(remoteAddress(ip))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"identifier\":\"" + alias + "\",\"password\":\"incorrect-password\"}"))
                        .andExpect(status().isUnauthorized());
            }
        }

        mvc.perform(post("/api/auth/login").with(remoteAddress("192.0.2.120"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(header().doesNotExist("Set-Cookie"));

        mvc.perform(post("/auth/login").with(csrf()).with(remoteAddress("192.0.2.121"))
                        .param("identifier", username).param("password", PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
