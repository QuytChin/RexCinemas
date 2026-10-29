package com.rexchain.cinema.config;

import com.rexchain.cinema.security.CustomUserDetailsService;
import com.rexchain.cinema.security.CsrfCookieFilter;
import com.rexchain.cinema.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    NotFoundAwareAuthenticationEntryPoint notFoundAwareAuthenticationEntryPoint(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping requestMappings) {
        return new NotFoundAwareAuthenticationEntryPoint(requestMappings);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwt,
                                    AuthenticationProvider authenticationProvider,
                                    NotFoundAwareAuthenticationEntryPoint authenticationEntryPoint,
                                    com.rexchain.cinema.security.JwtService jwtService,
                                    com.rexchain.cinema.service.AccessTokenRevocationService revocations,
                                    @Value("${app.cookie.secure:true}") boolean secureCookie) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").path("/").secure(secureCookie));
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);
        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/api/auth/**", "/payment/callback/**", "/api/payments/sepay/webhook", "/ws/**")
                // Only a verified, non-revoked bearer token may bypass CSRF; the header
                // alone is attacker-controlled and must not disable the protection.
                .ignoringRequestMatchers(request -> {
                    String authorization = request.getHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) return false;
                    String token = authorization.substring("Bearer ".length()).trim();
                    return !token.isEmpty() && jwtService.valid(token) && !revocations.isRevoked(token);
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/login", "/register", "/policies", "/auth/**",
                                "/css/**", "/js/**", "/images/**", "/ws/**",
                                "/api/auth/**", "/payment/callback/**", "/api/payments/sepay/webhook",
                                "/api/support/**",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/movies/**", "/schedule", "/articles/**",
                                "/api/movies/**", "/api/showtimes/*/seats", "/api/snacks").permitAll()
                        // Staff are redirected to check-in from the admin landing page; all
                        // other admin actions require an explicit role-specific rule below.
                        .requestMatchers("/admin", "/admin/").hasAnyRole("ADMIN", "CINEMA_MANAGER", "SUPERVISOR", "STAFF")
                        // Chain managers may inspect operational lists and reports only.
                        // Keep write methods and edit/new routes under the rules below.
                        .requestMatchers(HttpMethod.GET,
                                "/admin/movies", "/admin/cinemas", "/admin/auditoriums",
                                "/admin/transactions", "/admin/transactions/*")
                                .hasAnyRole("ADMIN", "CINEMA_MANAGER")
                        .requestMatchers(HttpMethod.GET,
                                "/admin/showtimes", "/admin/showtimes/api/occupancy", "/admin/bookings")
                                .hasAnyRole("ADMIN", "CINEMA_MANAGER", "SUPERVISOR")
                        .requestMatchers("/admin/checkin/**").hasAnyRole("ADMIN", "SUPERVISOR", "STAFF")
                        .requestMatchers("/admin/support/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/admin/bookings/*/refund").hasRole("ADMIN")
                        .requestMatchers("/admin/bookings/**", "/admin/showtimes/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/admin/users/**").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/admin/payments/**", "/admin/late-payments/**", "/admin/rewards/**").hasRole("ADMIN")
                        .requestMatchers("/admin/transactions/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                                "form-action 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; connect-src 'self' ws: wss:; " +
                                "frame-src https://www.youtube.com https://www.youtube-nocookie.com"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authenticationProvider(authenticationProvider)
                // Login is handled by AuthController so it can issue the existing JWT cookie.
                .formLogin(form -> form.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.disable())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }
}
