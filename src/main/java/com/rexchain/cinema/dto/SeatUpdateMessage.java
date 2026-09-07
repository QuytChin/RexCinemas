package com.rexchain.cinema.dto;
import java.util.List; public record SeatUpdateMessage(Long showtimeId,List<Long> seatIds,String status){}
