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
import java.time.LocalDate;
import java.util.List;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("0[0-9]{9}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._]{4,30}");
    private static final Pattern NATIONAL_ID = Pattern.compile("(?:[0-9]{9}|[0-9]{12})");
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
        String phone = request.phone() == null ? "" : request.phone().trim();
        String nationalId = request.nationalId() == null ? "" : request.nationalId().trim();

        if (request.fullName() == null || request.fullName().trim().length() < 2)
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
        if (!EMAIL.matcher(normalizedEmail).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
        if (request.birthDate() == null || !request.birthDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày sinh phải là một ngày trong quá khứ");
        if (!PHONE.matcher(phone).matches())
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0");
        if (!USERNAME.matcher(normalizedUsername).matches())
            throw new IllegalArgumentException("Tên đăng nhập dài 4-30 ký tự, chỉ gồm chữ, số, dấu chấm hoặc gạch dưới");
        if (!NATIONAL_ID.matcher(nationalId).matches())
            throw new IllegalArgumentException("CCCD/CMND phải gồm 9 hoặc 12 chữ số");
        if (request.password() == null || request.password().length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        if (!request.password().equals(request.confirmPassword()))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        if (users.existsByEmailIgnoreCase(normalizedEmail))
            throw new IllegalArgumentException("Email đã được sử dụng");
        if (users.existsByUsernameIgnoreCase(normalizedUsername))
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        if (users.existsByPhone(phone))
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        if (users.existsByNationalId(nationalId))
            throw new IllegalArgumentException("CCCD/CMND đã được sử dụng");

        Cinema favoriteCinema = cinemas.findById(request.favoriteCinemaId())
                .filter(Cinema::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Rạp yêu thích không hợp lệ"));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone);
        user.setUsername(normalizedUsername);
        user.setBirthDate(request.birthDate());
        user.setNationalId(nationalId);
        user.setFavoriteCinema(favoriteCinema);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Email, tên đăng nhập, số điện thoại hoặc CCCD/CMND đã được sử dụng", ex);
        }
    }

    public String login(String identifier, String password) {
        return login(identifier, password, false);
    }

    public String login(String identifier, String password, boolean remember) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase();
        List<User> matches = users.findAllByLoginIdentifier(normalizedIdentifier);
        if (matches.size() != 1) throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        User user = matches.getFirst();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), password));
        return jwt.generate(user.getEmail(), user.getRole().name(), remember);
    }
}
