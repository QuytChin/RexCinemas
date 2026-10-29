package com.rexchain.cinema.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import com.rexchain.cinema.config.RequiredSecretsValidator;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.OtpService;
import com.rexchain.cinema.service.PasswordResetService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadAuthorizationSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private RequiredSecretsValidator requiredSecretsValidator;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private OtpService otpService;

    @MockBean
    private PasswordResetService passwordResetService;

    @Test
    void anonymousCannotReadPaymentStatus() throws Exception {
        mockMvc.perform(get("/api/payment-status/REX1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReachAdminMovieUploadEndpoint() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "posterFile", "poster.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/admin/movies/save").file(image).with(csrf())
                        .param("title", "REX_TEST unauthorized")
                        .param("description", "This request must be blocked before validation or persistence.")
                        .param("durationMinutes", "120"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotChangeMoviesOrCinemas() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/movies/1/toggle").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/cinemas/1/toggle").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotChangeUnscopedContentOrVouchers() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/events/1/toggle").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/vouchers/1/toggle").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
