package com.rexchain.cinema.repository;

import com.rexchain.cinema.dto.PaymentTransactionDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTransactionPaginationTest {
    @Test
    void filtersAndPagesTransactionsInDatabaseWithStableOrder() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:admin_transactions_page;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE payment_transactions (id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "provider_transaction_id VARCHAR(80), booking_code VARCHAR(80), payment_reference VARCHAR(80), " +
                "gateway VARCHAR(40), transaction_date VARCHAR(80), account_number_last4 VARCHAR(4), " +
                "code VARCHAR(80), content VARCHAR(255), transfer_type VARCHAR(40), amount DECIMAL(12,2), " +
                "reference_code VARCHAR(80), status VARCHAR(40), reason VARCHAR(255), " +
                "received_at TIMESTAMP, verified_at TIMESTAMP, settled_at TIMESTAMP)");
        for (int i = 0; i < 45; i++) {
            jdbc.update("INSERT INTO payment_transactions(provider_transaction_id,booking_code,status,amount,received_at) " +
                            "VALUES(?,?,?,?,?)", "REX_TEST_TX_" + i, "REX_TEST_BOOK_" + i,
                    i < 42 ? "SETTLED" : "RECEIVED", 100_000,
                    java.sql.Timestamp.valueOf("2025-01-01 10:00:00"));
        }
        var repo = new PaymentTransactionRepository(jdbc, new DatabaseSchemaResolver(jdbc));
        var ids = new HashSet<Long>();
        for (int number = 0; number < 3; number++) {
            var page = repo.search("SETTLED", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1),
                    "REX_TEST", PageRequest.of(number, 20, Sort.by(Sort.Direction.DESC, "receivedAt")
                            .and(Sort.by(Sort.Direction.DESC, "id"))));
            assertThat(page.getTotalElements()).isEqualTo(42);
            assertThat(page.getContent()).hasSize(number == 2 ? 2 : 20);
            for (PaymentTransactionDto transaction : page) assertThat(ids.add(transaction.getId())).isTrue();
        }
        assertThat(ids).hasSize(42);
        assertThat(repo.search("SETTLED", null, null, "REX_TEST_MISSING",
                PageRequest.of(0, 20, Sort.by("id"))).getTotalElements()).isZero();
    }
}
