package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.BankTransferPaymentService;
import com.rexchain.cinema.dto.BankTransferSnapshot;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

@Controller
public class PaymentController {
    private final BookingRepository bookings;
    private final UserRepository users;
    private final BankTransferPaymentService bankTransferPaymentService;

    public PaymentController(
            BookingRepository bookings,
            UserRepository users,
            BankTransferPaymentService bankTransferPaymentService) {
        this.bookings = bookings;
        this.users = users;
        this.bankTransferPaymentService = bankTransferPaymentService;
    }

    @GetMapping("/payment/{bookingId}/start")
    String start(@PathVariable Long bookingId, Authentication authentication) {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Không có quyền thanh toán booking này");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return "redirect:/bookings/" + booking.getId();
        }

        if (booking.getPaymentMethod() == PaymentMethod.BANK_QR) {
            return "redirect:/payment/" + booking.getId() + "/bank-qr";
        }

        return "redirect:/bookings/" + booking.getId() + "?paymentError";
    }

    @GetMapping("/payment/{bookingId}/bank-qr")
    String bankQr(@PathVariable Long bookingId, Authentication authentication, Model model) {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if (!booking.getUser().getId().equals(user.getId()))
            throw new IllegalStateException("Không có quyền xem thanh toán booking này");
        if (booking.getPaymentMethod() != PaymentMethod.BANK_QR)
            return "redirect:/payment/" + booking.getId() + "/start";
        if (booking.getStatus() != BookingStatus.PENDING)
            return "redirect:/bookings/" + booking.getId();
        try {
            BankTransferSnapshot snapshot = bankTransferPaymentService.findSnapshot(booking.getBookingCode())
                    .orElseThrow(() -> new IllegalStateException("Booking chưa có snapshot VietQR hợp lệ"));
            if (snapshot.getExpiresAt() == null || !snapshot.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                return "redirect:/bookings/" + booking.getId() + "?paymentExpired";
            model.addAttribute("booking", booking);
            model.addAttribute("bankSnapshot", snapshot);
            model.addAttribute("bankAccount", snapshot.getBankAccount());
            model.addAttribute("bankName", snapshot.getBankName());
            model.addAttribute("accountHolder", snapshot.getAccountHolder());
            model.addAttribute("qrImageUrl", bankTransferPaymentService.qrImageUrl(snapshot));
            return "booking/bank-qr-waiting";
        } catch (Exception ex) {
            return "redirect:/bookings/" + booking.getId() + "?paymentError";
        }
    }

}
