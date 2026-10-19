package com.rexchain.cinema.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fails startup with property names only; never includes secret values in diagnostics. */
@Component
public class RequiredSecretsValidator implements InitializingBean {
    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "app.jwt.secret",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.mail.username",
            "spring.mail.password");

    private final Environment environment;

    public RequiredSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        for (String property : REQUIRED_PROPERTIES) {
            String value = environment.getProperty(property);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Required configuration is missing: " + property
                        + ". Supply the corresponding environment variable before startup.");
            }
        }
    }
}
