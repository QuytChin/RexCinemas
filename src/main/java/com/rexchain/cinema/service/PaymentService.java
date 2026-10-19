package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Coordinates the atomic, idempotent settlement of a booking. */
@Service
public class PaymentService {
    private final BookingRepository bookings;
    private final ShowtimeSeatRepository seats;
    private final UserRepository users;
    private final ComboProductRepository combos;
    private final VoucherRepository vouchers;
    private final SeatBroadcastService broadcast;
    private final SeatReservationService seatReservationService;
    private final NotificationService notifications;
    private final LoyaltyService loyalty;
    private final RewardRedemptionService rewardRedemptions;

    @Autowired
    public PaymentService(BookingRepository bookings, ShowtimeSeatRepository seats,
                          UserRepository users, ComboProductRepository combos,
                          VoucherRepository vouchers, SeatBroadcastService broadcast,
                          SeatReservationService seatReservationService, NotificationService notifications,
                          LoyaltyService loyalty, RewardRedemptionService rewardRedemptions) {
        this.bookings = bookings;
        this.seats = seats;
        this.users = users;
        this.combos = combos;
        this.vouchers = vouchers;
        this.broadcast = broadcast;
        this.seatReservationService = seatReservationService;
        this.notifications = notifications;
        this.loyalty = loyalty;
        this.rewardRedemptions = rewardRedemptions;
    }

    /** Compatibility constructor for focused tests. */
    PaymentService(BookingRepository bookings, ShowtimeSeatRepository seats,
                          UserRepository users, ComboProductRepository combos,
                          VoucherRepository vouchers, SeatBroadcastService broadcast,
                          SeatReservationService seatReservationService) {
        this(bookings, seats, users, combos, vouchers, broadcast, seatReservationService, null, null, null);
    }

    @Transactional
    public Booking completeOnlinePayment(String bookingCode, String reference) {
        String normalizedReference = requireReference(reference);
        Booking booking = bookings.findByBookingCodeForUpdate(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.REFUND_PENDING
                || booking.getStatus() == BookingStatus.REFUNDED) {
            verifySameReference(booking, normalizedReference);
            return booking;
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getStatus() == BookingStatus.EXPIRED
                || booking.getStatus() == BookingStatus.CANCELLED
                || (booking.getStatus() == BookingStatus.PENDING
                    && booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(now))) {
            return markPaidForRefund(booking, normalizedReference, now);
        }
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new IllegalStateException("Booking không còn chờ thanh toán");

        List<Long> seatIds = booking.getSeats().stream()
                .map(bs -> bs.getShowtimeSeat().getId()).distinct().sorted().toList();
        List<ShowtimeSeat> lockedSeats = seats.findByIdInForUpdate(seatIds);
        boolean seatHoldValid = lockedSeats.size() == seatIds.size()
                && lockedSeats.stream().allMatch(seat -> seat.getStatus() == SeatStatus.HELD
                    && Objects.equals(seat.getHeldByUserId(), booking.getUser().getId())
                    && seat.getHoldUntil() != null && seat.getHoldUntil().isAfter(now));
        if (!seatHoldValid) return markPaidForRefund(booking, normalizedReference, now);

        Map<Long, Integer> requiredStock = booking.getCombos().stream()
                .filter(line -> line.getCombo() != null)
                .collect(Collectors.toMap(line -> line.getCombo().getId(), BookingCombo::getQuantity, Integer::sum));
        List<ComboProduct> lockedCombos = requiredStock.isEmpty() ? List.of()
                : combos.findByIdInForUpdate(requiredStock.keySet().stream().sorted().toList());
        boolean stockAvailable = lockedCombos.size() == requiredStock.size()
                && lockedCombos.stream().allMatch(combo -> combo.isActive()
                    && combo.getStockQuantity() >= requiredStock.get(combo.getId()));

        Voucher lockedVoucher = booking.getVoucherCode() == null ? null
                : vouchers.findByCodeIgnoreCaseForUpdate(booking.getVoucherCode()).orElse(null);
        boolean voucherAvailable = booking.getVoucherCode() == null || (lockedVoucher != null
                && lockedVoucher.isActive()
                && (lockedVoucher.getUsageLimit() == null
                    || lockedVoucher.getUsedCount() < lockedVoucher.getUsageLimit()));
        if (!stockAvailable || !voucherAvailable)
            return markPaidForRefund(booking, normalizedReference, now);

        booking.setStatus(BookingStatus.CONFIRMED);
        recordProviderReference(booking, normalizedReference);
        booking.setPaidAt(now);
        booking.setExpiresAt(null);

        for (ShowtimeSeat seat : lockedSeats) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldByUserId(null);
            seat.setHoldUntil(null);
        }
        seats.saveAll(lockedSeats);
        if (!seatIds.isEmpty()) broadcast.send(booking.getShowtime().getId(), seatIds, "BOOKED");

