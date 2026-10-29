package com.rexchain.cinema.dto;

import java.math.BigDecimal;

public record MovieOccupancyDto(
        Long movieId,
        String movieTitle,
        String posterUrl,
        int durationMinutes,
        String ageRating,
        long showtimeCount,
        long totalCapacity,
        long soldTickets,
        BigDecimal revenue,
        double occupancyRate,
        String peakHourSlot,
        String recommendation,
        String badgeClass
) {}

