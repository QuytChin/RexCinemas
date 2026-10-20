package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AccountExperienceControllerTest {
    @Test
    void readEndpointsReturnPersistedUnreadCountForAuthenticatedUser() {
        CinemaRepository cinemas = mock(CinemaRepository.class);
        UserRepository users = mock(UserRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        AccountExperienceController controller = new AccountExperienceController(cinemas, users, notifications);
        User user = new User(); user.setId(10L); user.setEmail("rex_test_notification@example.invalid");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(user.getEmail(), null);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(notifications.markRead(5L, 10L)).thenReturn(true);
        when(notifications.unreadCount(10L)).thenReturn(2L, 0L);

        ResponseEntity<?> one = controller.markNotificationRead(5L, authentication);
        ResponseEntity<?> all = controller.markAllNotificationsRead(authentication);

        assertThat(one.getStatusCode().value()).isEqualTo(200);
        assertThat(one.getBody()).isEqualTo(Map.of("unreadCount", 2L));
        assertThat(all.getStatusCode().value()).isEqualTo(200);
        assertThat(all.getBody()).isEqualTo(Map.of("unreadCount", 0L));
        verify(notifications).markAllRead(10L);
        verify(notifications, times(2)).unreadCount(10L);
    }

    @Test
    void otherUsersNotificationIsNotReportedAsRead() {
        UserRepository users = mock(UserRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        AccountExperienceController controller = new AccountExperienceController(mock(CinemaRepository.class), users, notifications);
        User user = new User(); user.setId(10L); user.setEmail("rex_test_notification@example.invalid");
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(notifications.markRead(6L, 10L)).thenReturn(false);

        assertThat(controller.markNotificationRead(6L, new TestingAuthenticationToken(user.getEmail(), null)).getStatusCode().value())
                .isEqualTo(404);
        verify(notifications).markRead(6L, 10L);
        verify(notifications, never()).unreadCount(any());
    }
}
