package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {
    @Test
    void allowsOnlyConfiguredAndLocalOrigins() {
        WebSocketConfig config = new WebSocketConfig("https://cinema.example", "https://public.example/path");
        assertThat(config.allowedOrigins()).contains("http://localhost:8080", "https://cinema.example", "https://public.example")
                .doesNotContain("*");
    }

    @Test
    void rejectsClientSendsAndNonSeatSubscriptions() {
        WebSocketConfig config = new WebSocketConfig("", "");
        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/showtimes/12/seats");
        assertThatThrownBy(() -> config.validateInbound(StompCommand.SEND, "/app/anything"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> config.validateInbound(StompCommand.SUBSCRIBE, "/topic/private"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anonymousClientCannotSubscribeToAnotherAccountsSupportSession() {
        WebSocketConfig config = new WebSocketConfig("", "");
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/session/user_5"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/admin"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyTheOwningAccountMaySubscribeToItsSupportSession() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        User owner = user(5L, Role.CUSTOMER, null);
        User intruder = user(9L, Role.CUSTOMER, null);
        org.mockito.Mockito.when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(java.util.Optional.of(owner));
        org.mockito.Mockito.when(users.findByEmailIgnoreCase("intruder@example.com")).thenReturn(java.util.Optional.of(intruder));
        WebSocketConfig config = new WebSocketConfig("", "", users);

        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/support/session/user_5", principal("owner@example.com"));
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/session/user_5", principal("intruder@example.com")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void supportAdminTopicsRequireAnAdminOrTheAssignedCinemaSupervisor() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        Cinema cinema = new Cinema();
        cinema.setId(3L);
        User admin = user(1L, Role.ADMIN, null);
        User supervisor = user(2L, Role.SUPERVISOR, cinema);
        User customer = user(5L, Role.CUSTOMER, null);
        org.mockito.Mockito.when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(java.util.Optional.of(admin));
        org.mockito.Mockito.when(users.findByEmailIgnoreCase("sup@example.com")).thenReturn(java.util.Optional.of(supervisor));
        org.mockito.Mockito.when(users.findByEmailIgnoreCase("customer@example.com")).thenReturn(java.util.Optional.of(customer));
        WebSocketConfig config = new WebSocketConfig("", "", users);

        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/support/admin", principal("admin@example.com"));
        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/support/cinema/3", principal("sup@example.com"));
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/cinema/4", principal("sup@example.com")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/admin", principal("customer@example.com")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void publicSeatMapAndEphemeralGuestChannelsStayReachableWithoutLogin() {
        WebSocketConfig config = new WebSocketConfig("", "");
        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/showtimes/12/seats");
        config.validateInbound(StompCommand.SUBSCRIBE, "/topic/support/session/guest_ab12cd3");
        assertThatThrownBy(() -> config.validateInbound(
                StompCommand.SUBSCRIBE, "/topic/support/session/system"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static User user(Long id, Role role, Cinema cinema) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setCinema(cinema);
        return user;
    }

    private static java.security.Principal principal(String email) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                email, "n/a", java.util.List.of());
    }
}
