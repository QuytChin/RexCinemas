package com.rexchain.cinema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BookingUiRegressionTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/booking");
    private static final Path STATIC = Path.of("src/main/resources/static");

    @Test
    void t16_1SeatMapKeepsEachRowsRealCoordinatesAndSeatTypes() throws IOException {
        String page = read(TEMPLATES.resolve("seat-map.html"));
        String css = read(STATIC.resolve("css/app.css"));

        assertThat(page).contains(
                "class=\"seat-map-rows\"",
                "class=\"seat-row\"",
                "class=\"seat-row-grid\"",
                "data-row=${row.key}",
                "grid-column:${ss.seat.seatNumber};",
                "#strings.toLowerCase(ss.seat.type.name())");
        assertThat(css).contains(
                ".seat-row-grid{display:grid;grid-template-columns:repeat(var(--seat-columns),minmax(34px,1fr))",
                ".seat.couple",
                ".seat-dot.couple");
        assertThat(page).doesNotContain("seatNumber + 1", "seatNumber+1", "dummy", "fake");
    }

    @Test
    void t16_2SeatButtonsExposeBackendPriceAndCannotSelectUnavailableSeats() throws IOException {
        String page = read(TEMPLATES.resolve("seat-map.html"));
        String script = read(STATIC.resolve("js/booking.js"));

        assertThat(page).contains("data-price=${ss.price}", "data-status=${ss.status}",
                "th:disabled=\"${ss.status.name()!='AVAILABLE'}\"");
        assertThat(script).contains("if(held)return", "Number(btn.dataset.price)",
                "b.disabled=u.status!=='AVAILABLE'");
    }

    @Test
    void t16_3ConfirmedOnlyQrAndKeepsPrintPdfActionWithoutSuccessBanner() throws IOException {
        String page = read(TEMPLATES.resolve("ticket.html"));
        String service = read(Path.of("src/main/java/com/rexchain/cinema/service/CheckInService.java"));
        String controller = read(Path.of("src/main/java/com/rexchain/cinema/controller/BookingController.java"));

        assertThat(page).contains("th:if=\"${booking.status.name()=='CONFIRMED'}\"",
                "window.print()", "QR chỉ hoạt động khi booking được xác nhận");
        assertThat(page).doesNotContain("param.success", "alert-success");
        assertThat(service).contains("booking.getStatus() != BookingStatus.CONFIRMED",
                "seat.getBooking().getStatus() != BookingStatus.CONFIRMED");
        assertThat(controller).contains("authorizedBooking(id, auth)", "service.qrPayloadFor(id)",
                "service.seatQrPayloadFor(id, seatId)");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
