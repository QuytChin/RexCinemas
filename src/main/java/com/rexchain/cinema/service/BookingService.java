package com.rexchain.cinema.service;

import com.rexchain.cinema.dto.CheckInResult;
import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service điều phối quy trình đặt vé tổng thể (Booking Orchestration).
 * Các trách nhiệm chi tiết được phân rã thành các Service chuyên biệt:
 * - {@link SeatReservationService}: Giữ chỗ, trả ghế, quy tắc Orphan Seat.
 * - {@link PaymentService}: Xử lý thanh toán, idempotency, điểm thành viên.
 * - {@link CheckInService}: Soát vé và tạo mã QR vé điện tử.
 */
@Service
public class BookingService {

    public static final String QR_PREFIX = CheckInService.QR_PREFIX;

    private final ShowtimeSeatRepository seats;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final ShowtimeRepository showtimes;
    private final SeatBroadcastService broadcast;
    private final ComboProductRepository combos;
    private final VoucherRepository vouchers;

    private final SeatReservationService seatReservationService;
    private final PaymentService paymentService;
    private final CheckInService checkInService;

    public BookingService(ShowtimeSeatRepository seats,
                          BookingRepository bookings,
                          UserRepository users,
                          ShowtimeRepository showtimes,
                          SeatBroadcastService broadcast,
                          ComboProductRepository combos,
                          VoucherRepository vouchers,
                          SeatReservationService seatReservationService,
                          PaymentService paymentService,
                          CheckInService checkInService) {
        this.seats = seats;
        this.bookings = bookings;
        this.users = users;
        this.showtimes = showtimes;
        this.broadcast = broadcast;
        this.combos = combos;
        this.vouchers = vouchers;
        this.seatReservationService = seatReservationService;
        this.paymentService = paymentService;
        this.checkInService = checkInService;
    }

    /* ──────────────────────────── HOLD & RELEASE (DELEGATED) ──────────────────────────── */

    @Transactional
    public void holdSeats(Long userId, Long showtimeId, List<Long> ids) {
        seatReservationService.holdSeats(userId, showtimeId, ids);
    }

    @Transactional
    public void releaseOwnHolds(Long userId, Long showtimeId, List<Long> ids) {
        seatReservationService.releaseOwnHolds(userId, showtimeId, ids);
    }

    /* ──────────────────────────── CONFIRM BOOKING ──────────────────────────── */

