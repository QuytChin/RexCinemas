package com.rexchain.cinema.controller;

import com.rexchain.cinema.config.SecurityConfig;
import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.CustomUserDetailsService;
import com.rexchain.cinema.security.JwtAuthenticationFilter;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.AuthService;
import com.rexchain.cinema.service.LoginRateLimitException;
import com.rexchain.cinema.service.OtpRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import com.rexchain.cinema.service.PasswordResetService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, PolicyController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "server.forward-headers-strategy=framework")
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

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccessTokenRevocationService revocationService;

    @MockBean
    private com.rexchain.cinema.service.OtpService otpService;

    @MockBean
    private PasswordResetService passwordResetService;

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
    void approvedPoliciesArePublicAndRegistrationLinksHaveTargets() throws Exception {
        mockMvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(view().name("policies"))
                .andExpect(content().string(containsString("id=\"terms\"")))
                .andExpect(content().string(containsString("id=\"privacy\"")))
                .andExpect(content().string(containsString("Phiên bản 1.0")))
                .andExpect(content().string(not(containsString("dự thảo"))));
    }

    @Test
    void signedInUserOpeningLoginIsSentToProfile() throws Exception {
        mockMvc.perform(get("/auth/login").with(user("member").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
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
    void forgotPasswordPageRendersDedicatedResetFlow() throws Exception {
        mockMvc.perform(get("/auth/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(content().string(containsString("resetRequestForm")))
                .andExpect(content().string(containsString("resetVerifyForm")));
    }

    @Test
    void resetRequestUsesSamePublicResponseForDifferentAccountStates() throws Exception {
        String known = mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .with(csrf()).contentType("application/json")
                        .content("{\"email\":\"known@example.invalid\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String unknown = mockMvc.perform(post("/api/auth/forgot-password/send-otp")
                        .with(csrf()).contentType("application/json")
                        .content("{\"email\":\"unknown@example.invalid\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(known).isEqualTo(unknown);
        org.mockito.Mockito.verify(passwordResetService).requestReset("known@example.invalid", "127.0.0.1");
        org.mockito.Mockito.verify(passwordResetService).requestReset("unknown@example.invalid", "127.0.0.1");
    }

    @Test
    void resetCompletionReturnsLoginRedirectAndDelegatesProof() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .with(csrf()).contentType("application/json")
                        .content("""
                                {"email":"reset@example.invalid","resetToken":"proof",
                                 "newPassword":"NewPassword123","confirmPassword":"NewPassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/login?reset=success"));
        org.mockito.Mockito.verify(passwordResetService).resetPassword(
                "reset@example.invalid", "proof", "NewPassword123", "NewPassword123");
    }

    @Test
    void unknownPageReturnsNotFoundInsteadOfLoginRedirect() throws Exception {
        mockMvc.perform(get("/no-such-route"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void invalidCredentialsReturnVietnameseErrorOnCustomPage() throws Exception {
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authService).login("rexmember", "wrong-password", false, "127.0.0.1");

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
    void webLoginRateLimitReturns429AndRetryAfter() throws Exception {
        doThrow(new LoginRateLimitException(900)).when(authService)
                .login("member@example.invalid", "secret", false, "127.0.0.1");

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("identifier", "member@example.invalid").param("password", "secret"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Đăng nhập tạm thời bị giới hạn. Vui lòng thử lại sau."));
    }

    @Test
    void validCredentialsIssueJwtCookieAndRedirect() throws Exception {
        when(authService.login("member@rexchain.vn", "secret", true, "127.0.0.1")).thenReturn("signed-jwt");
        when(jwtService.expirationMs(true)).thenReturn(2_592_000_000L);

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
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=2592000")))
                .andExpect(header().string("Set-Cookie", not(containsString("Secure"))));
    }

    @Test
    void secureRequestIssuesSecureCookie() throws Exception {
        when(authService.login("member@rexchain.vn", "secret", false, "127.0.0.1")).thenReturn("signed-jwt");
        when(jwtService.expirationMs(false)).thenReturn(86_400_000L);

        mockMvc.perform(post("/auth/login").secure(true).with(csrf())
                        .param("identifier", "member@rexchain.vn").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=86400")));
    }

    @Test
    void forwardedHttpsRequestIssuesSecureCookie() throws Exception {
        when(authService.login("member@rexchain.vn", "secret", false, "127.0.0.1")).thenReturn("signed-jwt");
        when(jwtService.expirationMs(false)).thenReturn(86_400_000L);

        mockMvc.perform(post("/auth/login").header("X-Forwarded-Proto", "https").with(csrf())
                        .param("identifier", "member@rexchain.vn").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    @Test
    void apiLoginRateLimitReturns429AndRetryAfter() throws Exception {
        doThrow(new LoginRateLimitException(600)).when(authService)
                .login("member@example.invalid", "secret", false, "127.0.0.1");

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"identifier\":\"member@example.invalid\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"))
                .andExpect(jsonPath("$.error").value("Đăng nhập tạm thời bị giới hạn. Vui lòng thử lại sau."));
    }

    @Test
    void logoutRevokesCurrentJwtAndClearsHttpOnlyCookie() throws Exception {
        String token = "existing-jwt";
        java.time.Instant expiresAt = java.time.Instant.now().plusSeconds(600);
        when(jwtService.valid(token)).thenReturn(true);
        when(jwtService.extractExpiration(token)).thenReturn(expiresAt);

        mockMvc.perform(post("/auth/logout").with(csrf()).cookie(new jakarta.servlet.http.Cookie("access_token", token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie", containsString("access_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));

        org.mockito.Mockito.verify(revocationService).revoke(token, expiresAt);
    }

    @Test
    void apiRegistrationWithoutOtpIsRejected() throws Exception {
        doThrow(new IllegalStateException("OTP verification is required before account creation."))
                .when(authService).register(org.mockito.ArgumentMatchers.any());
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Test Member","email":"otp-bypass@example.invalid",
                                 "phone":"0800000001","gender":"Nam","birthDate":"1990-01-01",
                                 "password":"TestPassword123","confirmPassword":"TestPassword123",
                                 "termsAccepted":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("OTP")));

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.times(1))
                .register(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void htmlRegistrationWithoutOtpIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Test Member")
                        .param("email", "otp-bypass@example.invalid")
                        .param("phone", "0800000001")
                        .param("gender", "Nam")
                        .param("birthDate", "1990-01-01")
                        .param("password", "TestPassword123")
                        .param("confirmPassword", "TestPassword123")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.never())
                .register(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankOtpIsRejectedBeforeVerificationOrUserCreation() throws Exception {
        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"blank-otp@example.invalid","otp":""}
                                """))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(otpService, org.mockito.Mockito.never())
                .verifyOtp(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(authService, org.mockito.Mockito.never())
                .createUserAfterOtp(org.mockito.ArgumentMatchers.<com.rexchain.cinema.dto.VerifiedRegistration>any());
    }

    @Test
    void wrongOtpCannotCreateUser() throws Exception {
        String email = "wrong-otp@example.invalid";
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Invalid OTP"))
                .when(otpService).verifyOtp(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.eq("000000"), org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"wrong-otp@example.invalid","otp":"000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("OTP")));

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.never())
                .createUserAfterOtp(org.mockito.ArgumentMatchers.<com.rexchain.cinema.dto.VerifiedRegistration>any());
    }

    @Test
    void otpRateLimitIsReturnedAsTooManyRequests() throws Exception {
        doThrow(new OtpRateLimitException("OTP rate limited"))
                .when(otpService).generateAndSendOtp(org.mockito.ArgumentMatchers.any(RegisterRequest.class),
                        org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/auth/register/send-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Test Member","email":"limited@example.invalid",
                                 "phone":"0800000010","gender":"Nam","birthDate":"1990-01-01",
                                 "password":"TestPassword123","confirmPassword":"TestPassword123",
                                 "termsAccepted":true}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("OTP rate limited"));
    }

    @Test
    void expiredOtpCannotCreateUserOnHtmlRegistrationVerificationRoute() throws Exception {
        String email = "expired-otp@example.invalid";
        org.mockito.Mockito.doThrow(new IllegalArgumentException("OTP expired"))
                .when(otpService).verifyOtp(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.eq("123456"), org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/auth/register/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"expired-otp@example.invalid","otp":"123456"}
                                """))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.never())
                .createUserAfterOtp(org.mockito.ArgumentMatchers.<com.rexchain.cinema.dto.VerifiedRegistration>any());
    }

    @Test
    void replayedOtpProofCreatesAtMostOneUser() throws Exception {
        String email = "replay-otp@example.invalid";
        com.rexchain.cinema.dto.VerifiedRegistration verified = new com.rexchain.cinema.dto.VerifiedRegistration(
                "Test Member", email, "0800000002", "Nam", java.time.LocalDate.of(1990, 1, 1),
                "hashed-password", true, null, null, null);
        org.mockito.Mockito.when(otpService.verifyOtp(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.eq("654321"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(verified)
                .thenThrow(new IllegalArgumentException("OTP already consumed"));

        String request = """
                {"email":"replay-otp@example.invalid","otp":"654321"}
                """;
        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .with(csrf()).contentType("application/json").content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .with(csrf()).contentType("application/json").content(request))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.times(1)).createUserAfterOtp(verified);
    }

    @Test
    void correctOtpOnRegistrationRouteCreatesUserExactlyOnce() throws Exception {
        String email = "correct-otp@example.invalid";
        com.rexchain.cinema.dto.VerifiedRegistration verified = new com.rexchain.cinema.dto.VerifiedRegistration(
                "Test Member", email, "0800000003", "Nam", java.time.LocalDate.of(1990, 1, 1),
                "hashed-password", true, null, null, null);
        org.mockito.Mockito.when(otpService.verifyOtp(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.eq("246810"), org.mockito.ArgumentMatchers.anyString())).thenReturn(verified);

        mockMvc.perform(post("/auth/register/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"correct-otp@example.invalid","otp":"246810"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/login?registered"));

        org.mockito.Mockito.verify(authService, org.mockito.Mockito.times(1)).createUserAfterOtp(verified);
    }

    @Test
    void publicRoleFieldCannotElevateOtpRegistrationToAdmin() throws Exception {
        String email = "role-injection@example.invalid";
        org.mockito.ArgumentCaptor<RegisterRequest> registration =
                org.mockito.ArgumentCaptor.forClass(RegisterRequest.class);

        mockMvc.perform(post("/api/auth/register/send-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Test Member","email":"role-injection@example.invalid",
                                 "phone":"0800000005","gender":"Nam","birthDate":"1990-01-01",
                                 "password":"TestPassword123","confirmPassword":"TestPassword123",
                                 "termsAccepted":true,"role":"ADMIN"}
                                """))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(otpService).generateAndSendOtp(registration.capture(), org.mockito.ArgumentMatchers.anyString());
        RegisterRequest storedRegistration = registration.getValue();
        com.rexchain.cinema.dto.VerifiedRegistration verified = new com.rexchain.cinema.dto.VerifiedRegistration(
                storedRegistration.fullName(), storedRegistration.email(), storedRegistration.phone(),
                storedRegistration.gender(), storedRegistration.birthDate(), "hashed-password",
                storedRegistration.termsAccepted(), storedRegistration.username(), storedRegistration.nationalId(),
                storedRegistration.favoriteCinemaId());
        org.mockito.Mockito.when(otpService.verifyOtp(org.mockito.ArgumentMatchers.eq(email),
                        org.mockito.ArgumentMatchers.eq("135790"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(verified);

        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"email":"role-injection@example.invalid","otp":"135790"}
                                """))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(authService).createUserAfterOtp(verified);
    }
}
