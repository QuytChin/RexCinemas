package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequiredSecretsValidatorTest {
    @Test
    void reportsMissingPropertyNameWithoutExposingAValue() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "test-user");

        assertThatThrownBy(() -> new RequiredSecretsValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Required configuration is missing: spring.datasource.password. Supply the corresponding environment variable before startup.");
    }

    @Test
    void acceptsAllRequiredNonBlankProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "test-user")
                .withProperty("spring.datasource.password", "test-only-password")
                .withProperty("spring.mail.username", "test@example.invalid")
                .withProperty("spring.mail.password", "test-only-password");

        new RequiredSecretsValidator(environment).afterPropertiesSet();
    }
}
