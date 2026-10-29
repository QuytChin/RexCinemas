package com.rexchain.cinema.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoginClientIpResolverTest {
    @Test
    void usesSocketPeerInsteadOfForwardedAddressFromRequest() {
        MockHttpServletRequest peer = new MockHttpServletRequest();
        peer.setRemoteAddr("192.0.2.44");
        peer.addHeader("X-Forwarded-For", "198.51.100.99");
        HttpServletRequest forwardedWrapper = new HttpServletRequestWrapper(peer) {
            @Override public String getRemoteAddr() { return "198.51.100.99"; }
        };

        assertThat(LoginClientIpResolver.resolve(forwardedWrapper)).isEqualTo("192.0.2.44");
    }

    @Test
    void followsForwardedChainOnlyWhenSocketPeerIsExplicitlyTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.50");
        request.addHeader("X-Forwarded-For", "203.0.113.77, 192.0.2.51");

        assertThat(LoginClientIpResolver.resolve(request, "192.0.2.50,192.0.2.51"))
                .isEqualTo("203.0.113.77");
        assertThat(LoginClientIpResolver.resolve(request, "192.0.2.51"))
                .isEqualTo("192.0.2.50");
    }
}
