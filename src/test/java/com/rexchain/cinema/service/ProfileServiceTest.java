package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordChangeRateLimiter limiter = new PasswordChangeRateLimiter();
    private final ProfileService service = new ProfileService(users, encoder, limiter);

    @Test
    void passwordChangeIncrementsCredentialVersionToRevokeExistingSessions() {
        User user = new User();
        user.setId(42L);
        user.setPassword("old-hash");
        user.setCredentialVersion(4);
        when(encoder.matches("OldPassword1", "old-hash")).thenReturn(true);
        when(encoder.matches("NewPassword2", "old-hash")).thenReturn(false);
        when(encoder.encode("NewPassword2")).thenReturn("new-hash");

        service.changePassword(user, "OldPassword1", "NewPassword2", "NewPassword2");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getCredentialVersion()).isEqualTo(5);
        verify(users).save(user);
    }
}
