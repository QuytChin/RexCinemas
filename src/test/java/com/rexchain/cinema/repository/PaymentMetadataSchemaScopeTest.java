package com.rexchain.cinema.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMetadataSchemaScopeTest {
    @Test
    void metadataChecksIgnoreSameNamedTablesInAnotherSchema() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:payment_metadata_scope;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS OTHER_SCHEMA");
        jdbc.execute("CREATE TABLE OTHER_SCHEMA.payment_transactions (id BIGINT PRIMARY KEY)");
        jdbc.execute("SET SCHEMA PUBLIC");

        var resolver = new DatabaseSchemaResolver(jdbc);
        var transactions = new PaymentTransactionRepository(jdbc, resolver);
        var snapshots = new BankTransferSnapshotRepository(jdbc, resolver);
        var settings = new PaymentSettingsRepository(jdbc, resolver);

        assertThat(transactions.isSchemaAvailable()).isFalse();
        assertThat(snapshots.isSchemaAvailable()).isFalse();
        assertThat(settings.isSchemaAvailable()).isFalse();

        jdbc.execute("CREATE TABLE PUBLIC.payment_transactions (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE PUBLIC.bank_transfer_snapshots (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE PUBLIC.payment_settings (id BIGINT PRIMARY KEY)");

        assertThat(transactions.isSchemaAvailable()).isTrue();
        assertThat(snapshots.isSchemaAvailable()).isTrue();
        assertThat(settings.isSchemaAvailable()).isTrue();
    }
}
