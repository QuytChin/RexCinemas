package com.rexchain.cinema.dto;

import com.rexchain.cinema.entity.SenderType;
import com.rexchain.cinema.entity.SupportMessage;

import java.time.format.DateTimeFormatter;

public record SupportMessageDto(
        Long id,
        String sessionId,
        Long cinemaId,
        String cinemaName,
        String senderType,
        String senderName,
        String message,
        boolean isRead,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public static SupportMessageDto fromEntity(SupportMessage msg) {
        String senderName;
        if (msg.getSenderType() == SenderType.AI) {
            senderName = "Trợ lý Rex AI 🤖";
        } else if (msg.getSenderType() == SenderType.STAFF) {
            senderName = "Quản trị / CSKH Rạp 👨‍💼";
        } else {
            senderName = msg.getCustomer() != null ? msg.getCustomer().getFullName() :
                    (msg.getCustomerName() != null ? msg.getCustomerName() : "Khách hàng");
        }

        return new SupportMessageDto(
                msg.getId(),
                msg.getSessionId(),
                msg.getCinema() != null ? msg.getCinema().getId() : null,
                msg.getCinema() != null ? msg.getCinema().getName() : "Rex Cinemas",
                msg.getSenderType().name(),
                senderName,
                msg.getMessage(),
                msg.isRead(),
                msg.getCreatedAt() != null ? msg.getCreatedAt().format(FORMATTER) : ""
        );
    }
}
