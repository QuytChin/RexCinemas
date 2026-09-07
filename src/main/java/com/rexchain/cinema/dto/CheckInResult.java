package com.rexchain.cinema.dto;

public record CheckInResult(
        boolean success,
        String status,
        String message,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String auditoriumName,
        String showtime,
        String seats,
        String customerName,
        String checkedInAt,
        String checkedInBy
) {}