        for (ComboProduct combo : lockedCombos)
            combo.setStockQuantity(combo.getStockQuantity() - requiredStock.get(combo.getId()));
        combos.saveAll(lockedCombos);
        if (lockedVoucher != null) {
            lockedVoucher.setUsedCount(lockedVoucher.getUsedCount() + 1);
            vouchers.save(lockedVoucher);
            if (rewardRedemptions != null) rewardRedemptions.markVoucherUsed(lockedVoucher);
        }
        awardPoints(booking);
        Booking saved = bookings.save(booking);
        if (notifications != null) notifications.paymentConfirmed(saved);
        return saved;
    }

    @Transactional
    public Booking cancelOnlinePayment(String bookingCode) {
        Booking booking = bookings.findByBookingCodeForUpdate(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));
        if (booking.getStatus() != BookingStatus.PENDING) return booking;
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExpiresAt(null);
        seatReservationService.releaseBookingSeats(booking);
        Booking saved = bookings.save(booking);
        if (notifications != null) notifications.bookingCancelled(saved);
        return saved;
    }

    @Transactional
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = bookings.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now).stream()
                .map(Booking::getId).sorted().toList();
        for (Long id : ids) {
            Booking booking = bookings.findByIdForUpdate(id).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.PENDING
                    || booking.getExpiresAt() == null || booking.getExpiresAt().isAfter(now)) continue;
            booking.setStatus(BookingStatus.EXPIRED);
            seatReservationService.releaseBookingSeats(booking);
            bookings.save(booking);
        }
    }

    @Transactional
    public Booking processRefund(Long bookingId, String operatorEmail, String note) {
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        boolean paidCancellation = booking.getStatus() == BookingStatus.CANCELLED && booking.getPaidAt() != null;
        if (booking.getStatus() != BookingStatus.REFUND_PENDING && !paidCancellation)
            throw new IllegalStateException("Chỉ giao dịch đã thu tiền và đang chờ hoàn mới có thể xác nhận hoàn tiền");
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundedBy(operatorEmail);
        booking.setRefundNote(note != null && !note.isBlank() ? note.trim() : "Đã xác nhận hoàn tiền");
        // Safe if cancellation already reversed points: the loyalty ledger is idempotent per booking.
        reversePoints(booking);
        Booking saved = bookings.save(booking);
        if (notifications != null) notifications.refundProcessed(saved);
        return saved;
    }

    public void awardPoints(Booking booking) {
        if (loyalty != null) {
            loyalty.awardBooking(booking);
            return;
        }
        if (booking.isPointsAwarded() || booking.getStatus() != BookingStatus.CONFIRMED) return;
        User user = users.findByIdForUpdate(booking.getUser().getId()).orElseThrow();
        int points = pointsFor(booking);
        user.setLoyaltyPoints(user.getLoyaltyPoints() + points);
        user.setLifetimePoints(user.getLifetimePoints() + points);
        users.save(user);
        booking.setPointsAwarded(true);
    }

    public void reversePoints(Booking booking) {
        if (loyalty != null) {
            loyalty.reverseBooking(booking);
            return;
        }
        if (!booking.isPointsAwarded()) return;
        User user = users.findByIdForUpdate(booking.getUser().getId()).orElseThrow();
        int points = pointsFor(booking);
        user.setLoyaltyPoints(user.getLoyaltyPoints() - points);
        user.setLifetimePoints(user.getLifetimePoints() - points);
        users.save(user);
        booking.setPointsAwarded(false);
    }

    public void incrementVoucher(Voucher voucher) {
        Voucher locked = vouchers.findByIdForUpdate(voucher.getId()).orElseThrow();
        if (locked.getUsageLimit() != null && locked.getUsedCount() >= locked.getUsageLimit())
            throw new IllegalStateException("Voucher đã hết lượt sử dụng");
        locked.setUsedCount(locked.getUsedCount() + 1);
        vouchers.save(locked);
        if (rewardRedemptions != null) rewardRedemptions.markVoucherUsed(locked);
    }

    public void decrementVoucher(Voucher voucher) {
        Voucher locked = vouchers.findByIdForUpdate(voucher.getId()).orElseThrow();
        locked.setUsedCount(Math.max(0, locked.getUsedCount() - 1));
        vouchers.save(locked);
        if (rewardRedemptions != null) rewardRedemptions.restoreRewardVoucher(locked);
    }

    private Booking markPaidForRefund(Booking booking, String reference, LocalDateTime paidAt) {
        booking.setStatus(BookingStatus.REFUND_PENDING);
        recordProviderReference(booking, normalizeReference(reference));
        booking.setPaidAt(paidAt);
        booking.setExpiresAt(null);
        seatReservationService.releaseBookingSeats(booking);
        return bookings.save(booking);
    }

    private void verifySameReference(Booking booking, String reference) {
        String normalized = normalizeReference(reference);
        String previous = booking.getPaymentMethod() == PaymentMethod.BANK_QR
                ? booking.getSepayTransactionId() : booking.getPaymentReference();
        if (previous != null && normalized != null && !previous.equals(normalized))
            throw new IllegalStateException("Booking đã được xử lý bởi một giao dịch thanh toán khác");
    }

    private void recordProviderReference(Booking booking, String reference) {
        if (booking.getPaymentMethod() == PaymentMethod.BANK_QR)
            booking.setSepayTransactionId(reference);
        else
            booking.setPaymentReference(reference);
    }

    private String normalizeReference(String reference) {
        if (reference == null || reference.isBlank()) return null;
        String value = reference.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private String requireReference(String reference) {
        String normalized = normalizeReference(reference);
        if (normalized == null)
            throw new IllegalArgumentException("Mã giao dịch thanh toán không hợp lệ");
        return normalized;
    }

    private int pointsFor(Booking booking) {
        int points = booking.getTotalAmount().divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
        return points <= 0 && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 ? 1 : points;
    }
}
