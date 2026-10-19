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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(users, mock(CinemaRepository.class), mock(PasswordEncoder.class), authenticationManager, jwt);

    @org.junit.jupiter.api.Test
    void directRegistrationIsRejectedWithoutDatabaseWrites() {
        RegisterRequest request = new RegisterRequest("Test Member", "direct@example.invalid", "0800000001",
                "Nam", java.time.LocalDate.of(1990, 1, 1), "TestPassword123", "TestPassword123", true);

        assertThrows(IllegalStateException.class, () -> service.register(request));
        verify(users, never()).save(any());
        verify(users, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"member@rexchain.vn", "rexmember", "0901234567"})
    void loginAcceptsEmailUsernameOrPhoneWhenIdentifierResolvesToOneUser(String identifier) {
        User user = user();
        when(users.findAllByLoginIdentifier(identifier)).thenReturn(List.of(user));
        when(jwt.generate(user.getEmail(), user.getRole().name(), false)).thenReturn("token");

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
        verify(jwt, never()).generate(any(), any(), anyBoolean());
    }

    private User user() {
        User user = new User();
        user.setEmail("member@rexchain.vn");
        user.setUsername("rexmember");
        user.setPhone("0901234567");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
