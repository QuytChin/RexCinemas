package com.rexchain.cinema.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Lets MVC return a real 404 for unknown URLs before applying the usual
 * unauthenticated-page redirect policy. Security filters otherwise run first
 * and would redirect an arbitrary typo to the login screen.
 */
public class NotFoundAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final RequestMappingHandlerMapping requestMappings;

    public NotFoundAwareAuthenticationEntryPoint(RequestMappingHandlerMapping requestMappings) {
        this.requestMappings = requestMappings;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        try {
            if (requestMappings.getHandler(request) == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (Exception ignored) {
            // Fall through to the normal authentication response. The request
            // may be a valid mapping whose lookup raised an infrastructure error.
        }

        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String target = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            target += "?" + request.getQueryString();
        }
        response.sendRedirect("/login?returnUrl=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
    }
}
