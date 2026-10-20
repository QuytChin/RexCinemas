package com.rexchain.cinema.config;

import org.springframework.stereotype.Component;

import java.util.Map;

/** Vietnamese labels for values rendered by the web UI; stored/API values stay unchanged. */
@Component("uiText")
public class UiText {
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("id", "Mã"), Map.entry("name", "Tên"), Map.entry("city", "Thành phố"),
            Map.entry("status", "Trạng thái"), Map.entry("createdAt", "Ngày tạo"),
            Map.entry("receivedAt", "Thời gian nhận"), Map.entry("totalAmount", "Tổng tiền"),
            Map.entry("amount", "Số tiền"), Map.entry("sortOrder", "Thứ tự"),
            Map.entry("publishDate", "Ngày đăng"), Map.entry("releaseDate", "Ngày khởi chiếu"),
            Map.entry("title", "Tiêu đề"), Map.entry("email", "Email"),
            Map.entry("fullName", "Họ và tên"), Map.entry("pointsCost", "Điểm đổi"),
            Map.entry("NOW_SHOWING", "Đang chiếu"), Map.entry("COMING_SOON", "Sắp chiếu"),
            Map.entry("ENDED", "Đã kết thúc"),
            Map.entry("price", "Giá"), Map.entry("validUntil", "Ngày hết hạn"),
            Map.entry("code", "Mã"), Map.entry("PENDING", "Chờ thanh toán"),
            Map.entry("CONFIRMED", "Đã xác nhận"), Map.entry("CANCELLED", "Đã hủy"),
            Map.entry("EXPIRED", "Hết hạn"), Map.entry("REFUND_PENDING", "Đang chờ hoàn tiền"),
            Map.entry("REFUNDED", "Đã hoàn tiền"), Map.entry("RECEIVED", "Đã tiếp nhận"),
            Map.entry("VERIFIED", "Đã xác minh"), Map.entry("SETTLED", "Đã thanh toán"),
            Map.entry("NEEDS_REVIEW", "Cần kiểm tra"),
            Map.entry("LATE_PAYMENT_REFUND_PENDING", "Chờ hoàn tiền giao dịch đến muộn"),
            Map.entry("DUPLICATE", "Giao dịch trùng"),
            Map.entry("PENDING_REFUND", "Chờ hoàn tiền"),
            Map.entry("REFUND_FAILED", "Hoàn tiền thất bại"),
            Map.entry("PAY_AT_COUNTER", "Thanh toán tại quầy"),
            Map.entry("MOMO_SANDBOX", "MoMo (môi trường thử nghiệm)"),
            Map.entry("VNPAY_SANDBOX", "VNPAY (môi trường thử nghiệm)"),
            Map.entry("BANK_QR", "QR ngân hàng"),
            Map.entry("ADMIN", "Quản trị viên"),
            Map.entry("CINEMA_MANAGER", "Quản lý rạp"),
            Map.entry("MEMBER", "Thành viên"), Map.entry("SILVER", "Bạc"),
            Map.entry("GOLD", "Vàng"), Map.entry("DIAMOND", "Kim cương")
    );

    public String label(Object value) {
        if (value == null) return "";
        String key = value instanceof Enum<?> enumValue ? enumValue.name() : value.toString();
        String exact = LABELS.get(key);
        if (exact != null) return exact;
        return key.replace('_', ' ');
    }
}
