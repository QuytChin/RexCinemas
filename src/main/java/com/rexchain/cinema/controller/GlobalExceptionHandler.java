package com.rexchain.cinema.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.contains("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Xử lý lỗi validation DTO (@Valid, @RequestBody, @ModelAttribute)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValidationExceptions(Exception exception, HttpServletRequest request, Model model) {
        List<String> errors;
        if (exception instanceof MethodArgumentNotValidException manv) {
            errors = manv.getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
        } else if (exception instanceof BindException be) {
            errors = be.getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
        } else {
            errors = List.of(exception.getMessage());
        }

        String firstMessage = errors.isEmpty() ? "Dữ liệu không hợp lệ" : errors.get(0);

        if (isApiRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.BAD_REQUEST.value());
            body.put("error", firstMessage);
            body.put("errors", errors);
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        model.addAttribute("status", 400);
        model.addAttribute("message", firstMessage);
        model.addAttribute("errors", errors);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    /**
     * Dịch lỗi Hibernate Concurrency / Optimistic / Pessimistic Locking thành thông báo thân thiện
     */
    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public Object concurrencyError(Exception exception, HttpServletRequest request, Model model) {
        String friendlyMessage = "Ghế vừa được người khác chọn. Vui lòng chọn ghế khác.";
        if (isApiRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.CONFLICT.value());
            body.put("error", friendlyMessage);
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        model.addAttribute("status", 409);
        model.addAttribute("message", friendlyMessage);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    /**
     * Xử lý lỗi nghiệp vụ IllegalArgumentException, IllegalStateException
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Object businessError(Exception exception, HttpServletRequest request, Model model) {
        String msg = exception.getMessage() == null ? "Yêu cầu không hợp lệ" : exception.getMessage();
        if (isApiRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.BAD_REQUEST.value());
            body.put("error", msg);
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        model.addAttribute("status", 400);
        model.addAttribute("message", msg);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    /**
     * Xử lý lỗi không tìm thấy tài nguyên (NoSuchElementException)
     */
    @ExceptionHandler(NoSuchElementException.class)
    public Object notFoundError(Exception exception, HttpServletRequest request, Model model) {
        String msg = "Không tìm thấy dữ liệu yêu cầu";
        if (isApiRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", msg);
            body.put("path", request.getRequestURI());
            body.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        model.addAttribute("status", 404);
        model.addAttribute("message", msg);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object unexpectedError(Exception exception, HttpServletRequest request, Model model) {
        log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), exception);
        String message = "Hệ thống chưa thể xử lý yêu cầu. Vui lòng thử lại sau.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500, "error", message, "path", request.getRequestURI(),
                    "timestamp", LocalDateTime.now()));
        }
        model.addAttribute("status", 500);
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
        return "error";
    }
}
