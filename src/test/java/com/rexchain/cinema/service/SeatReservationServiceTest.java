package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Seat;
import com.rexchain.cinema.entity.SeatStatus;
import com.rexchain.cinema.entity.ShowtimeSeat;
import com.rexchain.cinema.repository.BookingSeatRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    @Mock ShowtimeSeatRepository seats;
    @Mock ShowtimeRepository showtimes;
    @Mock BookingSeatRepository bookingSeats;
    @Mock SeatBroadcastService broadcast;

    private SeatReservationService service;

    @BeforeEach
    void setUp() {
        service = new SeatReservationService(seats, showtimes, bookingSeats, broadcast);
    }

    private ShowtimeSeat makeSeat(long id, String row, int number) {
        ShowtimeSeat ss = new ShowtimeSeat();
        ss.setId(id);
        ss.setStatus(SeatStatus.AVAILABLE);
        Seat seat = new Seat();
        seat.setSeatRow(row);
        seat.setSeatNumber(number);
        ss.setSeat(seat);
        return ss;
    }

    @Test
    void testSingleRowOrphanSeat() {
        // Hàng C có 10 ghế (C1 -> C10)
        List<ShowtimeSeat> allSeats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            allSeats.add(makeSeat(i, "C", i));
        }
        when(seats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(1L)).thenReturn(allSeats);

        // User chọn ghế C2, bỏ lại đúng C1 (1 ghế mồ côi)
        ShowtimeSeat c2 = allSeats.get(1);

        assertThatThrownBy(() -> service.validateNoOrphanSeats(1L, List.of(c2), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không được để lại ghế trống đơn lẻ ở hàng C. Vui lòng chọn ghế liền kề.");
    }

    @Test
    void testMultipleRowsOrphanSeat() {
        // Hàng C (C1 -> C10) và Hàng D (D1 -> D10)
        List<ShowtimeSeat> allSeats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            allSeats.add(makeSeat(i, "C", i));
        }
        for (int i = 1; i <= 10; i++) {
            allSeats.add(makeSeat(10 + i, "D", i));
        }
        when(seats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(1L)).thenReturn(allSeats);

        // User chọn C2 (để lại C1 trống) và D9 (để lại D10 trống) - đúng như trường hợp của user!
        ShowtimeSeat c2 = allSeats.get(1); // C2
        ShowtimeSeat d9 = allSeats.get(18); // D9 (id=19)

        assertThatThrownBy(() -> service.validateNoOrphanSeats(1L, List.of(c2, d9), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không được để lại ghế trống đơn lẻ ở các hàng C, D. Vui lòng chọn ghế liền kề.");
    }

    @Test
    void testValidSelectionNoOrphanSeat() {
        List<ShowtimeSeat> allSeats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            allSeats.add(makeSeat(i, "C", i));
        }
        when(seats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(1L)).thenReturn(allSeats);

        // User chọn C1 và C2 liền kề nhau
        ShowtimeSeat c1 = allSeats.get(0);
        ShowtimeSeat c2 = allSeats.get(1);

        assertThatCode(() -> service.validateNoOrphanSeats(1L, List.of(c1, c2), LocalDateTime.now()))
                .doesNotThrowAnyException();
    }
}
