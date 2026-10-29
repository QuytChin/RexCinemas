package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwt = mock(JwtService.class);
    private final LoginRateLimiter loginRateLimiter = new LoginRateLimiter(5, 100, 900, 1000, java.time.Clock.systemUTC());
    private final AuthService service = new AuthService(users, mock(CinemaRepository.class), mock(PasswordEncoder.class),
            authenticationManager, jwt, loginRateLimiter);

    @org.junit.jupiter.api.Test
    void directRegistrationIsRejectedWithoutDatabaseWrites() {
        RegisterRequest request = new RegisterRequest("Test Member", "direct@example.invalid", "0800000001",
                "Nam", java.time.LocalDate.of(1990, 1, 1), "TestPassword123", "TestPassword123", true);

        assertThrows(IllegalStateException.class, () -> service.register(request));
        verify(users, never()).save(any());
        verify(users, never()).saveAndFlush(any());
    }

    @org.junit.jupiter.api.Test
    void otpRegistrationAlwaysCreatesCustomer() {
        RegisterRequest request = new RegisterRequest("Test Member", "customer@example.invalid", "0800000004",
                "Nam", java.time.LocalDate.of(1990, 1, 1), "TestPassword123", "TestPassword123", true,
                "test_member", null, null);
        when(users.existsByEmailIgnoreCase("customer@example.invalid")).thenReturn(false);
        when(users.existsByPhone("0800000004")).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("test_member")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.createUserAfterOtp(request);

        assertSame(Role.CUSTOMER, created.getRole());
        verify(users, times(1)).saveAndFlush(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"member@rexchain.vn", "rexmember", "0901234567"})
    void loginAcceptsEmailUsernameOrPhoneWhenIdentifierResolvesToOneUser(String identifier) {
        User user = user();
        when(users.findAllByLoginIdentifier(identifier)).thenReturn(List.of(user));
        when(jwt.generate(user.getEmail(), user.getRole().name(), false, user.getCredentialVersion())).thenReturn("token");

        assertEquals("token", service.login(identifier, "secret"));
        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken token
                        && "member@rexchain.vn".equals(token.getPrincipal())
                        && "secret".equals(token.getCredentials())));
    }

    @org.junit.jupiter.api.Test
    void loginRejectsAmbiguousIdentifierWithoutAuthenticatingEitherAccount() {
        when(users.findAllByLoginIdentifier("shared")).thenReturn(List.of(user(), user()));

        assertThrows(BadCredentialsException.class, () -> service.login("shared", "secret"));
        verify(authenticationManager, never()).authenticate(any());
        verify(jwt, never()).generate(any(), any(), anyBoolean(), anyInt());
    }

    @org.junit.jupiter.api.Test
    void unknownIdentifierAndWrongPasswordHaveTheSameFailure() {
        when(users.findAllByLoginIdentifier("missing")).thenReturn(List.of());
        User known = user();
        when(users.findAllByLoginIdentifier("rexmember")).thenReturn(List.of(known));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad password"));

        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> service.login("missing", "wrong", false, "192.0.2.20"));
        BadCredentialsException wrongPassword = assertThrows(BadCredentialsException.class,
                () -> service.login("rexmember", "wrong", false, "192.0.2.20"));

        assertEquals(unknown.getMessage(), wrongPassword.getMessage());
        assertEquals("Invalid credentials", unknown.getMessage());
    }

    @org.junit.jupiter.api.Test
    void failedAttemptsAreLimitedBeforeFurtherLookupOrPasswordVerification() {
        when(users.findAllByLoginIdentifier("member@rexchain.vn")).thenReturn(List.of(user()));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad password"));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("member@rexchain.vn", "wrong", false, "192.0.2.21"));
        }
        LoginRateLimitException limited = assertThrows(LoginRateLimitException.class,
                () -> service.login("member@rexchain.vn", "wrong", false, "192.0.2.21"));

        assertEquals(900, limited.getRetryAfterSeconds());
        verify(users, times(6)).findAllByLoginIdentifier("member@rexchain.vn");
        verify(authenticationManager, times(5)).authenticate(any());
    }

    @org.junit.jupiter.api.Test
    void emailAndUsernameAliasesShareTheSameAccountLimit() {
        User account = user();
        account.setId(77L);
        when(users.findAllByLoginIdentifier("member@rexchain.vn")).thenReturn(List.of(account));
        when(users.findAllByLoginIdentifier("rexmember")).thenReturn(List.of(account));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad password"));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("member@rexchain.vn", "wrong", false, "192.0.2.22"));
        }
        assertThrows(LoginRateLimitException.class,
                () -> service.login("rexmember", "wrong", false, "192.0.2.22"));

        verify(authenticationManager, times(5)).authenticate(any());
    }

    @org.junit.jupiter.api.Test
    void ipLimitRejectsBeforeLookingUpAnotherAccount() {
        UserRepository testUsers = mock(UserRepository.class);
        AuthenticationManager testAuthenticationManager = mock(AuthenticationManager.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        LoginRateLimiter limiter = new LoginRateLimiter(5, 1, 900, 100, java.time.Clock.systemUTC());
        AuthService limitedService = new AuthService(testUsers, mock(CinemaRepository.class), encoder,
                testAuthenticationManager, mock(JwtService.class), limiter);
        when(testUsers.findAllByLoginIdentifier("first@example.invalid")).thenReturn(List.of(user()));
        when(testAuthenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("wrong"));

        assertThrows(BadCredentialsException.class,
                () -> limitedService.login("first@example.invalid", "wrong", false, "192.0.2.23"));
        assertThrows(LoginRateLimitException.class,
                () -> limitedService.login("second@example.invalid", "wrong", false, "192.0.2.23"));

        verify(testUsers, never()).findAllByLoginIdentifier("second@example.invalid");
        verify(testAuthenticationManager, times(1)).authenticate(any());
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setEmail("member@rexchain.vn");
        user.setUsername("rexmember");
        user.setPhone("0901234567");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
