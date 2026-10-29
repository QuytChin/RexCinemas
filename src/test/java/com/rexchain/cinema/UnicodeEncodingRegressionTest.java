package com.rexchain.cinema;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeEncodingRegressionTest {

    @Test
    void keepsVietnameseLabelsInUtf8() throws IOException {
        assertContains("templates/admin/movies.html", "Quản lý phim");
        assertContains("templates/booking/seat-map.html", "Đặt vé");
        assertContains("templates/admin/users.html", "Người dùng");
    }

    @Test
    void doesNotContainReplacementOrCommonMojibakeInKeyPages() throws IOException {
        for (String resource : new String[]{
                "templates/admin/movies.html",
                "templates/booking/seat-map.html",
                "templates/admin/users.html",
                "templates/fragments/admin-nav.html"
        }) {
            String content = read(resource);
            assertFalse(content.contains("�"), resource);
            assertFalse(content.contains("Ã"), resource);
            assertFalse(content.contains("Â©") || content.contains("Â£"), resource);
        }
    }

    private static void assertContains(String resource, String expected) throws IOException {
        assertTrue(read(resource).contains(expected), resource + " should contain " + expected);
    }

    private static String read(String resource) throws IOException {
        return new String(new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
