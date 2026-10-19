package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingSeat;
import com.rexchain.cinema.entity.SeatStatus;
import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.entity.ShowtimeSeat;
import com.rexchain.cinema.entity.SeatHoldSession;
import com.rexchain.cinema.repository.ShowtimeRepository;
import com.rexchain.cinema.repository.ShowtimeSeatRepository;
import com.rexchain.cinema.repository.SeatHoldSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final com.rexchain.cinema.repository.BookingSeatRepository bookingSeats;
    private final SeatBroadcastService broadcast;
    private final SeatHoldSessionRepository holdSessions;

    @Autowired
    public SeatReservationService(ShowtimeSeatRepository seats,
                                  ShowtimeRepository showtimes,
                                  com.rexchain.cinema.repository.BookingSeatRepository bookingSeats,
                                  SeatBroadcastService broadcast,
                                  SeatHoldSessionRepository holdSessions) {
        this.seats = seats;
        this.showtimes = showtimes;
        this.bookingSeats = bookingSeats;
        this.broadcast = broadcast;
        this.holdSessions = holdSessions;
    }

    /** Compatibility constructor for isolated tests written before checkout sessions existed. */
    SeatReservationService(ShowtimeSeatRepository seats, ShowtimeRepository showtimes,
                           com.rexchain.cinema.repository.BookingSeatRepository bookingSeats,
                           SeatBroadcastService broadcast) {
        this(seats, showtimes, bookingSeats, broadcast, null);
    }

    /**
     * Giữ ghế tạm thời (5 phút) với Pessimistic Lock và kiểm tra Orphan Seat.
     */
    @Transactional
    public LocalDateTime holdSeats(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        if (ids.size() > 10)
            throw new IllegalArgumentException("Mỗi booking tối đa 10 ghế");

        LocalDateTime now = LocalDateTime.now();
        Showtime showtime = showtimes.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Suất chiếu không tồn tại"));
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(now)
                || showtime.getAuditorium().getCinema() == null
                || !showtime.getAuditorium().getCinema().isActive())
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");

        List<Long> requestedIds = ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (requestedIds.size() != ids.size())
            throw new IllegalArgumentException("Danh sách ghế có dữ liệu trùng hoặc không hợp lệ");

        // Lock one stable seat-map snapshot so two concurrent selections cannot bypass the orphan rule.
        LocalDateTime holdExpiresAt = checkoutExpiresAt(userId, showtime, now);
        if (!holdExpiresAt.isAfter(now))
            throw new IllegalStateException("Phiên thanh toán đã hết hạn; vui lòng bắt đầu lại");

        List<ShowtimeSeat> allSeats = seats.findByShowtimeIdForUpdate(showtimeId);
        Map<Long, ShowtimeSeat> byId = allSeats.stream()
                .collect(Collectors.toMap(ShowtimeSeat::getId, seat -> seat));
        List<ShowtimeSeat> list = requestedIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        if (list.size() != requestedIds.size())
            throw new IllegalArgumentException("Ghế không hợp lệ");

        validateNoOrphanSeats(list, allSeats, now);

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
            seat.setHoldUntil(holdExpiresAt);
        }
        seats.saveAllAndFlush(list);
        broadcast.send(showtimeId, requestedIds, "HELD");
        return holdExpiresAt;
    }

    private LocalDateTime checkoutExpiresAt(Long userId, Showtime showtime, LocalDateTime now) {
        if (holdSessions == null) return now.plusMinutes(5);
        SeatHoldSession session = holdSessions.findByUserIdAndShowtimeIdForUpdate(userId, showtime.getId())
                .orElse(null);
        if (session == null) {
            session = new SeatHoldSession();
            session.setUserId(userId);
            session.setShowtime(showtime);
            session.setExpiresAt(now.plusMinutes(5));
            holdSessions.save(session);
            return session.getExpiresAt();
        }
        if (!session.getExpiresAt().isAfter(now)) {
            session.setExpiresAt(now.plusMinutes(5));
            holdSessions.save(session);
        }
        return session.getExpiresAt();
    }

    /**
     * Khách hàng tự hủy giữ ghế (ví dụ quay lại bước trước hoặc rời khỏi màn hình).
     */
    @Transactional
    public void releaseOwnHolds(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return;
        List<Long> requestedIds = ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        List<ShowtimeSeat> list = seats.findByIdInForUpdate(requestedIds);
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
        List<Long> bookingSeatIds = booking.getSeats().stream()
                .map(BookingSeat::getShowtimeSeat)
                .filter(Objects::nonNull)
                .map(ShowtimeSeat::getId)
                .distinct().sorted().toList();
        if (bookingSeatIds.isEmpty()) return;

        List<ShowtimeSeat> lockedSeats = seats.findByIdInForUpdate(bookingSeatIds);
        List<Long> ids = new ArrayList<>();
        for (ShowtimeSeat seat : lockedSeats) {
            boolean ownedHold = seat.getStatus() == SeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), booking.getUser().getId());
            boolean bookedByAnotherConfirmedBooking = seat.getStatus() == SeatStatus.BOOKED
                    && bookingSeats.existsByShowtimeSeatIdAndBookingStatusAndBookingIdNot(
                        seat.getId(), com.rexchain.cinema.entity.BookingStatus.CONFIRMED, booking.getId());
            if ((seat.getStatus() == SeatStatus.BOOKED && !bookedByAnotherConfirmedBooking) || ownedHold) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
                ids.add(seat.getId());
            }
        }
        seats.saveAll(lockedSeats);
        if (!ids.isEmpty()) {
            broadcast.send(booking.getShowtime().getId(), ids, "AVAILABLE");
        }
    }

    /**
     * Tự động nhả các ghế HELD đã quá hạn giữ chỗ (5 phút).
     */
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> candidateIds = seats.findByStatusAndHoldUntilBefore(SeatStatus.HELD, now).stream()
                .map(ShowtimeSeat::getId).distinct().sorted().toList();
        if (candidateIds.isEmpty()) return;
        List<ShowtimeSeat> expired = seats.findByIdInForUpdate(candidateIds).stream()
                .filter(seat -> seat.getStatus() == SeatStatus.HELD)
                .filter(seat -> seat.getHoldUntil() != null && seat.getHoldUntil().isBefore(now))
                .toList();
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
        validateNoOrphanSeats(selectedSeats,
                seats.findByShowtimeIdOrderBySeatSeatRowAscSeatSeatNumberAsc(showtimeId), now);
    }

    private void validateNoOrphanSeats(List<ShowtimeSeat> selectedSeats,
                                       List<ShowtimeSeat> allSeats,
                                       LocalDateTime now) {
        Set<String> affectedRows = selectedSeats.stream()
                .map(s -> s.getSeat().getSeatRow())
                .collect(Collectors.toSet());

        Set<Long> selectedIds = selectedSeats.stream().map(ShowtimeSeat::getId).collect(Collectors.toSet());

        Map<String, List<ShowtimeSeat>> seatsByRow = allSeats.stream()
                .collect(Collectors.groupingBy(s -> s.getSeat().getSeatRow()));

        Set<String> orphanRows = new TreeSet<>();
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
                        orphanRows.add(row);
                        break;
                    }
                    consecutiveEmpty = 0;
                }
            }

            if (consecutiveEmpty == 1) {
                orphanRows.add(row);
            }
        }

        if (!orphanRows.isEmpty()) {
            String rowList = String.join(", ", orphanRows);
            String rowPrefix = orphanRows.size() > 1 ? "các hàng " : "hàng ";
            throw new IllegalArgumentException(
                    "Không được để lại ghế trống đơn lẻ ở " + rowPrefix + rowList + ". Vui lòng chọn ghế liền kề."
            );
        }
    }
}
