package com.rexchain.cinema.repository;

import com.rexchain.cinema.dto.BankTransferSnapshot;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BankTransferSnapshotRepository {
    private final JdbcTemplate jdbc;
    private final DatabaseSchemaResolver schemaResolver;

    public BankTransferSnapshotRepository(JdbcTemplate jdbc, DatabaseSchemaResolver schemaResolver) {
        this.jdbc = jdbc;
        this.schemaResolver = schemaResolver;
    }

    public boolean isSchemaAvailable() {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = ? AND LOWER(table_name) = LOWER(?)", Integer.class,
                    schemaResolver.currentSchema(), "bank_transfer_snapshots");
            return count != null && count > 0;
        } catch (DataAccessException ex) { return false; }
    }

    public void save(BankTransferSnapshot snapshot) {
        if (!isSchemaAvailable()) throw new IllegalStateException("Migration VietQR snapshot chưa được áp dụng");
        jdbc.update("INSERT INTO bank_transfer_snapshots(booking_code, bank_name, bank_code, bank_account, " +
                        "account_holder, amount, payment_reference, expires_at, created_at) VALUES(?,?,?,?,?,?,?, ?,CURRENT_TIMESTAMP)",
                snapshot.getBookingCode(), snapshot.getBankName(), snapshot.getBankCode(), snapshot.getBankAccount(),
                snapshot.getAccountHolder(), snapshot.getAmount(), snapshot.getPaymentReference(), snapshot.getExpiresAt());
    }

    public Optional<BankTransferSnapshot> findByBookingCode(String bookingCode) {
        if (!isSchemaAvailable()) return Optional.empty();
        return jdbc.query("SELECT booking_code, bank_name, bank_code, bank_account, account_holder, amount, " +
                        "payment_reference, expires_at FROM bank_transfer_snapshots WHERE booking_code=?", (rs, row) -> {
                    BankTransferSnapshot snapshot = new BankTransferSnapshot();
                    snapshot.setBookingCode(rs.getString("booking_code")); snapshot.setBankName(rs.getString("bank_name"));
                    snapshot.setBankCode(rs.getString("bank_code")); snapshot.setBankAccount(rs.getString("bank_account"));
                    snapshot.setAccountHolder(rs.getString("account_holder")); snapshot.setAmount(rs.getBigDecimal("amount"));
                    snapshot.setPaymentReference(rs.getString("payment_reference"));
                    snapshot.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    return snapshot;
                }, bookingCode).stream().findFirst();
    }
}
