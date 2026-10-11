package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingSeat;
import com.rexchain.cinema.entity.SeatStatus;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.entity.ShowtimeSeat;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.repository.ShowtimeSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ quản lý giữ chỗ, trả ghế và quy tắc chống ghế trống đơn lẻ (Orphan Seat).
 */
@Service
public class SeatReservationService {

    private final ShowtimeSeatRepository seats;
    private final ShowtimeRepository showtimes;
    private final SeatBroadcastService broadcast;

    public SeatReservationService(ShowtimeSeatRepository seats,
                                  ShowtimeRepository showtimes,
                                  SeatBroadcastService broadcast) {
        this.seats = seats;
        this.showtimes = showtimes;
        this.broadcast = broadcast;
    }

    /**
     * Giữ ghế tạm thời (5 phút) với Pessimistic Lock và kiểm tra Orphan Seat.
     */
    @Transactional
    public void holdSeats(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        if (ids.size() > 10)
            throw new IllegalArgumentException("Mỗi booking tối đa 10 ghế");

        LocalDateTime now = LocalDateTime.now();
        Showtime showtime = showtimes.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Suất chiếu không tồn tại"));
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(now))
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");

        List<ShowtimeSeat> list = seats.findByIdInForUpdate(ids);
        if (list.size() != ids.size())
            throw new IllegalArgumentException("Ghế không hợp lệ");

        validateNoOrphanSeats(showtimeId, list, now);

        for (ShowtimeSeat seat : list) {
            if (!seat.getShowtime().getId().equals(showtimeId))
                throw new IllegalArgumentException("Ghế không thuộc suất chiếu");

            boolean ownHold = seat.getStatus() == SeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), userId)
                    && seat.getHoldUntil() != null && seat.getHoldUntil().isAfter(now);
            boolean expired = seat.getStatus() == SeatStatus.HELD
                    && (seat.getHoldUntil() == null || seat.getHoldUntil().isBefore(now));

            if (expired) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
            }
            if (seat.getStatus() == SeatStatus.BOOKED || (seat.getStatus() == SeatStatus.HELD && !ownHold))
                throw new IllegalStateException("Một hoặc nhiều ghế vừa được người khác chọn");

            seat.setStatus(SeatStatus.HELD);
            seat.setHeldByUserId(userId);
            seat.setHoldUntil(now.plusMinutes(5));
        }
        seats.saveAllAndFlush(list);
        broadcast.send(showtimeId, ids, "HELD");
    }

    /**
     * Khách hàng tự hủy giữ ghế (ví dụ quay lại bước trước hoặc rời khỏi màn hình).
     */
    @Transactional
    public void releaseOwnHolds(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return;
        List<ShowtimeSeat> list = seats.findByIdIn(ids);
        List<Long> released = new ArrayList<>();
        for (ShowtimeSeat seat : list) {
            if (seat.getShowtime().getId().equals(showtimeId)
                    && seat.getStatus() == SeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), userId)) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
                released.add(seat.getId());
            }
        }
        if (!released.isEmpty()) {
            seats.saveAllAndFlush(list);
            broadcast.send(showtimeId, released, "AVAILABLE");
        }
    }

    /**
     * Trả ghế của một Booking (khi đơn bị hủy hoặc hết hạn).
     */
    @Transactional
    public void releaseBookingSeats(Booking booking) {
        List<Long> ids = new ArrayList<>();
        for (BookingSeat bookingSeat : booking.getSeats()) {
            ShowtimeSeat seat = bookingSeat.getShowtimeSeat();
            if (seat.getStatus() == SeatStatus.BOOKED || seat.getStatus() == SeatStatus.HELD) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
                seats.save(seat);
                ids.add(seat.getId());
            }
        }
        if (!ids.isEmpty()) {
            broadcast.send(booking.getShowtime().getId(), ids, "AVAILABLE");
        }
    }

    /**
     * Tự động nhả các ghế HELD đã quá hạn giữ chỗ (5 phút).
     */
    @Transactional
    public void releaseExpiredHolds() {
        List<ShowtimeSeat> expired = seats.findByStatusAndHoldUntilBefore(SeatStatus.HELD, LocalDateTime.now());
        if (!expired.isEmpty()) {
            Map<Long, List<Long>> byShowtime = new HashMap<>();
            for (ShowtimeSeat seat : expired) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
                byShowtime.computeIfAbsent(seat.getShowtime().getId(), k -> new ArrayList<>()).add(seat.getId());
            }
            seats.saveAll(expired);
            byShowtime.forEach((id, seatIds) -> broadcast.send(id, seatIds, "AVAILABLE"));
        }
    }

    /**
     * Thuật toán kiểm tra không để lại 1 ghế trống đơn lẻ (Orphan Seat Rule).
     */
    public void validateNoOrphanSeats(Long showtimeId, List<ShowtimeSeat> selectedSeats, LocalDateTime now) {
        Set<String> affectedRows = selectedSeats.stream()
                .map(s -> s.getSeat().getSeatRow())
                .collect(Collectors.toSet());

        List<ShowtimeSeat> allSeats = seats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(showtimeId);
        Set<Long> selectedIds = selectedSeats.stream().map(ShowtimeSeat::getId).collect(Collectors.toSet());

        Map<String, List<ShowtimeSeat>> seatsByRow = allSeats.stream()
                .collect(Collectors.groupingBy(s -> s.getSeat().getSeatRow()));

        for (String row : affectedRows) {
            List<ShowtimeSeat> rowSeats = seatsByRow.getOrDefault(row, Collections.emptyList());
            if (rowSeats.isEmpty()) continue;

            int consecutiveEmpty = 0;
            for (ShowtimeSeat s : rowSeats) {
                boolean isOccupied = selectedIds.contains(s.getId())
                        || s.getStatus() == SeatStatus.BOOKED
                        || (s.getStatus() == SeatStatus.HELD && s.getHoldUntil() != null && s.getHoldUntil().isAfter(now));

                if (!isOccupied) {
                    consecutiveEmpty++;
                } else {
                    if (consecutiveEmpty == 1) {
                        throw new IllegalArgumentException(
                                "Không được để lại đúng 1 ghế trống đơn độc ở hàng " + row + ". Vui lòng chọn ghế liền kề."
                        );
                    }
                    consecutiveEmpty = 0;
                }
            }

            if (consecutiveEmpty == 1) {
                throw new IllegalArgumentException(
                        "Không được để lại đúng 1 ghế trống đơn độc ở hàng " + row + ". Vui lòng chọn ghế liền kề."
                );
            }
        }
    }
}
