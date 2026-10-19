package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.AccountNotification;
import com.rexchain.cinema.entity.Booking;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.AccountNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final AccountNotificationRepository notifications;
    public NotificationService(AccountNotificationRepository notifications) { this.notifications = notifications; }

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
    public void refundProcessed(Booking booking) {
        create(booking, "REFUND_COMPLETED", "Hoàn tiền đã xử lý",
                "Yêu cầu hoàn tiền cho vé " + booking.getBookingCode() + " đã được xác nhận.", "refunded");
    }
    private void create(Booking booking, String type, String title, String message, String suffix) {
        User user = booking.getUser();
        if (user == null || user.getId() == null) return;
        String key = booking.getBookingCode() + ":" + suffix;
        if (notifications.existsByUserIdAndDeduplicationKey(user.getId(), key)) return;
        AccountNotification notification = new AccountNotification();
        notification.setUser(user); notification.setType(type); notification.setTitle(title);
        notification.setMessage(message); notification.setLink("/bookings/" + booking.getId());
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
