package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingSeat;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Dịch vụ soát vé (Check-in) và phát hành mã QR vé điện tử.
 */
@Service
public class CheckInService {

    public static final String QR_PREFIX = "REXCINEMAS:TICKET:";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final BookingRepository bookings;

    public CheckInService(BookingRepository bookings) {
        this.bookings = bookings;
    }

    /**
     * Lấy chuỗi dữ liệu nhúng vào mã QR (REXCINEMAS:TICKET:<token>).
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
     * Quét vé / kiểm tra mã booking tại cổng rạp với Atomic Check-in chống double scan.
     */
    @Transactional
    public CheckInResult checkInTicket(String rawCode, String operatorEmail) {
        if (rawCode == null || rawCode.isBlank())
            return emptyResult(false, "INVALID", "Mã QR / mã booking đang trống");

        String code = rawCode.trim();
        Booking booking;
        if (code.startsWith(QR_PREFIX)) {
            String token = code.substring(QR_PREFIX.length()).trim();
            booking = bookings.findByQrToken(token).orElse(null);
        } else {
            booking = bookings.findByBookingCode(code.toUpperCase(Locale.ROOT)).orElse(null);
        }

        if (booking == null)
            return emptyResult(false, "NOT_FOUND", "Không tìm thấy vé trong hệ thống");
        if (booking.getStatus() == BookingStatus.PENDING)
            return checkInResult(booking, false, "PENDING", "Vé chưa thanh toán / chưa được xác nhận");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            return checkInResult(booking, false, "CANCELLED", "Vé đã bị hủy và không còn hiệu lực");
        if (booking.getStatus() == BookingStatus.EXPIRED)
            return checkInResult(booking, false, "EXPIRED", "Vé đã hết hạn và không còn hiệu lực");
        if (booking.getStatus() == BookingStatus.REFUND_PENDING)
            return checkInResult(booking, false, "REFUND_PENDING", "Vé đang chờ hoàn tiền, không thể check-in");
        if (booking.isCheckedIn())
            return checkInResult(booking, false, "ALREADY_CHECKED_IN", "Vé đã được check-in trước đó");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = booking.getShowtime().getStartTime()
                .plusMinutes(booking.getShowtime().getMovie().getDurationMinutes())
                .plusMinutes(30);
        if (now.isAfter(endTime))
            return checkInResult(booking, false, "EXPIRED", "Suất chiếu đã kết thúc, vé đã hết thời gian check-in");

        int updated = bookings.atomicCheckIn(booking.getId(), now, operatorEmail);
        if (updated == 0) {
            return checkInResult(booking, false, "ALREADY_CHECKED_IN", "Vé đã được check-in trước đó");
        }

        String earlyNote = now.isBefore(booking.getShowtime().getStartTime().minusHours(2))
                ? " Check-in sớm hơn 2 giờ so với suất chiếu."
                : "";
        return checkInResult(booking, true, "CHECKED_IN", "Check-in thành công." + earlyNote);
    }

    public String newQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
        } while (bookings.findByQrToken(token).isPresent());
        return token;
    }

    private CheckInResult checkInResult(Booking booking, boolean success, String status, String message) {
        String seatLabels = booking.getSeats().stream()
                .map(BookingSeat::getSeatLabel).filter(Objects::nonNull).reduce((a, b) -> a + ", " + b).orElse("—");
        return new CheckInResult(
                success, status, message, booking.getBookingCode(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getAuditorium().getCinema().getName(),
                booking.getShowtime().getAuditorium().getName(),
                booking.getShowtime().getStartTime().format(DATE_TIME), seatLabels, booking.getUser().getFullName(),
                booking.getCheckedInAt() == null ? null : booking.getCheckedInAt().format(DATE_TIME),
                booking.getCheckedInBy());
    }

    private CheckInResult emptyResult(boolean success, String status, String message) {
        return new CheckInResult(success, status, message, null, null, null, null, null, null, null, null, null);
    }
}
