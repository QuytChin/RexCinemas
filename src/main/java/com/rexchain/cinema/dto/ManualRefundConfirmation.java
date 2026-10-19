package com.rexchain.cinema.dto;

import com.rexchain.cinema.entity.Booking;
import java.math.BigDecimal;

/** Evidence recorded after an administrator has completed a bank transfer outside RexCinemas. */
public record ManualRefundConfirmation(
        String transferReference,
        BigDecimal amount,
        String note,
        String evidence) {

    public ManualRefundConfirmation {
        transferReference = normalize(transferReference);
        note = normalize(note);
        evidence = normalize(evidence);
    }

    public void validateFor(Booking booking) {
        if (transferReference == null || transferReference.isBlank())
            throw new IllegalArgumentException("Phải nhập mã tham chiếu hoặc chứng từ chuyển khoản hoàn tiền");
        if (transferReference.length() > 120)
            throw new IllegalArgumentException("Mã tham chiếu hoàn tiền tối đa 120 ký tự");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0");
        if (booking.getTotalAmount() == null || amount.compareTo(booking.getTotalAmount()) != 0)
            throw new IllegalArgumentException("Hệ thống hiện chỉ hỗ trợ hoàn toàn bộ đúng bằng tổng tiền booking");
        if (note != null && note.length() > 255)
            throw new IllegalArgumentException("Ghi chú hoàn tiền tối đa 255 ký tự");
        if (evidence != null && evidence.length() > 600)
            throw new IllegalArgumentException("Thông tin bằng chứng hoàn tiền tối đa 600 ký tự");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
