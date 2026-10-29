package com.rexchain.cinema.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

/** Resolves the socket peer and deliberately ignores client-supplied forwarding headers. */
public final class LoginClientIpResolver {
    private LoginClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        return resolve(request, "");
    }

    public static String resolve(HttpServletRequest request, String trustedProxyIps) {
        if (request == null) return "unknown";
        ServletRequest current = request;
        while (current instanceof ServletRequestWrapper wrapper && wrapper.getRequest() != current) {
            current = wrapper.getRequest();
        }
        if (current instanceof HttpServletRequest peerRequest) {
            String peerAddress = peerRequest.getRemoteAddr();
            if (peerAddress == null || peerAddress.isBlank()) return "unknown";

            java.util.Set<String> trusted = parseTrustedIps(trustedProxyIps);
            if (!trusted.contains(peerAddress)) return peerAddress;
            String forwardedFor = peerRequest.getHeader("X-Forwarded-For");
            if (forwardedFor == null || forwardedFor.isBlank()) return peerAddress;

            String[] chain = forwardedFor.split(",");
            String candidate = peerAddress;
            for (int i = chain.length - 1; i >= 0 && trusted.contains(candidate); i--) {
                String forwardedAddress = chain[i].trim();
                if (forwardedAddress.isEmpty()) break;
                candidate = forwardedAddress;
            }
            return candidate;
        }
        return "unknown";
    }

    private static java.util.Set<String> parseTrustedIps(String configured) {
        if (configured == null || configured.isBlank()) return java.util.Set.of();
        return java.util.Arrays.stream(configured.split(","))
                .map(String::trim).filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
