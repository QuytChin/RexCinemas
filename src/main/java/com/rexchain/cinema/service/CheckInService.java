package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingSeat;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.BookingSeatRepository;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Dịch vụ soát vé (Check-in) và phát hành mã QR vé điện tử độc lập.
 */
@Service
public class CheckInService {

    public static final String QR_PREFIX = "REXCINEMAS:TICKET:";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final UserRepository users;

    @Autowired
    public CheckInService(BookingRepository bookings, BookingSeatRepository bookingSeats, UserRepository users) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.users = users;
    }

    /**
     * Lấy chuỗi dữ liệu nhúng vào mã QR tổng của Booking
     * (REXCINEMAS:TICKET:<token>).
     */
    @Transactional
    public String qrPayloadFor(Long bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Chỉ vé đã xác nhận mới có mã QR hợp lệ");
        if (booking.getQrToken() == null || booking.getQrToken().isBlank()) {
            booking.setQrToken(newQrToken());
            bookings.save(booking);
        }
        return QR_PREFIX + booking.getQrToken();
    }

    /**
     * Lấy chuỗi dữ liệu nhúng vào mã QR riêng cho từng ghế (Ticket).
     */
    @Transactional
    public String seatQrPayloadFor(Long bookingId, Long seatId) {
        BookingSeat seat = bookingSeats.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Vé ghế không tồn tại"));
        if (!seat.getBooking().getId().equals(bookingId))
            throw new IllegalArgumentException("Vé ghế không thuộc đơn hàng này");
        if (seat.getBooking().getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Chỉ vé đã xác nhận mới có mã QR hợp lệ");

        if (seat.getQrToken() == null || seat.getQrToken().isBlank()) {
            seat.setQrToken(newQrToken());
            if (seat.getTicketCode() == null || seat.getTicketCode().isBlank()) {
                seat.setTicketCode("TKT-" + seat.getBooking().getBookingCode() + "-" + seat.getSeatLabel());
            }
            bookingSeats.save(seat);
        }
        return QR_PREFIX + seat.getQrToken();
    }

    /**
     * Quét vé / kiểm tra mã booking tại cổng rạp.
     * Hỗ trợ quét cả vé riêng lẻ (Seat Ticket) và mã đoàn (Booking Code).
     */
    @Transactional
    public CheckInResult checkInTicket(String rawCode, String operatorEmail) {
        if (rawCode == null || rawCode.isBlank())
            return emptyResult(false, "INVALID", "Mã QR / mã booking đang trống");

        String code = rawCode.trim();
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra nếu là chuỗi QR (REXCINEMAS:TICKET:<token>)
        if (code.startsWith(QR_PREFIX)) {
            String token = code.substring(QR_PREFIX.length()).trim();

            // 1.1 Tìm theo vé ghế đơn lẻ (Ticket)
            BookingSeat seat = bookingSeats.findByQrToken(token).orElse(null);
            if (seat != null) {
                return checkInSingleSeat(seat, now, operatorEmail);
            }

            // 1.2 Tìm theo Booking tổng của cả đoàn
            Booking booking = bookings.findByQrToken(token).orElse(null);
            if (booking != null) {
                return checkInGroupBooking(booking, now, operatorEmail);
            }

            return emptyResult(false, "NOT_FOUND", "Không tìm thấy vé trong hệ thống");
        }

        // 2. Tìm theo mã vé đơn lẻ (vd: TKT-REX...)
        if (code.toUpperCase(Locale.ROOT).startsWith("TKT-")) {
            BookingSeat seat = bookingSeats.findByTicketCode(code.toUpperCase(Locale.ROOT)).orElse(null);
            if (seat != null) {
                return checkInSingleSeat(seat, now, operatorEmail);
            }
        }

        // 3. Tìm theo mã booking tổng (vd: REX...)
        Booking booking = bookings.findByBookingCode(code.toUpperCase(Locale.ROOT)).orElse(null);
        if (booking != null) {
            return checkInGroupBooking(booking, now, operatorEmail);
        }

        return emptyResult(false, "NOT_FOUND", "Không tìm thấy vé trong hệ thống");
    }

    private CheckInResult checkInSingleSeat(BookingSeat seat, LocalDateTime now, String operatorEmail) {
        Booking booking = bookings.findByIdForUpdate(seat.getBooking().getId()).orElseThrow();
        CheckInResult statusCheck = validateBookingStatus(booking, now, operatorEmail);
        if (statusCheck != null)
            return statusCheck;

        if (seat.isCheckedIn()) {
            String timeStr = seat.getCheckedInAt() != null ? seat.getCheckedInAt().format(DATE_TIME) : "trước đó";
            String byStr = (seat.getCheckedInBy() != null && !seat.getCheckedInBy().isBlank()) ? " bởi nhân viên " + seat.getCheckedInBy() : "";
            return checkInSeatResult(booking, seat, false, "ALREADY_CHECKED_IN",
                    "Ghế " + seat.getSeatLabel() + " (" + seat.getTicketCode() + ") đã được sử dụng lúc " + timeStr + byStr);
        }

        int updated = bookingSeats.atomicCheckInSeat(seat.getId(), now, operatorEmail);
        if (updated == 0) {
            BookingSeat refreshed = bookingSeats.findById(seat.getId()).orElse(seat);
            String timeStr = refreshed.getCheckedInAt() != null ? refreshed.getCheckedInAt().format(DATE_TIME) : "vừa xong";
            String byStr = (refreshed.getCheckedInBy() != null && !refreshed.getCheckedInBy().isBlank()) ? " bởi nhân viên " + refreshed.getCheckedInBy() : "";
            return checkInSeatResult(booking, refreshed, false, "ALREADY_CHECKED_IN",
                    "Ghế " + refreshed.getSeatLabel() + " đã được sử dụng lúc " + timeStr + byStr + "!");
        }

        seat.setCheckedIn(true);
        seat.setCheckedInAt(now);
        seat.setCheckedInBy(operatorEmail);

        // Nếu tất cả ghế trong đoàn đã vào cổng thì đánh dấu cả booking
        boolean allCheckedIn = bookingSeats.countByBookingIdAndCheckedInFalse(booking.getId()) == 0;
        if (allCheckedIn && !booking.isCheckedIn()) {
            bookings.atomicCheckIn(booking.getId(), now, operatorEmail);
        }

        return checkInSeatResult(booking, seat, true, "CHECKED_IN",
                "Check-in thành công ghế " + seat.getSeatLabel() + " (Mã vé: " + seat.getTicketCode() + ")");
    }

    private CheckInResult checkInGroupBooking(Booking booking, LocalDateTime now, String operatorEmail) {
        booking = bookings.findByIdForUpdate(booking.getId()).orElseThrow();
        CheckInResult statusCheck = validateBookingStatus(booking, now, operatorEmail);
        if (statusCheck != null)
            return statusCheck;

        if (booking.isCheckedIn()) {
            String timeStr = booking.getCheckedInAt() != null ? booking.getCheckedInAt().format(DATE_TIME) : "trước đó";
            String byStr = (booking.getCheckedInBy() != null && !booking.getCheckedInBy().isBlank()) ? " bởi nhân viên " + booking.getCheckedInBy() : "";
            return emptyResult(false, "ALREADY_CHECKED_IN", "Vé đoàn (" + booking.getBookingCode() + ") đã được sử dụng lúc " + timeStr + byStr);
        }

        // Claim the group ticket first. Exactly one gate can change NULL -> checked-in.
        int claimed = bookings.atomicCheckIn(booking.getId(), now, operatorEmail);
        if (claimed == 0) {
            Booking refreshed = bookings.findById(booking.getId()).orElse(booking);
            String timeStr = refreshed.getCheckedInAt() != null ? refreshed.getCheckedInAt().format(DATE_TIME) : "vừa xong";
            String byStr = (refreshed.getCheckedInBy() != null && !refreshed.getCheckedInBy().isBlank()) ? " bởi nhân viên " + refreshed.getCheckedInBy() : "";
            return emptyResult(false, "ALREADY_CHECKED_IN", "Vé đoàn (" + booking.getBookingCode() + ") đã được sử dụng lúc " + timeStr + byStr + "!");
        }
        bookingSeats.atomicCheckInAllSeats(booking.getId(), now, operatorEmail);

        booking.setCheckedInAt(now);
        booking.setCheckedInBy(operatorEmail);
        for (BookingSeat bs : booking.getSeats()) {
            bs.setCheckedIn(true);
            bs.setCheckedInAt(now);
            bs.setCheckedInBy(operatorEmail);
        }

        String seatLabels = booking.getSeats().stream()
                .map(BookingSeat::getSeatLabel).filter(Objects::nonNull).reduce((a, b) -> a + ", " + b).orElse("—");

        return new CheckInResult(
                true, "CHECKED_IN", "Check-in thành công cả đoàn (" + seatLabels + ")",
                booking.getBookingCode(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getAuditorium().getCinema().getName(),
                booking.getShowtime().getAuditorium().getName(),
                booking.getShowtime().getStartTime().format(DATE_TIME), seatLabels, booking.getUser().getFullName(),
                now.format(DATE_TIME), operatorEmail);
    }

    private CheckInResult validateBookingStatus(Booking booking, LocalDateTime now, String operatorEmail) {
        if (operatorEmail == null || operatorEmail.isBlank()) {
            throw new AccessDeniedException("Không xác định được tài khoản nhân viên soát vé");
        }
        User operator = users.findByEmailIgnoreCase(operatorEmail)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản soát vé không tồn tại"));

        if (operator.getRole() == Role.STAFF || operator.getRole() == Role.SUPERVISOR) {
            if (operator.getCinema() == null || operator.getCinema().getId() == null
                    || !operator.getCinema().isActive()) {
                throw new AccessDeniedException("Tài khoản chưa được gán vào rạp đang hoạt động");
            }

            var ticketCinema = booking.getShowtime().getAuditorium().getCinema();
            if (ticketCinema == null || ticketCinema.getId() == null || !ticketCinema.isActive()) {
                throw new AccessDeniedException("Không thể xác định rạp hợp lệ của vé");
            }
            if (!operator.getCinema().getId().equals(ticketCinema.getId())) {
                return emptyResult(false, "WRONG_CINEMA",
                        "Vé này thuộc cụm rạp " + ticketCinema.getName() + ". Bạn chỉ có quyền soát vé tại cụm rạp "
                                + operator.getCinema().getName() + "!");
            }
        } else if (operator.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Tài khoản không có quyền soát vé");
        }

        if (booking.getStatus() == BookingStatus.PENDING)
            return emptyResult(false, "PENDING", "Vé chưa thanh toán / chưa được xác nhận");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            return emptyResult(false, "CANCELLED", "Vé đã bị hủy và không còn hiệu lực");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            return emptyResult(false, "EXPIRED", "Vé đã hết hạn và không còn hiệu lực");
        if (booking.getStatus() == BookingStatus.REFUND_PENDING || booking.getStatus() == BookingStatus.REFUNDED)
            return emptyResult(false, "REFUNDED", "Vé đã hoặc đang trong quá trình hoàn tiền, không thể check-in");
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            return emptyResult(false, "INVALID_STATUS", "Trạng thái vé không hợp lệ để check-in");

        LocalDateTime startTime = booking.getShowtime().getStartTime();
        if (now.isBefore(startTime.minusMinutes(60)))
            return emptyResult(false, "TOO_EARLY", "Chỉ có thể check-in trong vòng 60 phút trước giờ chiếu");

        LocalDateTime endTime = startTime
                .plusMinutes(booking.getShowtime().getMovie().getDurationMinutes())
                .plusMinutes(30);
        if (now.isAfter(endTime))
            return emptyResult(false, "EXPIRED", "Suất chiếu đã kết thúc quá 30 phút, vé đã hết hạn check-in");

        return null;
    }

    private CheckInResult checkInSeatResult(Booking booking, BookingSeat seat, boolean success, String status,
            String message) {
        return new CheckInResult(
                success, status, message,
                seat.getTicketCode() != null ? seat.getTicketCode() : booking.getBookingCode(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getAuditorium().getCinema().getName(),
                booking.getShowtime().getAuditorium().getName(),
                booking.getShowtime().getStartTime().format(DATE_TIME),
                seat.getSeatLabel(),
                booking.getUser().getFullName(),
                seat.getCheckedInAt() == null ? null : seat.getCheckedInAt().format(DATE_TIME),
                seat.getCheckedInBy());
    }

    public String newQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
        } while (bookings.findByQrToken(token).isPresent() || bookingSeats.findByQrToken(token).isPresent());
        return token;
    }

    private CheckInResult emptyResult(boolean success, String status, String message) {
        return new CheckInResult(success, status, message, null, null, null, null, null, null, null, null, null);
    }
}
