package com.rexchain.cinema.repository;

import com.rexchain.cinema.dto.PaymentSettingsDto;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

@Repository
public class PaymentSettingsRepository {
    private final JdbcTemplate jdbc;
    private final DatabaseSchemaResolver schemaResolver;

    public PaymentSettingsRepository(JdbcTemplate jdbc, DatabaseSchemaResolver schemaResolver) {
        this.jdbc = jdbc;
        this.schemaResolver = schemaResolver;
    }

    public boolean isSchemaAvailable() {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND LOWER(table_name) = LOWER(?)", Integer.class,
                    schemaResolver.currentSchema(), "payment_settings");
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public Optional<PaymentSettingsDto> findBankQr() {
        if (!isSchemaAvailable()) return Optional.empty();
        try {
            return jdbc.query("SELECT id, payment_method, enabled, bank_name, bank_code, bank_account, " +
                            "account_holder, default_account, payment_timeout_minutes, payment_prefix, " +
                            "payment_description_template, version, updated_at FROM payment_settings " +
                            "WHERE payment_method = 'BANK_QR'",
                    (rs, row) -> map(rs)).stream().findFirst();
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    public PaymentSettingsDto save(PaymentSettingsDto settings, String actor) {
        if (!isSchemaAvailable()) throw new IllegalStateException("Migration payment settings chưa được áp dụng");
        int updated = jdbc.update("UPDATE payment_settings SET enabled=?, bank_name=?, bank_code=?, bank_account=?, " +
                        "account_holder=?, default_account=?, payment_timeout_minutes=?, payment_prefix=?, " +
                        "payment_description_template=?, version=version+1, updated_at=CURRENT_TIMESTAMP " +
                        "WHERE payment_method='BANK_QR' AND version=?",
                settings.isEnabled(), settings.getBankName(), settings.getBankCode(), settings.getBankAccount(),
                settings.getAccountHolder(), settings.isDefaultAccount(), settings.getPaymentTimeoutMinutes(),
                settings.getPaymentPrefix(), settings.getPaymentDescriptionTemplate(), settings.getVersion());
        if (updated == 0) {
            if (settings.getId() != null)
                throw new IllegalStateException("Cấu hình đã được thay đổi bởi tài khoản khác; hãy tải lại trang");
            jdbc.update("INSERT INTO payment_settings(payment_method, enabled, bank_name, bank_code, bank_account, " +
                            "account_holder, default_account, payment_timeout_minutes, payment_prefix, " +
                            "payment_description_template, version, updated_at) VALUES('BANK_QR',?,?,?,?,?,?,?,?,?,0,CURRENT_TIMESTAMP)",
                    settings.isEnabled(), settings.getBankName(), settings.getBankCode(), settings.getBankAccount(),
                    settings.getAccountHolder(), settings.isDefaultAccount(), settings.getPaymentTimeoutMinutes(),
                    settings.getPaymentPrefix(), settings.getPaymentDescriptionTemplate());
        }
        jdbc.update("INSERT INTO payment_settings_audit(actor, action, entity_key, changed_fields, created_at) " +
                        "VALUES(?,?,?,?,CURRENT_TIMESTAMP)", actor, "UPDATE", "BANK_QR",
                "non-secret payment settings updated");
        return findBankQr().orElseThrow();
    }

    private PaymentSettingsDto map(ResultSet rs) throws java.sql.SQLException {
        PaymentSettingsDto value = new PaymentSettingsDto();
        value.setId(rs.getLong("id")); value.setPaymentMethod(rs.getString("payment_method"));
        value.setEnabled(rs.getBoolean("enabled")); value.setBankName(rs.getString("bank_name"));
        value.setBankCode(rs.getString("bank_code")); value.setBankAccount(rs.getString("bank_account"));
        value.setAccountHolder(rs.getString("account_holder")); value.setDefaultAccount(rs.getBoolean("default_account"));
        value.setPaymentTimeoutMinutes(rs.getInt("payment_timeout_minutes")); value.setPaymentPrefix(rs.getString("payment_prefix"));
        value.setPaymentDescriptionTemplate(rs.getString("payment_description_template"));
        value.setVersion(rs.getLong("version")); value.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        value.setSchemaAvailable(true); return value;
    }
}
