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
                                    @Value("${app.cookie.secure:true}") boolean secureCookie) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").path("/").secure(secureCookie));
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);
        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/api/auth/**", "/payment/callback/**", "/api/payments/sepay/webhook", "/ws/**")
                .ignoringRequestMatchers(request -> {
                    String authorization = request.getHeader("Authorization");
                    return authorization != null && authorization.startsWith("Bearer ");
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/login", "/register", "/policies", "/auth/**",
                                "/css/**", "/js/**", "/images/**", "/ws/**",
                                "/api/auth/**", "/payment/callback/**", "/api/payments/sepay/webhook",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/movies/**", "/schedule", "/articles/**",
                                "/api/movies/**", "/api/showtimes/*/seats", "/api/snacks").permitAll()
                        .requestMatchers("/admin/users/**", "/admin/payments/**", "/admin/transactions/**", "/admin/late-payments/**",
                                "/admin/bookings/*/refund").hasRole("ADMIN")
                        .requestMatchers("/admin/rewards/**").hasRole("ADMIN")
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
