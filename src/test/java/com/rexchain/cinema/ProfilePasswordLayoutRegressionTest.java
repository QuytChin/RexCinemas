package com.rexchain.cinema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePasswordLayoutRegressionTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/profile.html");
    private static final Path STYLES = Path.of("src/main/resources/static/css/app.css");

    @Test
    void passwordFieldsUseOneSharedGridStructureAndStableHelperRows() throws Exception {
        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String css = Files.readString(STYLES, StandardCharsets.UTF_8);

        assertThat(count(html, "class=\"profile-password-field")).isEqualTo(3);
        assertThat(html).contains(
                "profile-password-field-current",
                "id=\"currentPassword\"",
                "id=\"newPassword\"",
                "id=\"confirmPassword\"",
                "class=\"profile-password-help\"",
                "th:action=\"@{/profile/password}\"");
        assertThat(css).contains(
                ".profile-password-layout{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))",
                ".profile-password-field-current{grid-column:1/-1}",
                ".profile-password-field{display:grid;grid-template-rows:auto auto minmax(1.05rem,auto)",
                ".profile-password-layout .form-control{display:block;width:100%;min-width:0;box-sizing:border-box}",
                "@media(max-width:767px){.profile-password-layout{grid-template-columns:1fr}");
        assertThat(css).doesNotContain(".profile-password-layout>label:first-child");
    }

    private long count(String value, String needle) {
        return Pattern.compile(Pattern.quote(needle)).matcher(value).results().count();
    }
}
