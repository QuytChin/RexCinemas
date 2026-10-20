package com.rexchain.cinema.security;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).disabled(!user.isEnabled()).build();
    }

    /** Load the account once while also checking the JWT's revocable credential version. */
    public UserDetails loadUserForToken(String email, int credentialVersion) {
        return users.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .filter(user -> user.getCredentialVersion() == credentialVersion)
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPassword()).roles(user.getRole().name()).build())
                .orElse(null);
    }
}
