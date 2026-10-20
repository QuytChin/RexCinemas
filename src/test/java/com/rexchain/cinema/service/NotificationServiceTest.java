package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.AccountNotification;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.AccountNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock AccountNotificationRepository notifications;
    @InjectMocks NotificationService service;

    @Test
    void markReadOnlyLoadsNotificationForTheCurrentUserAndIsIdempotent() {
        AccountNotification notification = new AccountNotification();
        User owner = new User(); owner.setId(10L); notification.setUser(owner);
        when(notifications.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(notification));

        assertThat(service.markRead(5L, 10L)).isTrue();
        assertThat(notification.isRead()).isTrue();
        assertThat(service.markRead(5L, 99L)).isFalse();
        verify(notifications).findByIdAndUserId(5L, 10L);
        verify(notifications).findByIdAndUserId(5L, 99L);
    }

    @Test
    void markAllReadScopesTheBulkUpdateToTheCurrentUser() {
        when(notifications.markAllReadByUserId(eq(10L), any())).thenReturn(3);

        assertThat(service.markAllRead(10L)).isEqualTo(3);
        verify(notifications).markAllReadByUserId(eq(10L), any());
        verifyNoMoreInteractions(notifications);
    }
}
