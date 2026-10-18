package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.SecurityConfig;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.security.CustomUserDetailsService;
import com.rexchain.cinema.security.JwtAuthenticationFilter;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CinemaRepository cinemaRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void loginAliasRendersRexCinemasPageInsteadOfSpringDefaultPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Đăng nhập")))
                .andExpect(content().string(containsString("action=\"/auth/login\"")))
                .andExpect(content().string(containsString("name=\"identifier\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("Please sign in"))))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void registerAliasRendersRexCinemasPage() throws Exception {
        when(cinemaRepository.findByActiveTrueOrderByCityAscNameAsc()).thenReturn(List.of());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Đăng ký")))
                .andExpect(content().string(not(containsString("Please sign in"))));
    }

    @Test
    void protectedPageRedirectsToCustomLoginInsteadOfBasicAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?returnUrl=%2Fadmin"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void invalidCredentialsReturnVietnameseErrorOnCustomPage() throws Exception {
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authService).login("rexmember", "wrong-password", false);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "rexmember")
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Thông tin đăng nhập hoặc mật khẩu không đúng"))
                .andExpect(content().string(not(containsString("Please sign in"))));
    }

    @Test
    void validCredentialsIssueJwtCookieAndRedirect() throws Exception {
        when(authService.login("member@rexchain.vn", "secret", true)).thenReturn("signed-jwt");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "member@rexchain.vn")
                        .param("password", "secret")
                        .param("remember", "true")
                        .param("returnUrl", "/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(header().string("Set-Cookie", containsString("access_token=signed-jwt")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }
}
