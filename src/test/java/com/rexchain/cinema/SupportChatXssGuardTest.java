package com.rexchain.cinema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REX-A03: support chat content is untrusted and must never be injected as HTML.
 * The browser level proof lives in tests/support-chat-xss.spec.js; this guard keeps
 * the Java build honest when the chat scripts or their templates are edited.
 */
class SupportChatXssGuardTest {
    private static final Path STATIC_JS = Path.of("src/main/resources/static/js");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void chatScriptsNeverInjectMessageContentAsHtml() throws IOException {
        String admin = read(STATIC_JS.resolve("admin-support.js"));
        String widget = read(STATIC_JS.resolve("chat-widget.js"));

        assertThat(admin)
                .doesNotContain("bubbleEl.innerHTML")
                .doesNotContain("item.innerHTML")
                .doesNotContain("formatMarkdown");
        assertThat(widget)
                .doesNotContain("bubbleEl.innerHTML")
                .doesNotContain("formatMarkdown");

        assertThat(admin).contains("window.RexChatRender").contains("createConversationItem");
        assertThat(widget).contains("window.RexChatRender");
    }

    @Test
    void sharedRendererOnlyAllowsHttpAndHttpsTargets() throws IOException {
        String renderer = read(STATIC_JS.resolve("chat-render.js"));

        assertThat(renderer)
                .contains("ALLOWED_PROTOCOLS = ['http:', 'https:']")
                .contains("textContent")
                .doesNotContain("innerHTML")
                .doesNotContain("insertAdjacentHTML")
                .doesNotContain("outerHTML");
    }

    @Test
    void chatPagesLoadTheSharedRendererBeforeTheirScripts() throws IOException {
        String adminPage = read(TEMPLATES.resolve("admin/support.html"));
        String widgetFragment = read(TEMPLATES.resolve("fragments/chat-widget.html"));

        assertThat(adminPage.indexOf("/js/chat-render.js"))
                .as("admin support page loads chat-render.js before admin-support.js")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(adminPage.indexOf("/js/admin-support.js"));
        assertThat(widgetFragment.indexOf("/js/chat-render.js"))
                .as("chat widget loads chat-render.js before chat-widget.js")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(widgetFragment.indexOf("/js/chat-widget.js"));
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}