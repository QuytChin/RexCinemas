package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.AccountNotification;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.repository.AccountNotificationRepository;
import com.rexchain.cinema.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final AccountNotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(AccountNotificationRepository notifications) {
        this(notifications, null);
    }
    @Autowired
    public NotificationService(AccountNotificationRepository notifications, UserRepository users) {
        this.notifications = notifications; this.users = users;
    }

    @Transactional
    public void paymentConfirmed(Booking booking) {
        create(booking, "PAYMENT_CONFIRMED", "Thanh toán thành công",
                "Vé " + booking.getBookingCode() + " đã được xác nhận.", "confirmed");
    }
    @Transactional
    public void bookingCancelled(Booking booking) {
        create(booking, "BOOKING_CANCELLED", "Đặt vé đã hủy",
                "Giao dịch cho vé " + booking.getBookingCode() + " đã được hủy.", "cancelled");
    }
    @Transactional
    public void refundPending(Booking booking) {
        create(booking, "REFUND_PENDING", "Yêu cầu hoàn tiền đang chờ xử lý",
                "Giao dịch cho vé " + booking.getBookingCode()
                        + " đang chờ rạp xác nhận chuyển khoản hoàn tiền. Chưa có xác nhận tiền đã về tài khoản.",
                "refund-pending");
    }
    @Transactional
    public void refundProcessed(Booking booking) {
        create(booking, "REFUND_COMPLETED", "Hoàn tiền đã được xác nhận",
                "Rạp đã xác nhận chuyển khoản hoàn tiền thủ công cho vé " + booking.getBookingCode() + ".", "refunded");
    }
    @Transactional
    public void latePaymentPending(Booking booking) {
        latePaymentPending(booking, null);
    }
    @Transactional
    public void latePaymentPending(Booking booking, String providerTransactionId) {
        String suffix = "late-payment-pending:" + (providerTransactionId == null || providerTransactionId.isBlank()
                ? booking.getBookingCode() : providerTransactionId.trim());
        create(booking, "LATE_PAYMENT_PENDING", "Giao dịch đến muộn đang chờ xử lý",
                "Rạp đã ghi nhận giao dịch đến sau hạn thanh toán cho vé " + booking.getBookingCode()
                        + ". Booking không được xác nhận; yêu cầu hoàn tiền thủ công đang được xử lý.",
                suffix);
        if (users != null) {
            for (User admin : users.findByRole(Role.ADMIN)) {
                createForUser(admin, "LATE_PAYMENT_ADMIN", "Cảnh báo giao dịch đến muộn",
                        "Booking " + booking.getBookingCode()
                                + " có tiền vào sau hạn thanh toán; cần đối soát và hoàn tiền.",
                        "admin:" + suffix, "/admin/late-payments");
            }
        }
    }
    @Transactional
    public void latePaymentRefunded(Booking booking) {
        create(booking, "LATE_PAYMENT_REFUNDED", "Hoàn tiền giao dịch đến muộn đã được xác nhận",
                "Rạp đã xác nhận chuyển khoản hoàn tiền thủ công cho giao dịch đến muộn của vé "
                        + booking.getBookingCode() + ".", "late-payment-refunded");
    }
    private void create(Booking booking, String type, String title, String message, String suffix) {
        User user = booking.getUser();
        if (user == null || user.getId() == null) return;
        createForUser(user, type, title, message, booking.getBookingCode() + ":" + suffix,
                "/bookings/" + booking.getId());
    }
    private void createForUser(User user, String type, String title, String message, String key, String link) {
        if (user == null || user.getId() == null) return;
        if (notifications.existsByUserIdAndDeduplicationKey(user.getId(), key)) return;
        AccountNotification notification = new AccountNotification();
        notification.setUser(user); notification.setType(type); notification.setTitle(title);
        notification.setMessage(message); notification.setLink(link);
        notification.setDeduplicationKey(key); notifications.save(notification);
    }
    @Transactional(readOnly = true)
    public List<AccountNotification> latest(Long userId, int size) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.min(Math.max(size, 1), 20))).getContent();
    }
    @Transactional(readOnly = true)
    public Page<AccountNotification> page(Long userId, int page) { return notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), 20)); }
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return notifications.countByUserIdAndReadAtPresentFalse(userId); }
    @Transactional
    public boolean markRead(Long notificationId, Long userId) {
        return notifications.findByIdAndUserId(notificationId, userId).map(notification -> { notification.markRead(); return true; }).orElse(false);
    }
}
