package com.rexchain.cinema.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record HoldRequest(
    @NotEmpty(message = "Danh sách ghế giữ không được để trống")
    List<Long> seatIds
) {}
