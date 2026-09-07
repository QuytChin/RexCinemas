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
        Auditorium auditorium = new Auditorium();
        auditorium.setCinema(cinema);
        auditorium.setName(name);
        auditorium.setTotalRows(rows);
        auditorium.setSeatsPerRow(cols);
        auditorium = auditoriums.save(auditorium);

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
                seats.save(seat);
            }
        }
        return auditorium;
    }

    @Transactional
    public void createShowtimeSeats(Showtime showtime) {
        List<Seat> baseSeats = seats.findByAuditoriumIdOrderBySeatRowAscSeatNumberAsc(showtime.getAuditorium().getId());
        for (Seat seat : baseSeats) {
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            BigDecimal price = showtime.getBasePrice();
            if (seat.getType() == SeatType.VIP) price = price.add(new BigDecimal("20000"));
            if (seat.getType() == SeatType.COUPLE) price = price.add(new BigDecimal("40000"));
            showtimeSeat.setPrice(price);
            showSeats.save(showtimeSeat);
        }
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
