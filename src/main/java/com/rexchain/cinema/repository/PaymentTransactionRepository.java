package com.rexchain.cinema.repository;

import com.rexchain.cinema.dto.PaymentTransactionDto;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentTransactionRepository {
    private final JdbcTemplate jdbc;

    public PaymentTransactionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isSchemaAvailable() {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class,
                    "payment_transactions");
            return count != null && count > 0;
        } catch (DataAccessException ex) { return false; }
    }

    public boolean insertIfAbsent(PaymentTransactionDto tx) {
        if (!isSchemaAvailable()) throw new IllegalStateException("Migration transaction chưa được áp dụng");
        try {
            jdbc.update("INSERT INTO payment_transactions(provider_transaction_id, gateway, transaction_date, " +
                            "account_number_last4, code, content, transfer_type, amount, reference_code, status, received_at) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                    tx.getProviderTransactionId(), tx.getGateway(), tx.getTransactionDate(), tx.getAccountNumberLast4(),
                    tx.getCode(), tx.getContent(), tx.getTransferType(), tx.getAmount(), tx.getReferenceCode(), "RECEIVED");
            return true;
        } catch (DuplicateKeyException ex) { return false; }
    }

    public Optional<PaymentTransactionDto> findByProviderId(String providerId) {
        if (!isSchemaAvailable()) return Optional.empty();
        return jdbc.query("SELECT * FROM payment_transactions WHERE provider_transaction_id=?", this::map, providerId)
                .stream().findFirst();
    }

    public Optional<PaymentTransactionDto> findById(Long id) {
        if (!isSchemaAvailable()) return Optional.empty();
        return jdbc.query("SELECT * FROM payment_transactions WHERE id=?", this::map, id).stream().findFirst();
    }

    public List<PaymentTransactionDto> search(String status, LocalDate from, LocalDate to, String query) {
        if (!isSchemaAvailable()) return List.of();
        StringBuilder sql = new StringBuilder("SELECT * FROM payment_transactions WHERE 1=1");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND status=?"); args.add(status.trim()); }
        if (from != null) { sql.append(" AND received_at>=?"); args.add(from.atStartOfDay()); }
        if (to != null) { sql.append(" AND received_at<?"); args.add(to.plusDays(1).atStartOfDay()); }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (provider_transaction_id LIKE ? OR booking_code LIKE ? OR payment_reference LIKE ? OR code LIKE ?)");
            String value = "%" + query.trim() + "%";
            args.add(value); args.add(value); args.add(value); args.add(value);
        }
        sql.append(" ORDER BY received_at DESC");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public void updateStatus(String providerId, String status, String bookingCode, String paymentReference, String reason) {
        jdbc.update("UPDATE payment_transactions SET status=?, booking_code=?, payment_reference=?, reason=?, " +
                        "verified_at=CASE WHEN ? IN ('VERIFIED','SETTLED','REFUND_PENDING','LATE_PAYMENT_REFUND_PENDING') THEN COALESCE(verified_at,CURRENT_TIMESTAMP) ELSE verified_at END, " +
                        "settled_at=CASE WHEN ? IN ('SETTLED','REFUND_PENDING') THEN COALESCE(settled_at,CURRENT_TIMESTAMP) ELSE settled_at END " +
                        "WHERE provider_transaction_id=?", status, bookingCode, paymentReference, reason, status, status, providerId);
    }

    public void audit(Long transactionId, String action, String actor, String details) {
        if (transactionId == null) return;
        jdbc.update("INSERT INTO payment_transaction_audit(transaction_id, action, actor, details, created_at) " +
                        "VALUES(?,?,?,?,CURRENT_TIMESTAMP)", transactionId, action, actor, details == null ? "" : details);
    }

    private PaymentTransactionDto map(ResultSet rs, int row) throws java.sql.SQLException {
        PaymentTransactionDto tx = new PaymentTransactionDto();
        tx.setId(rs.getLong("id")); tx.setProviderTransactionId(rs.getString("provider_transaction_id"));
        tx.setBookingCode(rs.getString("booking_code")); tx.setPaymentReference(rs.getString("payment_reference"));
        tx.setGateway(rs.getString("gateway")); tx.setTransactionDate(rs.getString("transaction_date"));
        tx.setAccountNumberLast4(rs.getString("account_number_last4")); tx.setCode(rs.getString("code"));
        tx.setContent(rs.getString("content")); tx.setTransferType(rs.getString("transfer_type"));
        tx.setAmount(rs.getBigDecimal("amount")); tx.setReferenceCode(rs.getString("reference_code"));
        tx.setStatus(rs.getString("status")); tx.setReason(rs.getString("reason"));
        tx.setReceivedAt(toDateTime(rs, "received_at")); tx.setVerifiedAt(toDateTime(rs, "verified_at"));
        tx.setSettledAt(toDateTime(rs, "settled_at"));
        return tx;
    }

    private LocalDateTime toDateTime(ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
