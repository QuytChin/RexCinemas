package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.dto.VerifiedRegistration;
import com.rexchain.cinema.entity.Cinema;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
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
    private final LoginRateLimiter loginRateLimiter;
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, CinemaRepository cinemas, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwt,
                       LoginRateLimiter loginRateLimiter) {
        this.users = users;
        this.cinemas = cinemas;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.loginRateLimiter = loginRateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    public void validateRegistrationData(RegisterRequest request) {
        String normalizedEmail = request.email() == null ? "" : request.email().trim().toLowerCase();
        String phone = request.phone() == null ? "" : request.phone().trim();

        if (request.fullName() == null || request.fullName().trim().length() < 2)
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
        if (!EMAIL.matcher(normalizedEmail).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
        if (request.gender() == null || request.gender().trim().isBlank())
            throw new IllegalArgumentException("Vui lòng chọn giới tính");
        if (request.birthDate() == null || !request.birthDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày sinh phải là một ngày trong quá khứ");
        if (!PHONE.matcher(phone).matches())
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0");
        if (request.password() == null || request.password().length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        if (!request.password().equals(request.confirmPassword()))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        if (users.existsByEmailIgnoreCase(normalizedEmail))
            throw new IllegalArgumentException("Email đã được sử dụng");
        if (users.existsByPhone(phone))
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");

        // Optional fields validation
        if (request.username() != null && !request.username().trim().isBlank()) {
            String normalizedUsername = request.username().trim().toLowerCase();
            if (!USERNAME.matcher(normalizedUsername).matches())
                throw new IllegalArgumentException("Tên đăng nhập dài 4-30 ký tự, chỉ gồm chữ, số, dấu chấm hoặc gạch dưới");
            if (users.existsByUsernameIgnoreCase(normalizedUsername))
                throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        }
        if (request.nationalId() != null && !request.nationalId().trim().isBlank()) {
            String nationalId = request.nationalId().trim();
            if (!NATIONAL_ID.matcher(nationalId).matches())
                throw new IllegalArgumentException("CCCD/CMND phải gồm 9 hoặc 12 chữ số");
            if (users.existsByNationalId(nationalId))
                throw new IllegalArgumentException("CCCD/CMND đã được sử dụng");
        }
    }

    /** Direct registration is disabled; accounts are created only after OTP verification. */
    public void register(RegisterRequest request) {
        throw new IllegalStateException("OTP verification is required before account creation.");
    }

    @Transactional
    public User createUserAfterOtp(RegisterRequest request) {
        validateRegistrationData(request);
        return createUser(request, passwordEncoder.encode(request.password()));
    }

    @Transactional
    public User createUserAfterOtp(VerifiedRegistration verified) {
        RegisterRequest validation = verified.asValidationRequest();
        validateRegistrationData(validation);
        return createUser(validation, verified.passwordHash());
    }

    private User createUser(RegisterRequest request, String encodedPassword) {

        String normalizedEmail = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        // Tự động sinh username nếu không có
        String username = request.username();
        if (username == null || username.trim().isBlank()) {
            String candidate = normalizedEmail.split("@")[0].replaceAll("[^A-Za-z0-9._]", "");
            if (candidate.length() < 4) candidate = "user_" + candidate;
            if (candidate.length() > 25) candidate = candidate.substring(0, 25);
            if (users.existsByUsernameIgnoreCase(candidate)) {
                candidate = candidate + "_" + (System.currentTimeMillis() % 10000);
            }
            username = candidate;
        } else {
            username = username.trim().toLowerCase();
        }

        Cinema favoriteCinema = null;
        if (request.favoriteCinemaId() != null) {
            favoriteCinema = cinemas.findById(request.favoriteCinemaId())
                    .filter(Cinema::isActive)
                    .orElse(null);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone);
        user.setGender(request.gender() == null ? "" : request.gender().trim());
        user.setUsername(username);
        user.setBirthDate(request.birthDate());
        if (request.nationalId() != null && !request.nationalId().trim().isBlank()) {
            user.setNationalId(request.nationalId().trim());
        }
        user.setFavoriteCinema(favoriteCinema);
        user.setPassword(encodedPassword);
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Email, số điện thoại hoặc tên đăng nhập đã được sử dụng", ex);
        }
    }

    public String login(String identifier, String password) {
        return login(identifier, password, false, "unknown");
    }

    public String login(String identifier, String password, boolean remember) {
        return login(identifier, password, remember, "unknown");
    }

    public String login(String identifier, String password, boolean remember, String clientIp) {
        LoginRateLimiter.IpReservation ipReservation = loginRateLimiter.begin(clientIp);
        LoginRateLimiter.Attempt attempt = null;
        boolean completed = false;
        try {
            String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase(java.util.Locale.ROOT);
            List<User> matches = users.findAllByLoginIdentifier(normalizedIdentifier);
            String throttleIdentity = matches.size() == 1
                    ? "account:" + matches.get(0).getId()
                    : "identifier:" + normalizedIdentifier;
            attempt = loginRateLimiter.attach(throttleIdentity, ipReservation);
            if (matches.size() != 1) {
                passwordEncoder.matches(password == null ? "" : password, dummyPasswordHash);
                loginRateLimiter.failed(attempt);
                completed = true;
                throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
            }
            User user = matches.get(0);
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), password));
            } catch (AuthenticationException failure) {
                loginRateLimiter.failed(attempt);
                completed = true;
                throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
            }
            String token = jwt.generate(user.getEmail(), user.getRole().name(), remember, user.getCredentialVersion());
            loginRateLimiter.succeeded(attempt);
            completed = true;
            return token;
        } finally {
            if (!completed) {
                if (attempt == null) loginRateLimiter.cancelled(ipReservation);
                else loginRateLimiter.cancelled(attempt);
            }
        }
    }
}
