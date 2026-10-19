package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class ProfileService {
    private final UserRepository users; private final PasswordEncoder encoder; private final PasswordChangeRateLimiter limiter;
    public ProfileService(UserRepository users, PasswordEncoder encoder, PasswordChangeRateLimiter limiter) { this.users = users; this.encoder = encoder; this.limiter = limiter; }
    @Transactional
    public void update(User user, String fullName, String phone, String address) {
        String name = required(fullName, "Họ và tên", 120);
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (!normalizedPhone.isEmpty() && !normalizedPhone.matches("^[0-9+() .-]{8,30}$")) throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        if (!normalizedPhone.isEmpty() && users.existsByPhone(normalizedPhone)
                && !normalizedPhone.equals(user.getPhone())) throw new IllegalArgumentException("Số điện thoại đã được dùng bởi tài khoản khác");
        String normalizedAddress = address == null ? "" : address.trim();
        if (normalizedAddress.length() > 300) throw new IllegalArgumentException("Địa chỉ tối đa 300 ký tự");
        user.setFullName(name); user.setPhone(normalizedPhone.isEmpty() ? null : normalizedPhone);
        user.setAddress(normalizedAddress.isEmpty() ? null : normalizedAddress); users.save(user);
    }
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        limiter.requireAllowed(user.getId());
        try {
            if (blank(currentPassword) || !encoder.matches(currentPassword, user.getPassword())) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
            if (blank(newPassword) || newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*"))
                throw new IllegalArgumentException("Mật khẩu mới cần ít nhất 8 ký tự, gồm chữ và số");
            if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("Xác nhận mật khẩu mới chưa khớp");
            if (encoder.matches(newPassword, user.getPassword())) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
            user.setPassword(encoder.encode(newPassword)); user.setCredentialVersion(user.getCredentialVersion() + 1); users.save(user); limiter.succeed(user.getId());
        } catch (IllegalArgumentException ex) { limiter.fail(user.getId()); throw ex; }
    }
    private String required(String value, String label, int limit) { String result = value == null ? "" : value.trim(); if (result.length() < 2 || result.length() > limit) throw new IllegalArgumentException(label + " phải có từ 2 đến " + limit + " ký tự"); return result; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
