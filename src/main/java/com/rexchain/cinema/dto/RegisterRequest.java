package com.rexchain.cinema.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(min = 2, max = 120, message = "Họ và tên phải từ 2 đến 120 ký tự")
        String fullName,

        @NotNull(message = "Vui lòng chọn ngày sinh")
        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate birthDate,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String phone,

        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Pattern(regexp = "[A-Za-z0-9._]{4,30}", message = "Tên đăng nhập dài 4-30 ký tự, chỉ gồm chữ, số, dấu chấm hoặc gạch dưới")
        String username,

        @NotBlank(message = "CCCD/CMND không được để trống")
        @Pattern(regexp = "(?:[0-9]{9}|[0-9]{12})", message = "CCCD/CMND phải gồm 9 hoặc 12 chữ số")
        String nationalId,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        @NotNull(message = "Vui lòng chọn rạp yêu thích")
        Long favoriteCinemaId,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự")
        String password,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu")
        String confirmPassword,

        @AssertTrue(message = "Bạn cần đồng ý với điều khoản và chính sách bảo mật")
        boolean termsAccepted
) {}
