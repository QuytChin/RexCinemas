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

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String phone,

        @NotBlank(message = "Vui lòng chọn giới tính")
        String gender,

        @NotNull(message = "Vui lòng chọn ngày sinh")
        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate birthDate,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự")
        String password,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu")
        String confirmPassword,

        @AssertTrue(message = "Bạn cần đồng ý với điều khoản dịch vụ và chính sách bảo mật")
        boolean termsAccepted,

        String username,
        String nationalId,
        Long favoriteCinemaId
) {
    public RegisterRequest(String fullName, String email, String phone, String gender,
                          LocalDate birthDate, String password, String confirmPassword, boolean termsAccepted) {
        this(fullName, email, phone, gender, birthDate, password, confirmPassword, termsAccepted, null, null, null);
    }
}
