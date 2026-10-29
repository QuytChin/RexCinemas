package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.BankTransferPaymentService;
import com.rexchain.cinema.service.BookingService;
import com.rexchain.cinema.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerAuthorizationTest {
    @Mock ShowtimeRepository showtimes;
    @Mock ShowtimeSeatRepository showSeats;
    @Mock UserRepository users;
    @Mock BookingRepository bookings;
    @Mock BookingService service;
    @Mock ComboProductRepository combos;
    @Mock QrCodeService qrCodes;
    @Mock BankTransferPaymentService bankTransferPaymentService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(showtimes, showSeats, users, bookings,
                service, combos, qrCodes, bankTransferPaymentService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void customerCanViewOwnTicket() throws Exception {
        User customer = user(1L, Role.CUSTOMER);
        Booking booking = booking(customer);
        when(users.findByEmailIgnoreCase("customer@example.test")).thenReturn(Optional.of(customer));
        when(bookings.findById(17L)).thenReturn(Optional.of(booking));

        mvc.perform(get("/bookings/17").principal(authentication("customer@example.test")))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotViewAnotherCustomersTicketOrQr() throws Exception {
        User customer = user(1L, Role.CUSTOMER);
        User otherCustomer = user(2L, Role.CUSTOMER);
        otherCustomer.setEmail("private-customer@example.test");
        Booking booking = mock(Booking.class);
        when(booking.getUser()).thenReturn(otherCustomer);
        when(users.findByEmailIgnoreCase("customer@example.test")).thenReturn(Optional.of(customer));
        when(bookings.findById(17L)).thenReturn(Optional.of(booking));

        mvc.perform(get("/bookings/17").principal(authentication("customer@example.test")))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-customer@example.test"))));
        mvc.perform(get("/bookings/17/qr").principal(authentication("customer@example.test")))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-customer@example.test"))));
        verifyNoInteractions(service, qrCodes);
    }

    @Test
    void nonCustomerRolesCannotUseCustomerTicketPageOrQr() throws Exception {
        for (Role role : List.of(Role.STAFF, Role.SUPERVISOR, Role.ADMIN, Role.CINEMA_MANAGER)) {
            String email = role.name().toLowerCase() + "@example.test";
            when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user(3L, role)));

            mvc.perform(get("/bookings/17").principal(authentication(email)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/bookings/17/qr").principal(authentication(email)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(bookings, service, qrCodes);
    }

    @Test
    void missingBookingReturnsNotFoundWithoutTicketData() throws Exception {
        User customer = user(1L, Role.CUSTOMER);
        when(users.findByEmailIgnoreCase("customer@example.test")).thenReturn(Optional.of(customer));
        when(bookings.findById(404L)).thenReturn(Optional.empty());

        mvc.perform(get("/bookings/404").principal(authentication("customer@example.test")))
                .andExpect(status().isNotFound());
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "", List.of());
    }

    private static User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role == Role.CUSTOMER && id == 1L ? "customer@example.test" : "user@example.test");
        user.setRole(role);
        return user;
    }

    private static Booking booking(User owner) {
        Showtime showtime = mock(Showtime.class);
        Auditorium auditorium = mock(Auditorium.class);
        Cinema cinema = mock(Cinema.class);
        when(showtime.isActive()).thenReturn(true);
        when(showtime.getAuditorium()).thenReturn(auditorium);
        when(showtime.getStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
        when(showtime.getId()).thenReturn(9L);
        when(auditorium.isActive()).thenReturn(true);
        when(auditorium.getCinema()).thenReturn(cinema);
        when(cinema.isActive()).thenReturn(true);

        Booking booking = mock(Booking.class);
        when(booking.getUser()).thenReturn(owner);
        when(booking.getStatus()).thenReturn(BookingStatus.PENDING);
        when(booking.getShowtime()).thenReturn(showtime);
        return booking;
    }
}
