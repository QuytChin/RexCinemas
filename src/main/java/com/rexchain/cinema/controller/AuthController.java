package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.LoginRequest;
import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.service.AuthService;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final com.rexchain.cinema.service.OtpService otpService;
    private final JwtService jwt;
    private final AccessTokenRevocationService revocations;
    private final boolean secureCookie;

    public AuthController(AuthService auth, CinemaRepository cinemas,
                          com.rexchain.cinema.service.OtpService otpService,
                          JwtService jwt, AccessTokenRevocationService revocations,
                          @Value("${app.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.cinemas = cinemas;
        this.otpService = otpService;
        this.jwt = jwt;
        this.revocations = revocations;
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
                "", "", "", "Nam", null, "", "", false));
        addCinemas(model);
        return "auth/register";
    }

    @PostMapping({"/auth/register/send-otp", "/api/auth/register/send-otp"})
    @ResponseBody
    public ResponseEntity<?> sendOtp(@Valid @RequestBody RegisterRequest form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            auth.validateRegistrationData(form);
            otpService.generateAndSendOtp(form);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mã xác thực OTP đã được gửi tới email " + form.email() + ". Vui lòng kiểm tra hộp thư."
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping({"/auth/register/verify-otp", "/api/auth/register/verify-otp"})
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody com.rexchain.cinema.dto.VerifyOtpRequest req,
                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            RegisterRequest verified = otpService.verifyOtp(req.email(), req.otp());
            auth.createUserAfterOtp(verified);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng ký tài khoản thành công!",
                    "redirectUrl", "/login?registered"
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping({"/auth/register/resend-otp", "/api/auth/register/resend-otp"})
    @ResponseBody
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không được để trống"));
        }
        try {
            otpService.resendOtp(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi lại mã OTP tới email " + email
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/auth/register")
    String registerPost(Model model) {
        model.addAttribute("error", "OTP verification is required before account creation.");
        addCinemas(model);
        return "auth/register";
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
                     HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            addCookie(response, auth.login(identifier, password, remember), remember, secureRequest(request));
            return "redirect:" + safeReturnUrl(returnUrl);
        } catch (Exception ex) {
            model.addAttribute("error", "Thông tin đăng nhập hoặc mật khẩu không đúng");
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            model.addAttribute("identifier", identifier == null ? "" : identifier.trim());
            return "auth/login";
        }
    }

    @PostMapping("/auth/logout")
    String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token == null && request.getCookies() != null) {
            token = java.util.Arrays.stream(request.getCookies())
                    .filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue).findFirst().orElse(null);
        }
        if (token != null && jwt.valid(token)) revocations.revoke(token, jwt.extractExpiration(token));
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(secureCookie || secureRequest(request)).sameSite("Lax").path("/").maxAge(0).build();
        response.addHeader("Set-Cookie", cookie.toString());
        return "redirect:/";
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    ResponseEntity<?> apiLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse response) {
        try {
            String token = auth.login(request.identifier(), request.password());
            addCookie(response, token, false, secureRequest(servletRequest));
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

    private void addCookie(HttpServletResponse response, String token, boolean remember, boolean requestIsSecure) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(secureCookie || requestIsSecure).sameSite("Lax").path("/")
                .maxAge(Duration.ofMillis(jwt.expirationMs(remember))).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private boolean secureRequest(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null || forwardedProto.isBlank()) return false;
        return "https".equalsIgnoreCase(forwardedProto.split(",", 2)[0].trim());
    }
}
