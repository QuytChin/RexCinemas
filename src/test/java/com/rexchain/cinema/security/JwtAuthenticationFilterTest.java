package com.rexchain.cinema.security;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService(UUID.randomUUID().toString() + UUID.randomUUID(), 60_000, 120_000);
    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService details = mock(CustomUserDetailsService.class);
    private final AccessTokenRevocationService revocations = mock(AccessTokenRevocationService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, details, users, revocations);

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsTokenIssuedBeforeCredentialVersionChanged() throws Exception {
        User user = user(3);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        String oldToken = jwt.generate(user.getEmail(), "CUSTOMER", false, 2);

        filter.doFilter(request(oldToken), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(details);
    }

    @Test
    void rejectsSingleRevokedTokenWithoutLoadingUserDetails() throws Exception {
        String token = jwt.generate("member@example.invalid", "CUSTOMER", false, 0);
        when(revocations.isRevoked(token)).thenReturn(true);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(users, details);
    }

    private User user(int credentialVersion) {
        User user = new User();
        user.setEmail("member@example.invalid");
        user.setCredentialVersion(credentialVersion);
        return user;
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
