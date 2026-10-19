package com.rexchain.cinema.dto;

import java.util.List;

public record BookingQuoteRequest(List<Long> seatIds, List<Long> comboIds, List<Integer> comboQtys, String voucherCode) {}
