package com.rexchain.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(
        @NotBlank(message = "Email không được để trống") String email,
        @NotBlank(message = "Thiếu mã xác nhận khôi phục") String resetToken,
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String newPassword,
        @NotBlank(message = "Vui lòng xác nhận mật khẩu mới") String confirmPassword
) {}
