package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.BookingStatus;
import com.rexchain.cinema.entity.PaymentMethod;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.BookingRepository;
import com.rexchain.cinema.repository.UserRepository;
import com.rexchain.cinema.service.BookingService;
import com.rexchain.cinema.service.MomoPaymentService;
import com.rexchain.cinema.service.SePayPaymentService;
import com.rexchain.cinema.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class PaymentController {
    private final BookingRepository bookings;
    private final UserRepository users;
    private final BookingService bookingService;
    private final MomoPaymentService momo;
    private final VnpayPaymentService vnpay;
    private final SePayPaymentService sepay;

    public PaymentController(
            BookingRepository bookings,
            UserRepository users,
            BookingService bookingService,
            MomoPaymentService momo,
            VnpayPaymentService vnpay,
            SePayPaymentService sepay) {
        this.bookings = bookings;
        this.users = users;
        this.bookingService = bookingService;
        this.momo = momo;
        this.vnpay = vnpay;
        this.sepay = sepay;
    }

    @GetMapping("/payment/{bookingId}/start")
    String start(@PathVariable Long bookingId, Authentication authentication, HttpServletRequest request) {
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

        try {
            if (booking.getPaymentMethod() == PaymentMethod.MOMO_SANDBOX) {
                return "redirect:" + momo.createPaymentUrl(booking);
            }
            if (booking.getPaymentMethod() == PaymentMethod.VNPAY_SANDBOX) {
                return "redirect:" + vnpay.createPaymentUrl(booking, request);
            }
            throw new IllegalStateException("Phương thức thanh toán không hợp lệ");
        } catch (Exception ex) {
            // Starting a provider redirect is a GET. Keep the pending booking intact so a
            // cross-site GET or a temporary provider outage cannot cancel the user's hold.
            return "redirect:/bookings/" + booking.getId() + "?paymentError";
        }
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
            sepay.requireAvailable();
            model.addAttribute("booking", booking);
            model.addAttribute("bankAccount", sepay.bankAccount());
            model.addAttribute("qrImageUrl", sepay.qrImageUrl(booking.getPaymentReference(), booking.getTotalAmount()));
            return "booking/bank-qr-waiting";
        } catch (Exception ex) {
            return "redirect:/bookings/" + booking.getId() + "?paymentError";
        }
    }

    @GetMapping("/payment/callback/momo")
    String momoReturn(@RequestParam Map<String, String> params) {
        String code = params.get("orderId");
        Booking booking = code == null ? null : bookings.findByBookingCode(code).orElse(null);
        if (booking == null) {
            return "redirect:/?paymentError";
        }

        boolean signatureValid = booking.getPaymentMethod() == PaymentMethod.MOMO_SANDBOX
                && momo.verifyResult(params);
        boolean successfulGatewayResult = signatureValid
                && "0".equals(params.get("resultCode"))
                && momoAmountMatches(booking, params.get("amount"));

        try {
            if (successfulGatewayResult) {
                booking = bookingService.completeOnlinePayment(code, params.get("transId"));
            } else if (signatureValid) {
                booking = bookingService.cancelOnlinePayment(code);
            }
        } catch (Exception ex) {
            successfulGatewayResult = false;
        }

        return paymentRedirect(booking, successfulGatewayResult);
    }

    @PostMapping("/payment/callback/momo/ipn")
    @ResponseBody
    ResponseEntity<?> momoIpn(@RequestBody Map<String, Object> params) {
        String code = text(params.get("orderId"));
        Booking booking = bookings.findByBookingCode(code).orElse(null);
        if (booking == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "booking not found"));
        }
        if (booking.getPaymentMethod() != PaymentMethod.MOMO_SANDBOX || !momo.verifyResult(params)) {
            return ResponseEntity.badRequest().body(Map.of("message", "invalid signature"));
        }
        if (!momoAmountMatches(booking, text(params.get("amount")))) {
            return ResponseEntity.badRequest().body(Map.of("message", "invalid amount"));
        }

        try {
            if ("0".equals(text(params.get("resultCode")))) {
                bookingService.completeOnlinePayment(code, text(params.get("transId")));
            } else {
                bookingService.cancelOnlinePayment(code);
            }
            return ResponseEntity.ok(Map.of("message", "received"));
        } catch (Exception ex) {
            return ResponseEntity.status(409).body(Map.of("message", "booking is no longer payable"));
        }
    }

    @GetMapping("/payment/callback/vnpay")
    String vnpayReturn(@RequestParam Map<String, String> params) {
        String code = params.get("vnp_TxnRef");
        Booking booking = code == null ? null : bookings.findByBookingCode(code).orElse(null);
        if (booking == null) {
            return "redirect:/?paymentError";
        }

        boolean signatureValid = booking.getPaymentMethod() == PaymentMethod.VNPAY_SANDBOX
                && vnpay.verify(params);
        boolean successfulGatewayResult = signatureValid
                && "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"))
                && vnpay.expectedAmount(booking).equals(params.get("vnp_Amount"));

        try {
            if (successfulGatewayResult) {
                booking = bookingService.completeOnlinePayment(code, params.get("vnp_TransactionNo"));
            } else if (signatureValid) {
                booking = bookingService.cancelOnlinePayment(code);
            }
        } catch (Exception ex) {
            successfulGatewayResult = false;
        }

        return paymentRedirect(booking, successfulGatewayResult);
    }

    /**
     * Server-to-server IPN endpoint. Configure this URL in the VNPAY sandbox merchant settings:
     * {PAYMENT_BASE_URL}/payment/callback/vnpay/ipn
     */
    @GetMapping("/payment/callback/vnpay/ipn")
    @ResponseBody
    Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        if (!vnpay.verify(params)) {
            return vnpayReply("97", "Invalid Signature");
        }

        String code = params.get("vnp_TxnRef");
        Booking booking = code == null ? null : bookings.findByBookingCode(code).orElse(null);
        if (booking == null || booking.getPaymentMethod() != PaymentMethod.VNPAY_SANDBOX) {
            return vnpayReply("01", "Order not found");
        }
        if (!vnpay.expectedAmount(booking).equals(params.get("vnp_Amount"))) {
            return vnpayReply("04", "Invalid amount");
        }

        try {
            boolean successful = "00".equals(params.get("vnp_ResponseCode"))
                    && "00".equals(params.get("vnp_TransactionStatus"));
            if (successful) {
                bookingService.completeOnlinePayment(code, params.get("vnp_TransactionNo"));
            } else {
                bookingService.cancelOnlinePayment(code);
            }
            return vnpayReply("00", "Confirm Success");
        } catch (Exception ex) {
            return vnpayReply("99", "Unknown error");
        }
    }

    private boolean momoAmountMatches(Booking booking, String amount) {
        try {
            return new BigDecimal(amount).compareTo(booking.getTotalAmount()) == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private Map<String, String> vnpayReply(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String paymentRedirect(Booking booking, boolean gatewaySuccess) {
        String query;
        if (booking.getStatus() == BookingStatus.CONFIRMED) query = "?success";
        else if (booking.getStatus() == BookingStatus.REFUND_PENDING) query = "?refundPending";
        else query = gatewaySuccess ? "?paymentPending" : "?paymentError";
        return "redirect:/bookings/" + booking.getId() + query;
    }
}
