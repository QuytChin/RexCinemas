package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
    }

    public void register(String fullName, String email, String phone, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (fullName == null || fullName.trim().length() < 2)
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
        if (!EMAIL.matcher(normalizedEmail).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        if (users.existsByEmailIgnoreCase(normalizedEmail))
            throw new IllegalArgumentException("Email đã được sử dụng");

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone == null ? null : phone.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.CUSTOMER);
        users.save(user);
    }

    public String login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        User user = users.findByEmailIgnoreCase(normalizedEmail).orElseThrow();
        return jwt.generate(user.getEmail(), user.getRole().name());
    }
}
