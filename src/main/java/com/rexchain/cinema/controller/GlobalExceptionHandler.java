package com.rexchain.cinema.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String businessError(Exception exception, HttpServletRequest request, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("message", exception.getMessage() == null ? "Yêu cầu không hợp lệ" : exception.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    /** Fix 2: Translate cryptic Hibernate lock exceptions into user-friendly Vietnamese messages. */
    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String concurrencyError(Exception exception, HttpServletRequest request, Model model) {
        model.addAttribute("status", 409);
        model.addAttribute("message", "Ghế vừa được người khác chọn. Vui lòng chọn ghế khác.");
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }
}
