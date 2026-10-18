package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.LoginRequest;
import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Controller
public class AuthController {
    private final AuthService auth;
    private final CinemaRepository cinemas;
    private final boolean secureCookie;

    public AuthController(AuthService auth, CinemaRepository cinemas,
                          @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.cinemas = cinemas;
        this.secureCookie = secureCookie;
    }

    @GetMapping({"/login", "/auth/login"})
    String login(@RequestParam(required = false) String returnUrl,
                 @RequestParam(required = false) String info, Model model) {
        model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
        model.addAttribute("info", info);
        return "auth/login";
    }

    @GetMapping({"/register", "/auth/register"})
    String register(Model model) {
        model.addAttribute("registerForm", new RegisterRequest(
                "", null, "", "", "", "", null, "", "", false));
        addCinemas(model);
        return "auth/register";
    }

    @PostMapping("/auth/register")
    String registerPost(@Valid @ModelAttribute("registerForm") RegisterRequest form,
                        BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            addCinemas(model);
            return "auth/register";
        }
        try {
            auth.register(form);
            return "redirect:/login?registered";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            addCinemas(model);
            return "auth/register";
        }
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    ResponseEntity<?> apiRegister(@Valid @RequestBody RegisterRequest request) {
        try {
            auth.register(request);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    String loginPost(@RequestParam String identifier, @RequestParam String password,
                     @RequestParam(required = false, defaultValue = "false") boolean remember,
                     @RequestParam(required = false) String returnUrl,
                     HttpServletResponse response, Model model) {
        try {
            addCookie(response, auth.login(identifier, password, remember), remember);
            return "redirect:" + safeReturnUrl(returnUrl);
        } catch (Exception ex) {
            model.addAttribute("error", "Thông tin đăng nhập hoặc mật khẩu không đúng");
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            model.addAttribute("identifier", identifier == null ? "" : identifier.trim());
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
    ResponseEntity<?> apiLogin(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            String token = auth.login(request.identifier(), request.password());
            addCookie(response, token, false);
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

    private void addCinemas(Model model) {
        model.addAttribute("cinemas", cinemas.findByActiveTrueOrderByCityAscNameAsc());
    }

    @GetMapping("/auth/forgot-password")
    String forgotPassword(Model model) {
        model.addAttribute("returnUrl", "/");
        model.addAttribute("info", "Khôi phục mật khẩu chưa được cấu hình qua email. Vui lòng liên hệ quản trị viên RexCinemas để được hỗ trợ.");
        return "auth/login";
    }

    private void addCookie(HttpServletResponse response, String token, boolean remember) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(remember ? Duration.ofDays(30) : Duration.ofDays(1)).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
