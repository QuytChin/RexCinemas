package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<SupportMessage> findBySessionIdAndCinemaIdOrderByCreatedAtAsc(String sessionId, Long cinemaId);

    List<SupportMessage> findBySessionIdAndCinemaIsNullOrderByCreatedAtAsc(String sessionId);

    List<SupportMessage> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    @Modifying
    @Query("UPDATE SupportMessage m SET m.read = true WHERE m.sessionId = :sessionId AND m.senderType = com.rexchain.cinema.entity.SenderType.CUSTOMER")
    int markAsReadBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE SupportMessage m SET m.read = true WHERE m.sessionId = :sessionId AND m.cinema.id = :cinemaId AND m.senderType = com.rexchain.cinema.entity.SenderType.CUSTOMER")
    int markAsReadBySessionIdAndCinemaId(@Param("sessionId") String sessionId, @Param("cinemaId") Long cinemaId);

    @Modifying
    @Query("UPDATE SupportMessage m SET m.read = true WHERE m.sessionId = :sessionId AND m.cinema IS NULL AND m.senderType = com.rexchain.cinema.entity.SenderType.CUSTOMER")
    int markAsReadBySessionIdAndNoCinema(@Param("sessionId") String sessionId);

    interface ConversationSummary {
        String getSessionId();
        String getCustomerName();
        String getCustomerEmail();
        Long getCinemaId();
        String getCinemaName();
        String getLastMessage();
        String getLastSenderType();
        LocalDateTime getLastMessageAt();
        Long getUnreadCount();
    }

    @Query(value = """
        SELECT 
            sm.session_id AS sessionId,
            COALESCE(u.full_name, sm.customer_name, 'Khách vãng lai') AS customerName,
            COALESCE(u.email, sm.customer_email, '') AS customerEmail,
            c.id AS cinemaId,
            c.name AS cinemaName,
            (SELECT m2.message FROM support_messages m2 WHERE m2.session_id = sm.session_id
                AND m2.cinema_id <=> c.id
                ORDER BY m2.created_at DESC, m2.id DESC LIMIT 1) AS lastMessage,
            (SELECT m2.sender_type FROM support_messages m2 WHERE m2.session_id = sm.session_id
                AND m2.cinema_id <=> c.id
                ORDER BY m2.created_at DESC, m2.id DESC LIMIT 1) AS lastSenderType,
            MAX(sm.created_at) AS lastMessageAt,
            SUM(CASE WHEN sm.sender_type = 'CUSTOMER' AND sm.is_read = 0 THEN 1 ELSE 0 END) AS unreadCount
        FROM support_messages sm
        LEFT JOIN app_users u ON u.id = sm.customer_id
        LEFT JOIN cinemas c ON c.id = sm.cinema_id
        WHERE (:cinemaId IS NULL OR sm.cinema_id = :cinemaId)
        GROUP BY sm.session_id, customerName, customerEmail, c.id, c.name
        ORDER BY lastMessageAt DESC
        """, nativeQuery = true)
    List<ConversationSummary> findRecentConversations(@Param("cinemaId") Long cinemaId);

    long countByCinemaIdAndReadFalseAndSenderType(Long cinemaId, com.rexchain.cinema.entity.SenderType senderType);
}