    @Transactional
    public Booking confirm(Long userId, Long showtimeId, List<Long> ids, PaymentMethod method,
                           String voucherCode, List<Long> comboIds, List<Integer> comboQtys) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        if (ids.size() > 10)
            throw new IllegalArgumentException("Mỗi booking tối đa 10 ghế");
        if (method == null)
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        LocalDateTime now = LocalDateTime.now();
        User user = users.findById(userId).orElseThrow();
        Showtime showtime = showtimes.findById(showtimeId).orElseThrow();
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(now))
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");

        List<ShowtimeSeat> list = seats.findByIdInForUpdate(ids);
        if (list.size() != ids.size())
            throw new IllegalArgumentException("Ghế không hợp lệ");

        for (ShowtimeSeat seat : list) {
            if (!seat.getShowtime().getId().equals(showtimeId)
                    || seat.getStatus() != SeatStatus.HELD
                    || !Objects.equals(seat.getHeldByUserId(), userId)
                    || seat.getHoldUntil() == null
                    || seat.getHoldUntil().isBefore(now)) {
                throw new IllegalStateException("Phiên giữ ghế đã hết hạn hoặc ghế đã thay đổi");
            }
        }

        BigDecimal seatAmount = list.stream().map(ShowtimeSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<ComboProduct, Integer> comboMap = resolveCombos(comboIds, comboQtys);
        BigDecimal comboAmount = comboMap.entrySet().stream()
                .map(e -> e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal beforeDiscount = seatAmount.add(comboAmount);
        Voucher voucher = resolveVoucher(voucherCode, beforeDiscount, now);
        BigDecimal discount = voucher == null ? BigDecimal.ZERO : calculateDiscount(voucher, beforeDiscount);
        BigDecimal total = beforeDiscount.subtract(discount).max(BigDecimal.ZERO);
        boolean online = method == PaymentMethod.MOMO_SANDBOX || method == PaymentMethod.VNPAY_SANDBOX;

        Booking booking = new Booking();
        booking.setBookingCode("REX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        booking.setQrToken(checkInService.newQrToken());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setPaymentMethod(method);
        booking.setStatus(online ? BookingStatus.PENDING : BookingStatus.CONFIRMED);
        booking.setSeatAmount(seatAmount);
        booking.setComboAmount(comboAmount);
        booking.setDiscountAmount(discount);
        booking.setVoucherCode(voucher == null ? null : voucher.getCode());
        booking.setTotalAmount(total);
        if (!online)
            booking.setPaidAt(now);
        if (online)
            booking.setExpiresAt(now.plusMinutes(15));
        booking = bookings.save(booking);

        for (ShowtimeSeat seat : list) {
            if (online) {
                seat.setHoldUntil(booking.getExpiresAt());
            } else {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
            }
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setShowtimeSeat(seat);
            bookingSeat.setSeatLabel(seat.getSeat().getLabel());
            bookingSeat.setPrice(seat.getPrice());
            bookingSeat.setTicketCode("TKT-" + booking.getBookingCode() + "-" + seat.getSeat().getLabel());
            bookingSeat.setQrToken(checkInService.newQrToken());
            bookingSeat.setCheckedIn(false);
            booking.getSeats().add(bookingSeat);
        }

        for (Map.Entry<ComboProduct, Integer> entry : comboMap.entrySet()) {
            BookingCombo bookingCombo = new BookingCombo();
            bookingCombo.setBooking(booking);
            bookingCombo.setCombo(entry.getKey());
            bookingCombo.setComboName(entry.getKey().getName());
            bookingCombo.setQuantity(entry.getValue());
            bookingCombo.setUnitPrice(entry.getKey().getPrice());
            bookingCombo.setLineTotal(entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            booking.getCombos().add(bookingCombo);

            if (!online) {
                ComboProduct cp = entry.getKey();
                cp.setStockQuantity(Math.max(0, cp.getStockQuantity() - entry.getValue()));
                combos.save(cp);
            }
        }

        if (voucher != null && !online)
            paymentService.incrementVoucher(voucher);
        if (!online)
            paymentService.awardPoints(booking);

        bookings.save(booking);
        seats.saveAllAndFlush(list);
        broadcast.send(showtimeId, ids, online ? "HELD" : "BOOKED");
        return booking;
    }

    /* ──────────────────────── PAYMENT (DELEGATED) ──────────────────────── */

    @Transactional
    public Booking completeOnlinePayment(String bookingCode, String reference) {
        return paymentService.completeOnlinePayment(bookingCode, reference);
    }

    @Transactional
    public Booking cancelOnlinePayment(String bookingCode) {
        return paymentService.cancelOnlinePayment(bookingCode);
    }

    /* ──────────────────────── CANCEL BY ADMIN ──────────────────────── */

    @Transactional
    public Booking cancelByAdmin(Long bookingId) {
        Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow();
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REFUND_PENDING
                || booking.getStatus() == BookingStatus.REFUNDED)
            return booking;
        if (booking.isCheckedIn())
            throw new IllegalStateException("Vé đã check-in nên không thể hủy");
        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.setStatus(wasConfirmed ? BookingStatus.REFUND_PENDING : BookingStatus.CANCELLED);
        booking.setExpiresAt(null);
        seatReservationService.releaseBookingSeats(booking);
        if (wasConfirmed) {
            Map<Long, Integer> restore = new TreeMap<>();
            for (BookingCombo line : booking.getCombos()) {
                if (line.getCombo() != null)
                    restore.merge(line.getCombo().getId(), line.getQuantity(), Integer::sum);
            }
            if (!restore.isEmpty()) {
                List<ComboProduct> locked = combos.findByIdInForUpdate(new ArrayList<>(restore.keySet()));
                for (ComboProduct combo : locked)
                    combo.setStockQuantity(combo.getStockQuantity() + restore.get(combo.getId()));
                combos.saveAll(locked);
            }
            if (booking.getVoucherCode() != null) {
                vouchers.findByCodeIgnoreCaseForUpdate(booking.getVoucherCode())
                        .ifPresent(paymentService::decrementVoucher);
            }
        }
        // Keep the same resource lock order as payment settlement: seats -> combos -> voucher -> user.
        paymentService.reversePoints(booking);
        return bookings.save(booking);
    }

    /* ──────────────────────── QR & CHECK-IN (DELEGATED) ──────────────────────── */

    @Transactional
    public String qrPayloadFor(Long bookingId) {
        return checkInService.qrPayloadFor(bookingId);
    }

    @Transactional
    public String seatQrPayloadFor(Long bookingId, Long seatId) {
        return checkInService.seatQrPayloadFor(bookingId, seatId);
    }

    @Transactional
    public CheckInResult checkInTicket(String rawCode, String operatorEmail) {
        return checkInService.checkInTicket(rawCode, operatorEmail);
    }

    @Transactional
    public Booking processRefund(Long bookingId, String operatorEmail, String note) {
        return paymentService.processRefund(bookingId, operatorEmail, note);
    }

    /* ──────────────────────── SCHEDULED: RELEASE EXPIRED ──────────────────────── */

    @Scheduled(fixedRate = 60_000)
    public void releaseExpired() {
        seatReservationService.releaseExpiredHolds();
        paymentService.expirePendingBookings();
    }

    /* ──────────────────────── PRIVATE HELPERS ──────────────────────── */

    private Map<ComboProduct, Integer> resolveCombos(List<Long> ids, List<Integer> qtys) {
        Map<ComboProduct, Integer> result = new LinkedHashMap<>();
        if (ids == null || qtys == null)
            return result;
        int n = Math.min(ids.size(), qtys.size());
        Map<Long, Integer> requested = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            Integer qty = qtys.get(i);
            if (qty == null || qty <= 0)
                continue;
            if (qty > 10)
                throw new IllegalArgumentException("Mỗi combo tối đa 10 phần");
            Long comboId = ids.get(i);
            if (comboId == null) throw new IllegalArgumentException("Combo không tồn tại");
            requested.merge(comboId, qty, Integer::sum);
            if (requested.get(comboId) > 10)
                throw new IllegalArgumentException("Mỗi combo tối đa 10 phần");
        }
        if (requested.isEmpty()) return result;
        List<ComboProduct> locked = combos.findByIdInForUpdate(new ArrayList<>(requested.keySet()));
        if (locked.size() != requested.size())
            throw new IllegalArgumentException("Combo không tồn tại");
        for (ComboProduct combo : locked) {
            int qty = requested.get(combo.getId());
            if (!combo.isActive())
                throw new IllegalArgumentException("Combo " + combo.getName() + " hiện không khả dụng");
            if (combo.getStockQuantity() < qty)
                throw new IllegalArgumentException("Món '" + combo.getName() + "' không đủ số lượng trong kho (chỉ còn " + combo.getStockQuantity() + " phần)");
            result.put(combo, qty);
        }
        return result;
    }

    private Voucher resolveVoucher(String code, BigDecimal subtotal, LocalDateTime now) {
        if (code == null || code.isBlank())
            return null;
        Voucher voucher = vouchers.findByCodeIgnoreCaseForUpdate(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại"));
        if (!voucher.isActive())
            throw new IllegalArgumentException("Voucher đã bị khóa");
        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom()))
            throw new IllegalArgumentException("Voucher chưa đến thời gian áp dụng");
        if (voucher.getValidUntil() != null && now.isAfter(voucher.getValidUntil()))
            throw new IllegalArgumentException("Voucher đã hết hạn");
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit())
            throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
        if (voucher.getMinOrderAmount() != null && subtotal.compareTo(voucher.getMinOrderAmount()) < 0)
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của voucher");
        return voucher;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.FIXED)
            discount = voucher.getDiscountValue();
        else
            discount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0,
                    RoundingMode.HALF_UP);
        if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            discount = discount.min(voucher.getMaxDiscountAmount());
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }
}
