package com.rexchain.cinema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UiDesignSystemRegressionTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path APP_CSS = Path.of("src/main/resources/static/css/app.css");
    private static final Path ICON_SPRITE = Path.of("src/main/resources/static/images/icons.svg");

    @Test
    void everyPageDeclaresResponsiveViewportAndLoadsAppCssAfterBootstrap() throws IOException {
        try (Stream<Path> paths = Files.walk(TEMPLATES)) {
            List<Path> pages = paths
                    .filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> !path.toString().contains("fragments"))
                    .toList();

            assertThat(pages).isNotEmpty();
            for (Path page : pages) {
                String html = Files.readString(page, StandardCharsets.UTF_8);
                assertThat(html)
                        .as("responsive viewport in %s", page)
                        .contains("name=\"viewport\"");

                int bootstrap = html.indexOf("bootstrap@5.3.3/dist/css/bootstrap.min.css");
                int appCss = html.indexOf("/css/app.css");
                assertThat(bootstrap)
                        .as("Bootstrap stylesheet in %s", page)
                        .isGreaterThanOrEqualTo(0);
                assertThat(appCss)
                        .as("app.css loaded after Bootstrap in %s", page)
                        .isGreaterThan(bootstrap);
            }
        }
    }

    @Test
    void primaryCreateAndSaveActionsNeverUseDangerStyling() throws IOException {
        try (Stream<Path> paths = Files.walk(TEMPLATES.resolve("admin"))) {
            for (Path page : paths.filter(path -> path.toString().endsWith(".html")).toList()) {
                String html = Files.readString(page, StandardCharsets.UTF_8);
                assertThat(html)
                        .as("semantic danger buttons in %s", page)
                        .doesNotContainPattern("(?is)class=\"[^\"]*btn-danger[^\"]*\"[^>]*>[^<]*(Lưu|Tạo|Thêm)");
            }
        }
    }

    @Test
    void sharedStylesExposeTheRequiredDesignTokens() throws IOException {
        String css = Files.readString(APP_CSS, StandardCharsets.UTF_8);
        assertThat(css).contains(
                "--color-bg:",
                "--color-surface:",
                "--color-primary:",
                "--color-text:",
                "--color-text-muted:",
                "--color-border:",
                "--color-success:",
                "--color-danger:",
                "--color-warning:",
                "--radius-sm:",
                "--radius-md:",
                "--radius-lg:",
                "--focus-ring:");
    }

    @Test
    void cinemaManagementUsesSharedComponentsAndLocalSvgIcons() throws IOException {
        String cinemaPage = Files.readString(TEMPLATES.resolve("admin/cinemas.html"), StandardCharsets.UTF_8);
        String sidebar = Files.readString(TEMPLATES.resolve("fragments/admin-nav.html"), StandardCharsets.UTF_8);
        String css = Files.readString(APP_CSS, StandardCharsets.UTF_8);
        String icons = Files.readString(ICON_SPRITE, StandardCharsets.UTF_8);

        assertThat(cinemaPage).contains("ui-page-header", "ui-card", "images/icons.svg#plus", "images/icons.svg#map-pin");
        assertThat(sidebar).contains("images/icons.svg#chart-column", "images/icons.svg#building-2");
        assertThat(css).contains("UI component system v2", ".ui-btn", ".ui-card", ".ui-page-header", ".ui-icon");
        assertThat(icons).contains("id=\"plus\"", "id=\"pencil\"", "id=\"trash-2\"", "id=\"eye\"");
    }

    @Test
    void paymentSettingsUseSafeStatusOnlyDesignAndSharedSvgControls() throws IOException {
        String page = Files.readString(TEMPLATES.resolve("admin/payment-settings.html"), StandardCharsets.UTF_8);
        String sidebar = Files.readString(TEMPLATES.resolve("fragments/admin-nav.html"), StandardCharsets.UTF_8);
        String css = Files.readString(APP_CSS, StandardCharsets.UTF_8);
        String icons = Files.readString(ICON_SPRITE, StandardCharsets.UTF_8);

        assertThat(page).contains("payment-settings-page", "bankSelect", "payment-admin-grid", "Xem trước mã thanh toán");
        assertThat(page).contains("/admin/payments/save", "paymentTimeoutMinutes", "paymentDescriptionTemplate");
        assertThat(page).doesNotContain("SEPAY_API_KEY", "SEPAY_BANK_ACCOUNT", "name=\"apiKey\"", "name=\"webhookSecret\"");
        assertThat(sidebar).contains("data-admin-route=\"/admin/payments\"", "icons.svg#credit-card");
        assertThat(css).contains(".payment-admin-grid", ".payment-status-item", ".rex-carousel-control");
        assertThat(icons).contains("id=\"chevron-left\"", "id=\"chevron-right\"", "id=\"credit-card\"");
        assertThat(Files.readString(Path.of("database/mysql/schema.sql"), StandardCharsets.UTF_8))
                .contains("payment_settings", "payment_settings_audit", "CREATE TABLE")
                .doesNotContain("SEPAY_API_KEY");
    }

    @Test
    void homePageAndSharedControlsMeetBasicAccessibilityContracts() throws IOException {
        String home = Files.readString(TEMPLATES.resolve("index.html"), StandardCharsets.UTF_8);
        String footer = Files.readString(TEMPLATES.resolve("fragments/footer.html"), StandardCharsets.UTF_8);
        String chat = Files.readString(TEMPLATES.resolve("fragments/chat-widget.html"), StandardCharsets.UTF_8);

        assertThat(home).contains("name=\"description\"", "<label for=\"quickCinemaSelect\">",
                "id=\"quickCinemaSelect\"");
        assertThat(footer).contains("<h2>").doesNotContain("<h6>");
        assertThat(chat).contains("<button id=\"rexChatOpenBtn\"")
                .contains("aria-label=\"Open Rex support chat\"")
                .doesNotContain("id=\"rexChatLauncher\" class=\"rex-chat-launcher\" aria-label=");
    }

    @Test
    void bankQrCountdownUsesServerDeadlineAndRemovesPaymentDataAfterExpiry() throws IOException {
        String page = Files.readString(TEMPLATES.resolve("booking/bank-qr-waiting.html"), StandardCharsets.UTF_8);
        String script = Files.readString(Path.of("src/main/resources/static/js/bank-qr-payment.js"), StandardCharsets.UTF_8);

        assertThat(page).contains("data-expires-at-epoch", "data-server-now-epoch", "data-seat-map-url")
                .doesNotContain(">05:00<");
        assertThat(script).contains("estimatedServerNow", "expiresAtEpochMillis", "paymentAllowed",
                "hidePaymentInstructions", "A network error must never declare a booking expired");
    }
}
