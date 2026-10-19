package com.rexchain.cinema.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeConfigurationContractTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("(?i)^(your[-_].*|replace[-_].*|<[^>]+>|)$");

    @Test
    void requiredRuntimeCredentialsComeFromEnvironmentWithoutFallbacks() throws IOException {
        Properties base = properties("application.properties");
        Properties mysql = properties("application-mysql.properties");

        assertThat(base.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET:}");
        assertEnvironmentOnly(base, "spring.mail.username", "MAIL_USERNAME", true);
        assertEnvironmentOnly(base, "spring.mail.password", "MAIL_PASSWORD", true);
        assertEnvironmentOnly(mysql, "spring.datasource.username", "DB_USERNAME", true);
        assertEnvironmentOnly(mysql, "spring.datasource.password", "DB_PASSWORD", true);
        assertBlankEnvironmentPlaceholder(base, "app.seed.admin-password");
        assertBlankEnvironmentPlaceholder(base, "app.seed.demo-customer-password");
        assertBlankEnvironmentPlaceholder(base, "app.seed.demo-manager-password");
    }

    @Test
    void environmentSampleContainsOnlyPlaceholdersForCredentialFields() throws IOException {
        List<String> lines = Files.readAllLines(Path.of(".env.example"));
        for (String key : List.of("DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "MAIL_USERNAME", "MAIL_PASSWORD",
                "CLOUDINARY_API_KEY", "CLOUDINARY_API_SECRET", "SEPAY_API_KEY", "SEPAY_WEBHOOK_SECRET")) {
            String value = lines.stream()
                    .filter(line -> line.startsWith(key + "="))
                    .map(line -> line.substring(key.length() + 1).trim())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing placeholder key: " + key));
            assertThat(PLACEHOLDER.matcher(value).matches())
                    .as("Credential sample %s must remain a placeholder", key)
                    .isTrue();
        }
    }

    @Test
    void devAndProdProfilesKeepTheirSafetyContracts() throws IOException {
        Properties dev = properties("application-dev.properties");
        Properties prod = properties("application-prod.properties");

        assertThat(dev.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("${HIBERNATE_DDL_AUTO:update}");
        assertThat(dev.getProperty("app.cookie.secure")).isEqualTo("${COOKIE_SECURE:false}");
        assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(prod.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(prod.getProperty("app.cookie.secure")).isEqualTo("true");
    }

    private Properties properties(String name) throws IOException {
        Properties properties = new Properties();
        Path resource = Path.of("src/main/resources", name);
        try (var stream = Files.newInputStream(resource)) {
            properties.load(stream);
        }
        return properties;
    }

    private void assertEnvironmentOnly(Properties properties, String property, String variable, boolean blankDefault) {
        assertThat(properties.getProperty(property))
                .isEqualTo("${" + variable + (blankDefault ? ":}" : "}"));
    }

    private void assertBlankEnvironmentPlaceholder(Properties properties, String property) {
        assertThat(properties.getProperty(property)).matches("\\$\\{[A-Z0-9_]+:}");
    }
}
