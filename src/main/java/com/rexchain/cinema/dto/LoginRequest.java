package com.rexchain.cinema.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Vui lòng nhập tên đăng nhập, số điện thoại hoặc email")
    @JsonAlias({"email", "username", "phone"})
    String identifier,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password
) {}
