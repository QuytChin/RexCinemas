package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.LoginRequest;
import com.rexchain.cinema.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Controller
public class AuthController {
    private final AuthService auth;
    private final boolean secureCookie;

    public AuthController(AuthService auth, @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/auth/login")
    String login(@RequestParam(required = false) String returnUrl, Model model) {
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        return "auth/login";
    }

    @GetMapping("/auth/register")
    String register() { return "auth/register"; }

    @PostMapping("/auth/register")
    String registerPost(@RequestParam String fullName, @RequestParam String email,
                        @RequestParam(required = false) String phone, @RequestParam String password, Model model) {
        try {
            auth.register(fullName, email, phone, password);
            return "redirect:/auth/login?registered";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/auth/login")
    String loginPost(@RequestParam String email, @RequestParam String password,
                     @RequestParam(required = false) String returnUrl,
                     HttpServletResponse response, Model model) {
        try {
            addCookie(response, auth.login(email, password));
            return "redirect:" + safeReturnUrl(returnUrl);
        } catch (Exception ex) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng");
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            return "auth/login";
        }
    }

    @PostMapping("/auth/logout")
    String logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(0).build();
        response.addHeader("Set-Cookie", cookie.toString());
        return "redirect:/";
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    ResponseEntity<?> apiLogin(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            String token = auth.login(request.email(), request.password());
            addCookie(response, token);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Sai thông tin đăng nhập"));
        }
    }

    private String safeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) return "/";
        String value = returnUrl.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("\r") || value.contains("\n"))
            return "/";
        return value;
    }

    private void addCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(Duration.ofDays(1)).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
