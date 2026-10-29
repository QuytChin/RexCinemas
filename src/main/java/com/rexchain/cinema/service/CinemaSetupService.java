package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CinemaSetupService {
    private final AuditoriumRepository auditoriums;
    private final SeatRepository seats;
    private final ShowtimeSeatRepository showSeats;

    public CinemaSetupService(AuditoriumRepository auditoriums, SeatRepository seats, ShowtimeSeatRepository showSeats) {
        this.auditoriums = auditoriums;
        this.seats = seats;
        this.showSeats = showSeats;
    }

    @Transactional
    public Auditorium createAuditorium(Cinema cinema, String name, int rows, int cols) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 80)
            throw new IllegalArgumentException("Tên phòng chiếu phải từ 1 đến 80 ký tự");
        if (rows < 1 || rows > 26 || cols < 1 || cols > 50)
            throw new IllegalArgumentException("Phòng chiếu hỗ trợ 1-26 hàng và 1-50 ghế mỗi hàng");
        if (auditoriums.existsByCinemaIdAndNameIgnoreCase(cinema.getId(), normalizedName))
            throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại trong rạp");
        Auditorium auditorium = new Auditorium();
        auditorium.setCinema(cinema);
        auditorium.setName(normalizedName);
        auditorium.setTotalRows(rows);
        auditorium.setSeatsPerRow(cols);
        auditorium = auditoriums.save(auditorium);

        java.util.ArrayList<Seat> generatedSeats = new java.util.ArrayList<>(rows * cols);
        for (int r = 0; r < rows; r++) {
            String row = String.valueOf((char) ('A' + r));
            for (int n = 1; n <= cols; n++) {
                Seat seat = new Seat();
                seat.setAuditorium(auditorium);
                seat.setSeatRow(row);
                seat.setSeatNumber(n);
                if (r == rows - 1) seat.setType(SeatType.COUPLE);
                else if (r >= Math.max(0, rows - 3)) seat.setType(SeatType.VIP);
                else seat.setType(SeatType.STANDARD);
                generatedSeats.add(seat);
            }
        }
        seats.saveAll(generatedSeats);
        return auditorium;
    }

    @Transactional
    public Auditorium renameAuditorium(Long id, String name) {
        Auditorium auditorium = auditoriums.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phòng chiếu không tồn tại"));
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank() || normalized.length() > 80)
            throw new IllegalArgumentException("Tên phòng chiếu phải từ 1 đến 80 ký tự");
        if (!auditorium.getName().equalsIgnoreCase(normalized)
                && auditoriums.existsByCinemaIdAndNameIgnoreCase(auditorium.getCinema().getId(), normalized))
            throw new IllegalArgumentException("Tên phòng chiếu đã tồn tại trong rạp");
        auditorium.setName(normalized);
        return auditoriums.save(auditorium);
    }

    @Transactional
    public void createShowtimeSeats(Showtime showtime) {
        List<Seat> baseSeats = seats.findByAuditoriumIdOrderBySeatRowAscSeatNumberAsc(showtime.getAuditorium().getId());
        java.util.ArrayList<ShowtimeSeat> generated = new java.util.ArrayList<>(baseSeats.size());
        for (Seat seat : baseSeats) {
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            BigDecimal price = showtime.getBasePrice();
            if (seat.getType() == SeatType.VIP) price = price.add(new BigDecimal("20000"));
            if (seat.getType() == SeatType.COUPLE) price = price.add(new BigDecimal("40000"));
            showtimeSeat.setPrice(price);
            generated.add(showtimeSeat);
        }
        showSeats.saveAll(generated);
    }
    @Transactional
    public void repriceShowtimeSeats(Showtime showtime) {
        List<ShowtimeSeat> existing = showSeats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(showtime.getId());
        for (ShowtimeSeat showtimeSeat : existing) {
            BigDecimal price = showtime.getBasePrice();
            if (showtimeSeat.getSeat().getType() == SeatType.VIP) price = price.add(new BigDecimal("20000"));
            if (showtimeSeat.getSeat().getType() == SeatType.COUPLE) price = price.add(new BigDecimal("40000"));
            showtimeSeat.setPrice(price);
        }
        showSeats.saveAll(existing);
    }

}
