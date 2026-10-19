package com.rexchain.cinema.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Prevents production from silently applying schema changes or demo data. */
@Component
public final class SchemaSafetyGuard {
    private final Environment environment;
    private final String ddlAuto;
    private final String sqlInitMode;
    private final boolean seedEnabled;

    public SchemaSafetyGuard(Environment environment,
                              @Value("${spring.jpa.hibernate.ddl-auto:validate}") String ddlAuto,
                              @Value("${spring.sql.init.mode:never}") String sqlInitMode,
                              @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        this.environment = environment;
        this.ddlAuto = ddlAuto;
        this.sqlInitMode = sqlInitMode;
        this.seedEnabled = seedEnabled;
    }

    @PostConstruct
    public void validate() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) return;
        if (!"validate".equalsIgnoreCase(ddlAuto)) {
            throw new IllegalStateException("Production requires spring.jpa.hibernate.ddl-auto=validate");
        }
        if (!"never".equalsIgnoreCase(sqlInitMode)) {
            throw new IllegalStateException("Production requires spring.sql.init.mode=never");
        }
        if (seedEnabled) {
            throw new IllegalStateException("Production cannot enable app.seed.enabled");
        }
    }
}
