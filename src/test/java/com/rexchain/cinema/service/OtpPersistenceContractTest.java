package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.OtpChallenge;
import com.rexchain.cinema.repository.OtpChallengeRepository;
import com.rexchain.cinema.repository.OtpRateLimitRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OtpPersistenceContractTest {

    @Test
    void releaseSchemaCreatesBothOtpTablesWithoutDestructiveStatements() throws Exception {
        String sql = Files.readString(Path.of("database/mysql/schema.sql"), StandardCharsets.UTF_8)
                .toLowerCase(java.util.Locale.ROOT);
        assertThat(sql).contains("otp_challenges", "otp_rate_limits");
        assertThat(sql).doesNotMatch("(?s).*\\b(drop|truncate|delete)\\b.*");
    }

    @Test
    void repositoriesUseDatabaseLocksAndOtpIsNeverStoredAsPlaintextField() throws Exception {
        assertThat(OtpChallengeRepository.class.getMethod("findByEmailAndPurpose", String.class, String.class)
                .getAnnotation(org.springframework.data.jpa.repository.Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(OtpRateLimitRepository.class.getMethod("findByScopeKeyAndPurpose", String.class, String.class)
                .getAnnotation(org.springframework.data.jpa.repository.Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(Arrays.stream(OtpChallenge.class.getDeclaredFields()).map(Field::getName))
                .contains("otpHash", "otpSalt")
                .doesNotContain("otp");
    }
}
