package com.rexchain.cinema.controller;

import com.rexchain.cinema.service.OtpRateLimitException;
import com.rexchain.cinema.security.CustomUserDetailsService;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import com.rexchain.cinema.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ExceptionMappingRegressionTest.ThrowingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ExceptionMappingRegressionTest.ThrowingController.class})
class ExceptionMappingRegressionTest {
    @Autowired MockMvc mvc;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService userDetailsService;
    @MockBean UserRepository users;
    @MockBean AccessTokenRevocationService revocations;

    @Test
    void businessErrorKeepsBadRequestContract() throws Exception {
        mvc.perform(get("/api/regression/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Dữ liệu không hợp lệ"));
    }

    @Test
    void unexpectedErrorIsNotDowngradedToBadRequestOrLeaked() throws Exception {
        mvc.perform(get("/api/regression/unexpected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Hệ thống chưa thể xử lý yêu cầu. Vui lòng thử lại sau."))
                .andExpect(content().string(not(containsString("REX_TEST_SECRET"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void rateLimitKeepsTooManyRequestsStatus() throws Exception {
        mvc.perform(get("/api/regression/rate-limit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Thử lại sau"));
    }

    @Test
    void htmlErrorViewKeepsTheActualHttpStatus() throws Exception {
        mvc.perform(get("/regression/business").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @RestController
    @RequestMapping({"/api/regression", "/regression"})
    static class ThrowingController {
        @GetMapping("/business")
        void business() { throw new IllegalArgumentException("Dữ liệu không hợp lệ"); }

        @GetMapping("/unexpected")
        void unexpected() { throw new RuntimeException("REX_TEST_SECRET should never be returned"); }

        @GetMapping("/rate-limit")
        void rateLimit() { throw new OtpRateLimitException("Thử lại sau"); }
    }
}
