package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaSafetyGuardTest {
    @Test
    void productionAcceptsValidateAndNeverWithoutSeed() {
        SchemaSafetyGuard guard = guard("prod", "validate", "never", false);

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void productionRejectsDdlUpdate() {
        SchemaSafetyGuard guard = guard("prod", "update", "never", false);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ddl-auto=validate");
    }

    @Test
    void productionRejectsAlwaysInitAndSeed() {
        SchemaSafetyGuard initGuard = guard("prod", "validate", "always", false);
        SchemaSafetyGuard seedGuard = guard("prod", "validate", "never", true);

        assertThatThrownBy(initGuard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sql.init.mode=never");
        assertThatThrownBy(seedGuard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.seed.enabled");
    }

    @Test
    void developmentCanUseUpdateWithoutEnablingSeed() {
        SchemaSafetyGuard guard = guard("dev", "update", "never", false);

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    private SchemaSafetyGuard guard(String profile, String ddlAuto, String sqlInitMode, boolean seedEnabled) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new SchemaSafetyGuard(environment, ddlAuto, sqlInitMode, seedEnabled);
    }
}
