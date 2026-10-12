package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserRepository users;
    private final CinemaRepository cinemas;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, CinemaRepository cinemas, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwt) {
        this.users = users;
        this.cinemas = cinemas;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = request.email() == null ? "" : request.email().trim().toLowerCase();
        String normalizedUsername = request.username() == null ? "" : request.username().trim().toLowerCase();
        String nationalId = request.nationalId() == null ? "" : request.nationalId().trim();

        if (request.fullName() == null || request.fullName().trim().length() < 2)
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
        if (!EMAIL.matcher(normalizedEmail).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
        if (request.password() == null || request.password().length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        if (!request.password().equals(request.confirmPassword()))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        if (users.existsByEmailIgnoreCase(normalizedEmail))
            throw new IllegalArgumentException("Email đã được sử dụng");
        if (users.existsByUsernameIgnoreCase(normalizedUsername))
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        if (users.existsByNationalId(nationalId))
            throw new IllegalArgumentException("CCCD/CMND đã được sử dụng");

        Cinema favoriteCinema = cinemas.findById(request.favoriteCinemaId())
                .filter(Cinema::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Rạp yêu thích không hợp lệ"));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(request.phone().trim());
        user.setUsername(normalizedUsername);
        user.setBirthDate(request.birthDate());
        user.setNationalId(nationalId);
        user.setFavoriteCinema(favoriteCinema);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Email, tên đăng nhập hoặc CCCD/CMND đã được sử dụng", ex);
        }
    }

    public String login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        User user = users.findByEmailIgnoreCase(normalizedEmail).orElseThrow();
        return jwt.generate(user.getEmail(), user.getRole().name());
    }
}
