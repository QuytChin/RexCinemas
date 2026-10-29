package com.rexchain.cinema.controller;

import com.rexchain.cinema.dto.LoginRequest;
import com.rexchain.cinema.dto.RegisterRequest;
import com.rexchain.cinema.dto.PasswordResetCompleteRequest;
import com.rexchain.cinema.dto.PasswordResetRequest;
import com.rexchain.cinema.dto.PasswordResetVerifyRequest;
import com.rexchain.cinema.repository.CinemaRepository;
import com.rexchain.cinema.service.AuthService;
import com.rexchain.cinema.service.OtpRateLimitException;
import com.rexchain.cinema.security.JwtService;
import com.rexchain.cinema.security.LoginClientIpResolver;
import com.rexchain.cinema.service.AccessTokenRevocationService;
import com.rexchain.cinema.service.LoginRateLimitException;
import com.rexchain.cinema.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final PasswordResetService passwordReset;
    private final boolean secureCookie;
    private final String trustedLoginProxyIps;

    public AuthController(AuthService auth, CinemaRepository cinemas,
                          com.rexchain.cinema.service.OtpService otpService,
                          JwtService jwt, AccessTokenRevocationService revocations,
                          PasswordResetService passwordReset,
                          @Value("${app.cookie.secure:false}") boolean secureCookie,
                          @Value("${app.auth.login.trusted-proxy-ips:}") String trustedLoginProxyIps) {
        this.auth = auth;
        this.cinemas = cinemas;
        this.otpService = otpService;
        this.jwt = jwt;
        this.revocations = revocations;
        this.passwordReset = passwordReset;
        this.secureCookie = secureCookie;
        this.trustedLoginProxyIps = trustedLoginProxyIps;
    }

    @GetMapping({"/login", "/auth/login"})
    String login(@RequestParam(required = false) String returnUrl,
                 @RequestParam(required = false) String info, Model model,
                 Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + (returnUrl == null || returnUrl.isBlank()
                    ? "/profile" : safeReturnUrl(returnUrl));
        }
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
    public ResponseEntity<?> sendOtp(@Valid @RequestBody RegisterRequest form, BindingResult bindingResult,
                                     HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            auth.validateRegistrationData(form);
            otpService.generateAndSendOtp(form, clientIp(request));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mã xác thực OTP đã được gửi tới email " + form.email() + ". Vui lòng kiểm tra hộp thư."
            ));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return errorResponse(ex, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.");
        }
    }

    @PostMapping({"/auth/register/verify-otp", "/api/auth/register/verify-otp"})
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody com.rexchain.cinema.dto.VerifyOtpRequest req,
                                       BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            com.rexchain.cinema.dto.VerifiedRegistration verified = otpService.verifyOtp(req.email(), req.otp(), clientIp(request));
            auth.createUserAfterOtp(verified);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng ký tài khoản thành công!",
                    "redirectUrl", "/login?registered"
            ));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return errorResponse(ex, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.");
        }
    }

    @PostMapping({"/auth/register/resend-otp", "/api/auth/register/resend-otp"})
    @ResponseBody
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không được để trống"));
        }
        try {
            otpService.resendOtp(email, clientIp(request));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi lại mã OTP tới email " + email
            ));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return errorResponse(ex, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.");
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
            return errorResponse(ex, "Không thể đăng ký tài khoản lúc này. Vui lòng thử lại sau.");
        }
    }

    @PostMapping("/auth/login")
    String loginPost(@RequestParam String identifier, @RequestParam String password,
                     @RequestParam(required = false, defaultValue = "false") boolean remember,
                     @RequestParam(required = false) String returnUrl,
                     HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            addCookie(response, auth.login(identifier, password, remember,
                    LoginClientIpResolver.resolve(request, trustedLoginProxyIps)), remember, secureRequest(request));
            return "redirect:" + safeReturnUrl(returnUrl);
        } catch (LoginRateLimitException ex) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(ex.getRetryAfterSeconds()));
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("returnUrl", safeReturnUrl(returnUrl));
            model.addAttribute("identifier", identifier == null ? "" : identifier.trim());
            return "auth/login";
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
            String token = auth.login(request.identifier(), request.password(), false,
                    LoginClientIpResolver.resolve(servletRequest, trustedLoginProxyIps));
            addCookie(response, token, false, secureRequest(servletRequest));
            return ResponseEntity.ok(Map.of("token", token));
        } catch (LoginRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
                    .body(Map.of("error", ex.getMessage()));
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
        return "auth/forgot-password";
    }

    @PostMapping({"/auth/forgot-password/send-otp", "/api/auth/forgot-password/send-otp"})
    @ResponseBody
    public ResponseEntity<?> sendPasswordResetOtp(@Valid @RequestBody PasswordResetRequest request,
                                                    BindingResult bindingResult, HttpServletRequest servletRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            passwordReset.requestReset(request.email(), clientIp(servletRequest));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Nếu email hợp lệ và đã đăng ký, mã OTP sẽ được gửi tới hộp thư của bạn."
            ));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau."));
        }
    }

    @PostMapping({"/auth/forgot-password/verify-otp", "/api/auth/forgot-password/verify-otp"})
    @ResponseBody
    public ResponseEntity<?> verifyPasswordResetOtp(@Valid @RequestBody PasswordResetVerifyRequest request,
                                                      BindingResult bindingResult, HttpServletRequest servletRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            String resetToken = passwordReset.verifyOtp(request.email(), request.otp(), clientIp(servletRequest));
            return ResponseEntity.ok(Map.of("success", true, "resetToken", resetToken));
        } catch (OtpRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return errorResponse(ex, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.");
        }
    }

    @PostMapping({"/auth/forgot-password/reset", "/api/auth/forgot-password/reset"})
    @ResponseBody
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetCompleteRequest request,
                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            passwordReset.resetPassword(request.email(), request.resetToken(), request.newPassword(), request.confirmPassword());
            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", "/login?reset=success"));
        } catch (Exception ex) {
            return errorResponse(ex, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.");
        }
    }

    private void addCookie(HttpServletResponse response, String token, boolean remember, boolean requestIsSecure) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true).secure(secureCookie || requestIsSecure).sameSite("Lax").path("/")
                .maxAge(Duration.ofMillis(jwt.expirationMs(remember))).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private ResponseEntity<Map<String, String>> errorResponse(Exception exception, String fallback) {
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage() == null ? fallback : exception.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", fallback));
    }

    private boolean secureRequest(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto == null || forwardedProto.isBlank()) return false;
        return "https".equalsIgnoreCase(forwardedProto.split(",", 2)[0].trim());
    }

    private String clientIp(HttpServletRequest request) {
        return request == null || request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
