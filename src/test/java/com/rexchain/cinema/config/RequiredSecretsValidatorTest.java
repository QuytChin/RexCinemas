package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredSecretsValidatorTest {
    @Test
    void reportsMissingPropertyNameWithoutExposingAValue() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", UUID.randomUUID().toString() + UUID.randomUUID())
                .withProperty("spring.datasource.username", "test-user");

        assertThatThrownBy(() -> new RequiredSecretsValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Required configuration is missing: spring.datasource.password. Supply the corresponding environment variable before startup.");
    }

    @Test
    void acceptsAllRequiredNonBlankProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", UUID.randomUUID().toString() + UUID.randomUUID())
                .withProperty("spring.datasource.username", "test-user")
                .withProperty("spring.datasource.password", UUID.randomUUID().toString())
                .withProperty("spring.mail.username", "test@example.invalid")
                .withProperty("spring.mail.password", UUID.randomUUID().toString());

        new RequiredSecretsValidator(environment).afterPropertiesSet();
    }

    @Test
    void missingJwtSecretFailsFastWithoutEchoingASecret() {
        String isolatedValue = UUID.randomUUID().toString();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "test-user")
                .withProperty("spring.datasource.password", isolatedValue)
                .withProperty("spring.mail.username", "test@example.invalid")
                .withProperty("spring.mail.password", UUID.randomUUID().toString());

        assertThatThrownBy(() -> new RequiredSecretsValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Required configuration is missing: app.jwt.secret. Supply the corresponding environment variable before startup.")
                .hasMessageNotContaining(isolatedValue);
    }
}
