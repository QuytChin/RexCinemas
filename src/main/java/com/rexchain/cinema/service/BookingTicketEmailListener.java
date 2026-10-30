package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.event.BookingConfirmedEvent;
import com.rexchain.cinema.repository.BookingRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Sends the electronic ticket only after the payment transaction has committed. */
@Component
public class BookingTicketEmailListener {
    private static final Logger log = LoggerFactory.getLogger(BookingTicketEmailListener.class);
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", VIETNAMESE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", VIETNAMESE);

    private final BookingRepository bookings;
    private final CheckInService checkIn;
    private final QrCodeService qrCodes;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String publicBaseUrl;
    private final boolean enabled;

    public BookingTicketEmailListener(BookingRepository bookings, CheckInService checkIn,
                                      QrCodeService qrCodes, JavaMailSender mailSender,
                                      TemplateEngine templateEngine,
                                      @Value("${spring.mail.username:}") String fromEmail,
                                      @Value("${app.public-base-url:}") String publicBaseUrl,
                                      @Value("${app.mail.tickets.enabled:false}") boolean enabled) {
        this.bookings = bookings;
        this.checkIn = checkIn;
        this.qrCodes = qrCodes;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        this.enabled = enabled;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendTicketAfterConfirmation(BookingConfirmedEvent event) {
        if (!enabled) return;

        try {
            Booking booking = bookings.findById(event.bookingId()).orElse(null);
            if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED
                    || booking.getUser() == null || booking.getUser().getEmail() == null
                    || booking.getUser().getEmail().isBlank()) return;

            String qrCid = "bookingTicketQr";
            byte[] qrPng = qrCodes.png(checkIn.qrPayloadFor(booking.getId()), 420);
            Context context = new Context(VIETNAMESE);
            context.setVariables(templateData(booking, qrCid));
            String html = templateEngine.process("email/booking-ticket", context);
            String text = plainText(booking);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Rex Cinemas");
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Vé điện tử Rex Cinemas — " + booking.getShowtime().getMovie().getTitle());
            helper.setText(text, html);
            helper.addInline(qrCid, new ByteArrayResource(qrPng), "image/png");
            mailSender.send(message);
        } catch (Exception exception) {
            // Payment is already committed. A mail provider outage must never undo the ticket.
            log.warn("Không gửi được email vé cho booking id {}. Khách vẫn có thể xem vé trong tài khoản.",
                    event.bookingId());
        }
    }

    private Map<String, Object> templateData(Booking booking, String qrCid) {
        Map<String, Object> data = new HashMap<>();
        var showtime = booking.getShowtime();
        var cinema = showtime.getAuditorium().getCinema();
        data.put("customerName", Objects.toString(booking.getUser().getFullName(), "Quý khách"));
        data.put("movieTitle", showtime.getMovie().getTitle());
        data.put("cinemaName", cinema.getName());
        data.put("cinemaAddress", cinema.getAddress());
        data.put("auditoriumName", showtime.getAuditorium().getName());
        data.put("showtimeDate", showtime.getStartTime().format(DATE));
        data.put("showtimeTime", showtime.getStartTime().format(TIME));
        data.put("bookingCode", booking.getBookingCode());
        data.put("seatLines", booking.getSeats().stream()
                .sorted(Comparator.comparing(seat -> seat.getSeatLabel() == null ? "" : seat.getSeatLabel()))
                .map(seat -> Map.of("label", Objects.toString(seat.getSeatLabel(), "—"),
                        "price", money(seat.getPrice())))
                .toList());
        data.put("comboLines", booking.getCombos().stream()
                .map(combo -> Map.of("name", combo.getComboName(), "quantity", combo.getQuantity(),
                        "lineTotal", money(combo.getLineTotal())))
                .toList());
        data.put("seatAmount", money(booking.getSeatAmount()));
        data.put("comboAmount", money(booking.getComboAmount()));
        data.put("discountAmount", money(booking.getDiscountAmount()));
        data.put("hasDiscount", booking.getDiscountAmount() != null
                && booking.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0);
        data.put("totalAmount", money(booking.getTotalAmount()));
        boolean complimentary = booking.getTotalAmount() == null
                || booking.getTotalAmount().compareTo(BigDecimal.ZERO) == 0;
        data.put("confirmationLabel", complimentary ? "ĐẶT VÉ THÀNH CÔNG" : "THANH TOÁN THÀNH CÔNG");
        data.put("totalLabel", complimentary ? "Tổng cộng" : "Đã thanh toán");
        data.put("paymentReference", Objects.toString(booking.getPaymentReference(), ""));
        data.put("qrCid", qrCid);
        data.put("ticketUrl", publicBaseUrl.isBlank() ? "" : publicBaseUrl + "/bookings/" + booking.getId());
        return data;
    }

    private String plainText(Booking booking) {
        var showtime = booking.getShowtime();
        String seats = booking.getSeats().stream().map(seat -> seat.getSeatLabel())
                .filter(Objects::nonNull).sorted().collect(Collectors.joining(", "));
        return "VÉ ĐIỆN TỬ REX CINEMAS\n\n"
                + "Xin chào " + Objects.toString(booking.getUser().getFullName(), "Quý khách") + ",\n"
                + (booking.getTotalAmount() != null && booking.getTotalAmount().signum() == 0
                    ? "Đặt vé thành công, bạn không cần thanh toán. Mã đặt vé: "
                    : "Thanh toán đã thành công. Mã đặt vé: ") + booking.getBookingCode() + "\n"
                + "Phim: " + showtime.getMovie().getTitle() + "\n"
                + "Rạp: " + showtime.getAuditorium().getCinema().getName() + " — "
                + showtime.getAuditorium().getName() + "\n"
                + "Suất: " + showtime.getStartTime().format(TIME) + " — " + showtime.getStartTime().format(DATE) + "\n"
                + "Ghế: " + seats + "\n"
                + (booking.getTotalAmount() != null && booking.getTotalAmount().signum() == 0
                    ? "Tổng cộng: " : "Đã thanh toán: ") + money(booking.getTotalAmount()) + "\n\n"
                + "Mã QR check-in được đính kèm trong email. Vui lòng xuất trình mã khi đến rạp.";
    }

    private static String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(VIETNAMESE);
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " ₫";
    }
}
