package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotFoundAwareAuthenticationEntryPointTest {
    @Test
    void unknownHtmlUrlReturnsNotFoundInsteadOfLoginRedirect() throws Exception {
        RequestMappingHandlerMapping mappings = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no-such-route");
        when(mappings.getHandler(request)).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        new NotFoundAwareAuthenticationEntryPoint(mappings).commence(request, response,
                new InsufficientAuthenticationException("missing authentication"));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    void knownProtectedHtmlUrlRedirectsToLogin() throws Exception {
        RequestMappingHandlerMapping mappings = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        when(mappings.getHandler(request)).thenReturn(new HandlerExecutionChain(new Object()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        new NotFoundAwareAuthenticationEntryPoint(mappings).commence(request, response,
                new InsufficientAuthenticationException("missing authentication"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?returnUrl=%2Fadmin");
    }
}
