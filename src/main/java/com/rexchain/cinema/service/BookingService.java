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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BookingService {
    public static final String QR_PREFIX = "REXCINEMAS:TICKET:";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final ShowtimeSeatRepository seats;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final ShowtimeRepository showtimes;
    private final SeatBroadcastService broadcast;
    private final ComboProductRepository combos;
    private final VoucherRepository vouchers;

    public BookingService(ShowtimeSeatRepository seats, BookingRepository bookings, UserRepository users,
                          ShowtimeRepository showtimes, SeatBroadcastService broadcast,
                          ComboProductRepository combos, VoucherRepository vouchers) {
        this.seats = seats;
        this.bookings = bookings;
        this.users = users;
        this.showtimes = showtimes;
        this.broadcast = broadcast;
        this.combos = combos;
        this.vouchers = vouchers;
    }

    /* ──────────────────────────── HOLD SEATS ──────────────────────────── */

    @Transactional
    public void holdSeats(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        if (ids.size() > 10) throw new IllegalArgumentException("Mỗi booking tối đa 10 ghế");
        LocalDateTime now = LocalDateTime.now();
        Showtime showtime = showtimes.findById(showtimeId).orElseThrow(() -> new IllegalArgumentException("Suất chiếu không tồn tại"));
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(now))
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");

        // Fix 2: PESSIMISTIC_WRITE — lock rows to prevent race condition
        List<ShowtimeSeat> list = seats.findByIdInForUpdate(ids);
        if (list.size() != ids.size()) throw new IllegalArgumentException("Ghế không hợp lệ");

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

    /* ──────────────────────────── CONFIRM BOOKING ──────────────────────────── */

    @Transactional
    public Booking confirm(Long userId, Long showtimeId, List<Long> ids, PaymentMethod method,
                           String voucherCode, List<Long> comboIds, List<Integer> comboQtys) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        if (ids.size() > 10) throw new IllegalArgumentException("Mỗi booking tối đa 10 ghế");
        LocalDateTime now = LocalDateTime.now();
        User user = users.findById(userId).orElseThrow();
        Showtime showtime = showtimes.findById(showtimeId).orElseThrow();
        if (!showtime.isActive() || !showtime.getStartTime().isAfter(now))
            throw new IllegalStateException("Suất chiếu đã đóng hoặc đã bắt đầu");

        // Fix 2: PESSIMISTIC_WRITE — lock rows to prevent race condition
        List<ShowtimeSeat> list = seats.findByIdInForUpdate(ids);
        if (list.size() != ids.size()) throw new IllegalArgumentException("Ghế không hợp lệ");

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
        booking.setQrToken(newQrToken());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setPaymentMethod(method);
        booking.setStatus(online ? BookingStatus.PENDING : BookingStatus.CONFIRMED);
        booking.setSeatAmount(seatAmount);
        booking.setComboAmount(comboAmount);
        booking.setDiscountAmount(discount);
        booking.setVoucherCode(voucher == null ? null : voucher.getCode());
        booking.setTotalAmount(total);
        if (!online) booking.setPaidAt(now);
        // Fix 1: Set expiry for online bookings (15 min window for payment)
        if (online) booking.setExpiresAt(now.plusMinutes(15));
        booking = bookings.save(booking);

        // Fix 1: For online payments, keep seats HELD (with extended hold time for payment)
        //         For counter payments, set seats to BOOKED immediately.
        for (ShowtimeSeat seat : list) {
            if (online) {
                // Extend hold time to cover the payment window
                seat.setHoldUntil(now.plusMinutes(20));
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
        }

        // Fix 1: Only award points & increment voucher for counter payment (already paid)
        if (voucher != null && !online) incrementVoucher(voucher);
        if (!online) awardPoints(booking);
        bookings.save(booking);
        seats.saveAllAndFlush(list);
        // Fix 1: Online bookings are still HELD, not BOOKED
        broadcast.send(showtimeId, ids, online ? "HELD" : "BOOKED");
        return booking;
    }

    /* ──────────────────────── COMPLETE ONLINE PAYMENT ──────────────────────── */

    @Transactional
    public Booking completeOnlinePayment(String bookingCode, String reference) {
        // Fix 3: PESSIMISTIC_WRITE — prevents duplicate webhook processing
        Booking booking = bookings.findByBookingCodeForUpdate(bookingCode).orElseThrow();

        // Idempotency guard: already processed
        if (booking.getStatus() == BookingStatus.CONFIRMED) return booking;

        // Fix 4: Payment arrived after booking was expired by cron job
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

        // Fix 1: Now transition seats from HELD → BOOKED
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

        if (booking.getVoucherCode() != null)
            vouchers.findByCodeIgnoreCase(booking.getVoucherCode()).ifPresent(this::incrementVoucher);
        awardPoints(booking);
        return bookings.save(booking);
    }

    /* ──────────────────────── CANCEL ONLINE PAYMENT ──────────────────────── */

    @Transactional
    public Booking cancelOnlinePayment(String bookingCode) {
        Booking booking = bookings.findByBookingCode(bookingCode).orElseThrow();
        // Fix 4: Also allow cancelling EXPIRED bookings
        if (booking.getStatus() != BookingStatus.PENDING
            && booking.getStatus() != BookingStatus.EXPIRED) return booking;
        booking.setStatus(BookingStatus.CANCELLED);
        releaseBookingSeats(booking);
        return bookings.save(booking);
    }

    /* ──────────────────────── CANCEL BY ADMIN ──────────────────────── */

    @Transactional
    public Booking cancelByAdmin(Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        if (booking.getStatus() == BookingStatus.CANCELLED) return booking;
        if (booking.isCheckedIn()) throw new IllegalStateException("Vé đã check-in nên không thể hủy");
        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.setStatus(BookingStatus.CANCELLED);
        releaseBookingSeats(booking);
        reversePoints(booking);
        if (wasConfirmed && booking.getVoucherCode() != null) {
            vouchers.findByCodeIgnoreCase(booking.getVoucherCode()).ifPresent(this::decrementVoucher);
        }
        return bookings.save(booking);
    }

    /* ──────────────────────── RELEASE OWN HOLDS ──────────────────────── */

    @Transactional
    public void releaseOwnHolds(Long userId, Long showtimeId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
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

    /* ──────────────────────── LOYALTY POINTS ──────────────────────── */

    private void awardPoints(Booking booking) {
        if (booking.isPointsAwarded() || booking.getStatus() != BookingStatus.CONFIRMED) return;
        int points = booking.getTotalAmount().divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
        if (points <= 0 && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) points = 1;
        User user = booking.getUser();
        user.setLoyaltyPoints(user.getLoyaltyPoints() + points);
        user.setLifetimePoints(user.getLifetimePoints() + points);
        users.save(user);
        booking.setPointsAwarded(true);
    }

    private void reversePoints(Booking booking) {
        if (!booking.isPointsAwarded()) return;
        int points = booking.getTotalAmount().divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
        if (points <= 0 && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) points = 1;
        User user = booking.getUser();
        user.setLoyaltyPoints(Math.max(0, user.getLoyaltyPoints() - points));
        user.setLifetimePoints(Math.max(0, user.getLifetimePoints() - points));
        users.save(user);
        booking.setPointsAwarded(false);
    }

    /* ──────────────────────── RELEASE BOOKING SEATS ──────────────────────── */

    private void releaseBookingSeats(Booking booking) {
        List<Long> ids = new ArrayList<>();
        for (BookingSeat bookingSeat : booking.getSeats()) {
            ShowtimeSeat seat = bookingSeat.getShowtimeSeat();
            // Fix 6: Also release HELD seats (online bookings keep seats HELD until payment)
            if (seat.getStatus() == SeatStatus.BOOKED || seat.getStatus() == SeatStatus.HELD) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldUntil(null);
                seats.save(seat);
                ids.add(seat.getId());
            }
        }
        if (!ids.isEmpty()) broadcast.send(booking.getShowtime().getId(), ids, "AVAILABLE");
    }

    /* ──────────────────────── QR CODE ──────────────────────── */

    @Transactional
    public String qrPayloadFor(Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Chỉ vé đã xác nhận mới có mã QR hợp lệ");
        if (booking.getQrToken() == null || booking.getQrToken().isBlank()) {
            booking.setQrToken(newQrToken());
            bookings.save(booking);
        }
        return QR_PREFIX + booking.getQrToken();
    }

    /* ──────────────────────── CHECK-IN ──────────────────────── */

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

        if (booking == null) return emptyResult(false, "NOT_FOUND", "Không tìm thấy vé trong hệ thống");
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

        // Fix 5: Atomic check-in — prevents double scan at two gates
        int updated = bookings.atomicCheckIn(booking.getId(), now, operatorEmail);
        if (updated == 0) {
            return checkInResult(booking, false, "ALREADY_CHECKED_IN", "Vé đã được check-in trước đó");
        }

        String earlyNote = now.isBefore(booking.getShowtime().getStartTime().minusHours(2))
                ? " Check-in sớm hơn 2 giờ so với suất chiếu." : "";
        return checkInResult(booking, true, "CHECKED_IN", "Check-in thành công." + earlyNote);
    }

    /* ──────────────────────── CHECK-IN RESULT HELPERS ──────────────────────── */

    private CheckInResult checkInResult(Booking booking, boolean success, String status, String message) {
        String seatLabels = booking.getSeats().stream()
                .map(BookingSeat::getSeatLabel).filter(Objects::nonNull).reduce((a, b) -> a + ", " + b).orElse("—");
        return new CheckInResult(
                success, status, message, booking.getBookingCode(), booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getAuditorium().getCinema().getName(), booking.getShowtime().getAuditorium().getName(),
                booking.getShowtime().getStartTime().format(DATE_TIME), seatLabels, booking.getUser().getFullName(),
                booking.getCheckedInAt() == null ? null : booking.getCheckedInAt().format(DATE_TIME),
                booking.getCheckedInBy());
    }

    private CheckInResult emptyResult(boolean success, String status, String message) {
        return new CheckInResult(success, status, message, null, null, null, null, null, null, null, null, null);
    }

    /* ──────────────────────── PRIVATE HELPERS ──────────────────────── */

    private String newQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
        } while (bookings.findByQrToken(token).isPresent());
        return token;
    }

    private Map<ComboProduct, Integer> resolveCombos(List<Long> ids, List<Integer> qtys) {
        Map<ComboProduct, Integer> result = new LinkedHashMap<>();
        if (ids == null || qtys == null) return result;
        int n = Math.min(ids.size(), qtys.size());
        for (int i = 0; i < n; i++) {
            Integer qty = qtys.get(i);
            if (qty == null || qty <= 0) continue;
            if (qty > 10) throw new IllegalArgumentException("Mỗi combo tối đa 10 phần");
            ComboProduct combo = combos.findById(ids.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Combo không tồn tại"));
            if (!combo.isActive()) throw new IllegalArgumentException("Combo " + combo.getName() + " hiện không khả dụng");
            result.put(combo, qty);
        }
        return result;
    }

    private Voucher resolveVoucher(String code, BigDecimal subtotal, LocalDateTime now) {
        if (code == null || code.isBlank()) return null;
        Voucher voucher = vouchers.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại"));
        if (!voucher.isActive()) throw new IllegalArgumentException("Voucher đã bị khóa");
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
        if (voucher.getDiscountType() == DiscountType.FIXED) discount = voucher.getDiscountValue();
        else discount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            discount = discount.min(voucher.getMaxDiscountAmount());
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private void incrementVoucher(Voucher voucher) {
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) return;
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        vouchers.save(voucher);
    }

    private void decrementVoucher(Voucher voucher) {
        voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
        vouchers.save(voucher);
    }

    /* ──────────────────────── SCHEDULED: RELEASE EXPIRED ──────────────────────── */

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void releaseExpired() {
        // Release individual seat holds that have timed out (user selected but didn't proceed)
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

        // Fix 1 + Fix 4: Expire PENDING bookings that have passed their expiresAt deadline
        for (Booking booking : bookings.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING, LocalDateTime.now())) {
            // Fix 4: Mark as EXPIRED (not CANCELLED) so we can detect late payment arrivals
            booking.setStatus(BookingStatus.EXPIRED);
            releaseBookingSeats(booking);
            bookings.save(booking);
        }
    }
}
