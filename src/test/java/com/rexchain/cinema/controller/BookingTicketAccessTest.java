package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.BookingService;
import com.rexchain.cinema.service.BankTransferPaymentService;
import com.rexchain.cinema.service.MembershipService;
import com.rexchain.cinema.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * REX-A01 — customer tickets, QR codes and payment status stay on customer-facing
 * routes only for the owning customer. Administrators use dedicated admin routes.
 */
@ExtendWith(MockitoExtension.class)
class BookingTicketAccessTest {
    @Mock MovieRepository movies;
    @Mock ShowtimeRepository showtimes;
    @Mock ShowtimeSeatRepository showtimeSeats;
    @Mock ComboProductRepository combos;
    @Mock ReviewRepository reviews;
    @Mock UserRepository users;
    @Mock BookingRepository bookings;
    @Mock BookingService bookingService;
    @Mock MembershipService membership;
    @Mock com.rexchain.cinema.config.CinemaArtwork cinemaArtwork;
    @Mock QrCodeService qrCodes;
    @Mock BankTransferPaymentService bankTransfer;

    CinemaApiController controller;
    BookingController bookingController;

    @BeforeEach
    void setUp() {
        controller = new CinemaApiController(movies, showtimes, showtimeSeats, combos, reviews, users,
                bookings, bookingService, membership, cinemaArtwork);
        bookingController = new BookingController(showtimes, showtimeSeats, users, bookings, bookingService,
                combos, qrCodes, bankTransfer);
    }

    @Test
    void staffCannotOpenAnotherCustomersTicketPage() {
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(actor(70L, Role.STAFF)));

        assertThatThrownBy(() -> bookingController.ticket(21L, authentication("staff@example.com"),
                org.mockito.Mockito.mock(org.springframework.ui.Model.class)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void staffCannotDownloadAnotherCustomersTicketQr() {
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(actor(70L, Role.STAFF)));

        assertThatThrownBy(() -> bookingController.ticketQr(21L, authentication("staff@example.com")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> bookingController.seatTicketQr(21L, 99L, authentication("staff@example.com")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void customerCannotOpenAnotherCustomersTicketPage() {
        when(users.findByEmailIgnoreCase("intruder@example.com")).thenReturn(Optional.of(actor(9L, Role.CUSTOMER)));
        when(bookings.findById(21L)).thenReturn(Optional.of(booking(owner(7L))));

        assertThatThrownBy(() -> bookingController.ticket(21L, authentication("intruder@example.com"),
                org.mockito.Mockito.mock(org.springframework.ui.Model.class)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void ownerStillOpensOwnTicketPage() {
        User owner = owner(7L);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(bookings.findById(21L)).thenReturn(Optional.of(booking(owner)));

        String view = bookingController.ticket(21L, authentication("owner@example.com"),
                org.mockito.Mockito.mock(org.springframework.ui.Model.class));

        assertThat(view).isEqualTo("booking/ticket");
    }

    @Test
    void staffCannotReadAnotherCustomersPaymentStatus() {
        Booking booking = booking(owner(7L));
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(actor(70L, Role.STAFF)));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("staff@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(String.valueOf(response.getBody())).doesNotContain("REX1", "75000", "bookingCode", "totalAmount");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingService, never()).expireIfDue("REX1");
    }

    @Test
    void customerCannotReadAnotherCustomersPaymentStatus() {
        Booking booking = booking(owner(7L));
        when(users.findByEmailIgnoreCase("intruder@example.com"))
                .thenReturn(Optional.of(actor(9L, Role.CUSTOMER)));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("intruder@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(String.valueOf(response.getBody())).doesNotContain("REX1", "75000", "bookingCode", "totalAmount");
        verify(bookingService, never()).expireIfDue("REX1");
    }

    @Test
    void supervisorCannotReadAnotherCustomersPaymentStatus() {
        Booking booking = booking(owner(7L));
        when(users.findByEmailIgnoreCase("supervisor@example.com"))
                .thenReturn(Optional.of(actor(71L, Role.SUPERVISOR)));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("supervisor@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(bookingService, never()).expireIfDue("REX1");
    }

    @Test
    void ownerStillReadsOwnPaymentStatus() {
        User owner = owner(7L);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        Booking booking = booking(owner);
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));
        when(bookingService.expireIfDue("REX1")).thenReturn(booking);

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("owner@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingService).expireIfDue("REX1");
    }

    @Test
    void ownerCanPollConfirmedPaymentStatus() {
        User owner = owner(7L);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        Booking booking = booking(owner);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));
        when(bookingService.expireIfDue("REX1")).thenReturn(booking);

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("owner@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("status")).isEqualTo("CONFIRMED");
        verify(bookingService).expireIfDue("REX1");
    }

    @Test
    void ownerCanPollExpiredPaymentStatus() {
        User owner = owner(7L);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        Booking booking = booking(owner);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));
        when(bookingService.expireIfDue("REX1")).thenReturn(booking);

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("owner@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("status")).isEqualTo("EXPIRED");
        verify(bookingService).expireIfDue("REX1");
    }

    @Test
    void administratorUsesAdminWorkflowInsteadOfCustomerPaymentStatusEndpoint() {
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(actor(1L, Role.ADMIN)));
        Booking booking = booking(owner(7L));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("admin@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(bookingService, never()).expireIfDue("REX1");
    }

    @Test
    void cinemaManagerCannotReadAnotherCustomersPaymentStatus() {
        Booking booking = booking(owner(7L));
        when(users.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(actor(2L, Role.CINEMA_MANAGER)));
        when(bookings.findByBookingCode("REX1")).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = controller.paymentStatus("REX1", authentication("manager@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(String.valueOf(response.getBody())).doesNotContain("REX1", "75000", "bookingCode", "totalAmount");
        verify(bookingService, never()).expireIfDue("REX1");
    }

    @Test
    void unknownBookingCodeReturnsNotFoundWithoutExpiringAnything() {
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner(7L)));
        when(bookings.findByBookingCode("MISSING")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.paymentStatus("MISSING", authentication("owner@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(bookingService, never()).expireIfDue(anyString());
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "n/a", List.of());
    }

    private static User owner(Long id) {
        User user = actor(id, Role.CUSTOMER);
        user.setFullName("Khách hàng");
        user.setEmail("owner@example.com");
        return user;
    }

    private static User actor(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail("actor" + id + "@example.com");
        user.setFullName("Actor " + id);
        return user;
    }

    private static Booking booking(User owner) {
        Cinema cinema = new Cinema();
        cinema.setId(3L);
        cinema.setName("Rex Quận 1");
        Auditorium auditorium = new Auditorium();
        auditorium.setId(4L);
        auditorium.setName("Phòng 01");
        auditorium.setCinema(cinema);
        Movie movie = new Movie();
        movie.setId(5L);
        movie.setTitle("Phim thử");
        movie.setDurationMinutes(120);
        Showtime showtime = new Showtime();
        showtime.setId(11L);
        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(LocalDateTime.now().plusHours(3));
        showtime.setBasePrice(BigDecimal.valueOf(75_000));
        Booking booking = new Booking();
        booking.setId(21L);
        booking.setBookingCode("REX1");
        booking.setUser(owner);
        booking.setShowtime(showtime);
        booking.setPaymentMethod(PaymentMethod.BANK_QR);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(75_000));
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        booking.setSeats(new ArrayList<>());
        return booking;
    }
}
