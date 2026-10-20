package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaMigrationContractTest {
    @Test
    void mysqlReleaseSchemaIsCompleteAndContainsNoBusinessData() throws IOException {
        String schema = read("database/mysql/schema.sql");

        assertThat(schema).contains("CHARACTER SET utf8mb4", "CREATE TABLE `bookings`", "CREATE TABLE `payment_transactions`",
                        "CREATE TABLE `otp_challenges`", "CREATE TABLE `membership_tier_policies`")
                .contains("CONSTRAINT")
                .doesNotContainPattern("(?im)^\\s*(INSERT|DROP|TRUNCATE|DELETE)\\b");
        assertThat(schema.split("CREATE TABLE `", -1)).hasSize(36);
    }

    @Test
    void initialDataContainsOnlyRequiredNeutralRewardDefaults() throws IOException {
        String initialData = read("database/mysql/initial-data.sql");

        assertThat(initialData).contains("loyalty_settings", "membership_tier_policies")
                .doesNotContain("INSERT INTO `app_users`")
                .doesNotContain("INSERT INTO `bookings`");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
