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
            Map.entry("PAY_AT_COUNTER", "Thanh toán tại quầy"),
            Map.entry("MOMO_SANDBOX", "MoMo (môi trường thử nghiệm)"),
            Map.entry("VNPAY_SANDBOX", "VNPAY (môi trường thử nghiệm)"),
            Map.entry("BANK_QR", "QR ngân hàng"),
            Map.entry("ADMIN", "Quản trị viên"),
            Map.entry("CINEMA_MANAGER", "Quản lý rạp"),
            Map.entry("MEMBER", "Thành viên"), Map.entry("SILVER", "Bạc"),
            Map.entry("GOLD", "Vàng"), Map.entry("DIAMOND", "Kim cương"),
            Map.entry("startTime", "Giờ chiếu"), Map.entry("basePrice", "Giá vé"),
            Map.entry("rating", "Điểm đánh giá"),
            Map.entry("active", "Hiển thị"), Map.entry("type", "Loại nội dung"),
            Map.entry("role", "Vai trò"), Map.entry("transactionType", "Loại biến động"),
            Map.entry("pointsDelta", "Biến động điểm"),
            Map.entry("startDate", "Ngày bắt đầu"), Map.entry("endDate", "Ngày kết thúc"),
            Map.entry("TRAILER", "Trailer"), Map.entry("CLIP", "Clip"),
            Map.entry("POSTER", "Poster"), Map.entry("BANNER", "Banner"),
            Map.entry("PERCENT", "Phần trăm"), Map.entry("FIXED", "Số tiền cố định"),
            Map.entry("CUSTOMER", "Khách hàng"), Map.entry("STAFF", "Nhân viên"),
            Map.entry("EARN", "Tích điểm"), Map.entry("REDEEM", "Đổi điểm"),
            Map.entry("REVERSE_EARN", "Thu hồi điểm"), Map.entry("REFUND_REDEMPTION", "Hoàn điểm đã đổi"),
            Map.entry("ADMIN_ADJUSTMENT", "Điều chỉnh thủ công"), Map.entry("MIGRATION_OPENING", "Số dư đầu kỳ"),
            Map.entry("ADJUST", "Điều chỉnh"), Map.entry("EXPIRE", "Hết hạn"),
            Map.entry("true", "Đang bật"), Map.entry("false", "Đã tắt"),
            Map.entry("OPENED", "Mở hồ sơ"), Map.entry("MARKED_IN_REVIEW", "Đang xử lý"),
            Map.entry("REFUND_CONFIRMED", "Đã xác nhận hoàn tiền"), Map.entry("REFUND_FAILED", "Hoàn tiền thất bại")
    );

    public String label(Object value) {
        if (value == null) return "";
        String key = value instanceof Enum<?> enumValue ? enumValue.name() : value.toString();
        String exact = LABELS.get(key);
        if (exact != null) return exact;
        return key.replace('_', ' ');
    }
}
