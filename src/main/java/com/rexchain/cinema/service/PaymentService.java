package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dịch vụ xử lý thanh toán, callback/webhook idempotency, điểm thưởng thành viên và đối soát voucher.
 */
@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final ShowtimeSeatRepository seats;
    private final UserRepository users;
    private final ComboProductRepository combos;
    private final VoucherRepository vouchers;
    private final SeatBroadcastService broadcast;
    private final SeatReservationService seatReservationService;

    public PaymentService(BookingRepository bookings,
                          ShowtimeSeatRepository seats,
                          UserRepository users,
                          ComboProductRepository combos,
                          VoucherRepository vouchers,
                          SeatBroadcastService broadcast,
                          SeatReservationService seatReservationService) {
        this.bookings = bookings;
        this.seats = seats;
        this.users = users;
        this.combos = combos;
        this.vouchers = vouchers;
        this.broadcast = broadcast;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Hoàn tất thanh toán trực tuyến (MoMo/VNPAY) với khóa bi quan và bảo vệ Idempotency.
     */
    @Transactional
    public Booking completeOnlinePayment(String bookingCode, String reference) {
        // PESSIMISTIC_WRITE — ngăn chặn race condition khi Webhook IPN và Return URL gọi đồng thời
        Booking booking = bookings.findByBookingCodeForUpdate(bookingCode).orElseThrow();

        // Idempotency: nếu đơn đã được xác nhận trước đó thì bỏ qua
        if (booking.getStatus() == BookingStatus.CONFIRMED)
            return booking;

        // Thanh toán đến trễ sau khi đơn đã bị cron job đánh dấu EXPIRED -> chuyển REFUND_PENDING
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            booking.setStatus(BookingStatus.REFUND_PENDING);
            booking.setPaymentReference(reference);
            booking.setPaidAt(LocalDateTime.now());
            return bookings.save(booking);
        }

        if (booking.getStatus() != BookingStatus.PENDING)
            throw new IllegalStateException("Booking không còn chờ thanh toán");

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentReference(reference);
        booking.setPaidAt(LocalDateTime.now());

        // Chuyển trạng thái ghế từ HELD → BOOKED
        List<Long> seatIds = new ArrayList<>();
        for (BookingSeat bs : booking.getSeats()) {
            ShowtimeSeat seat = bs.getShowtimeSeat();
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldByUserId(null);
            seat.setHoldUntil(null);
            seats.save(seat);
            seatIds.add(seat.getId());
        }
        if (!seatIds.isEmpty()) {
            broadcast.send(booking.getShowtime().getId(), seatIds, "BOOKED");
        }

        // Trừ tồn kho F&B sau khi thanh toán thành công
        for (BookingCombo bc : booking.getCombos()) {
            ComboProduct cp = bc.getCombo();
            if (cp != null) {
                cp.setStockQuantity(Math.max(0, cp.getStockQuantity() - bc.getQuantity()));
                combos.save(cp);
            }
        }

        if (booking.getVoucherCode() != null)
            vouchers.findByCodeIgnoreCase(booking.getVoucherCode()).ifPresent(this::incrementVoucher);

        awardPoints(booking);
        return bookings.save(booking);
    }

    /**
     * Hủy giao dịch thanh toán online (khách hủy hoặc thanh toán thất bại).
     */
    @Transactional
    public Booking cancelOnlinePayment(String bookingCode) {
        Booking booking = bookings.findByBookingCode(bookingCode).orElseThrow();
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.EXPIRED)
            return booking;
        booking.setStatus(BookingStatus.CANCELLED);
        seatReservationService.releaseBookingSeats(booking);
        return bookings.save(booking);
    }

    /**
     * Duyệt hoàn tiền cho đơn hàng (REFUND_PENDING hoặc đơn đã CANCELLED).
     */
    @Transactional
    public Booking processRefund(Long bookingId, String operatorEmail, String note) {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        if (booking.getStatus() != BookingStatus.REFUND_PENDING && booking.getStatus() != BookingStatus.CANCELLED) {
            throw new IllegalStateException("Chỉ đơn hàng đang chờ hoàn tiền (REFUND_PENDING) hoặc đã hủy mới có thể duyệt hoàn tiền");
        }
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundedBy(operatorEmail);
        booking.setRefundNote(note != null && !note.isBlank() ? note.trim() : "Đã duyệt hoàn tiền thành công qua cổng thanh toán/quầy");
        return bookings.save(booking);
    }

    /**
     * Cộng điểm thành viên Rex Member khi thanh toán thành công.
     */
    public void awardPoints(Booking booking) {
        if (booking.isPointsAwarded() || booking.getStatus() != BookingStatus.CONFIRMED)
            return;
        int points = booking.getTotalAmount().divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
        if (points <= 0 && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            points = 1;
        User user = booking.getUser();
        user.setLoyaltyPoints(user.getLoyaltyPoints() + points);
        user.setLifetimePoints(user.getLifetimePoints() + points);
        users.save(user);
        booking.setPointsAwarded(true);
    }

    /**
     * Thu hồi điểm thành viên khi đơn hàng bị hủy bởi Admin.
     */
    public void reversePoints(Booking booking) {
        if (!booking.isPointsAwarded())
            return;
        int points = booking.getTotalAmount().divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
        if (points <= 0 && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            points = 1;
        User user = booking.getUser();
        user.setLoyaltyPoints(Math.max(0, user.getLoyaltyPoints() - points));
        user.setLifetimePoints(Math.max(0, user.getLifetimePoints() - points));
        users.save(user);
        booking.setPointsAwarded(false);
    }

    public void incrementVoucher(Voucher voucher) {
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit())
            return;
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        vouchers.save(voucher);
    }

    public void decrementVoucher(Voucher voucher) {
        voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
        vouchers.save(voucher);
    }
}
