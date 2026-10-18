package com.rexchain.cinema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "[0-9]{6}", message = "Mã OTP phải gồm 6 chữ số")
        String otp
) {}
