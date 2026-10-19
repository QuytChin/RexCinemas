package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationContractTest {
    @Test
    void timeoutMigrationsAreAdditiveAndNotAutoRegistered() throws IOException {
        String mysql = read("src/main/resources/db/mysql-v10-timeout-late-payment.sql");
        String postgres = read("src/main/resources/db/postgres-v10-timeout-late-payment.sql");
        String sqlServer = read("src/main/resources/db/migration/sqlserver/V16__timeout_late_payment.sql");

        assertThat(mysql).contains("CREATE TABLE IF NOT EXISTS").doesNotContainPattern("(?i)\\b(drop|truncate|delete)\\b");
        assertThat(postgres).contains("CREATE TABLE IF NOT EXISTS").doesNotContainPattern("(?i)\\b(drop|truncate|delete)\\b");
        assertThat(sqlServer).contains("IF OBJECT_ID").doesNotContainPattern("(?i)\\b(drop|truncate|delete)\\b");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
