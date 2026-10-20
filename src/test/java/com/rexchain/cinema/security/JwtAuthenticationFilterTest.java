package com.rexchain.cinema.security;

import com.rexchain.cinema.service.AccessTokenRevocationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService(UUID.randomUUID().toString() + UUID.randomUUID(), 60_000, 120_000);
    private final CustomUserDetailsService details = mock(CustomUserDetailsService.class);
    private final AccessTokenRevocationService revocations = mock(AccessTokenRevocationService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, details, revocations);

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void validatesAccountAndCredentialVersionInOneUserLookup() throws Exception {
        String email = "member@example.invalid";
        String token = jwt.generate(email, "CUSTOMER", false, 2);
        when(details.loadUserForToken(email, 2)).thenReturn(null);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(details).loadUserForToken(email, 2);
    }

    @Test
    void rejectsSingleRevokedTokenWithoutLoadingAccount() throws Exception {
        String token = jwt.generate("member@example.invalid", "CUSTOMER", false, 0);
        when(revocations.isRevoked(token)).thenReturn(true);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(details);
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
