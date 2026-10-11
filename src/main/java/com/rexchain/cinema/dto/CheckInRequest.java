package com.rexchain.cinema.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(
    @NotBlank(message = "Mã check-in hoặc mã QR không được để trống")
    String code
) {}
