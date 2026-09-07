package com.rexchain.cinema.controller;

import jakarta.servlet.http.HttpServletRequest;
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
}
