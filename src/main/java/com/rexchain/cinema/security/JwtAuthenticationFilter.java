package com.rexchain.cinema.security;

import com.rexchain.cinema.service.AccessTokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final CustomUserDetailsService userDetailsService;
    private final AccessTokenRevocationService revocations;

    public JwtAuthenticationFilter(JwtService jwt, CustomUserDetailsService userDetailsService,
                                   AccessTokenRevocationService revocations) {
        this.jwt = jwt;
        this.userDetailsService = userDetailsService;
        this.revocations = revocations;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) token = authorization.substring(7);
        if (token == null && request.getCookies() != null) {
            token = Arrays.stream(request.getCookies()).filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(Cookie::getValue).findFirst().orElse(null);
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null
                && jwt.valid(token) && !revocations.isRevoked(token)) {
            String email = jwt.extractEmail(token);
            UserDetails details = userDetailsService.loadUserForToken(email, jwt.extractCredentialVersion(token));
            if (details != null && details.isEnabled() && details.isAccountNonExpired()
                    && details.isAccountNonLocked() && details.isCredentialsNonExpired()) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        details, null, details.getAuthorities()));
            }
        }
        chain.doFilter(request, response);
    }
}
