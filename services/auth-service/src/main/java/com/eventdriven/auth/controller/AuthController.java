package com.eventdriven.auth.controller;

import com.eventdriven.auth.dto.auth.*;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.exception.RateLimitExceededException;
import com.eventdriven.auth.service.AuthCookiesService;
import com.eventdriven.auth.service.AuthService;
import com.eventdriven.auth.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, credential login, email verification, token refresh, and password reset")
public class AuthController {

    private final AuthService authService;
    private final AuthCookiesService authCookieService;
    private final RateLimitService rateLimitService;

    public AuthController(
            AuthService authService,
            AuthCookiesService authCookieService,
            RateLimitService rateLimitService
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account with email and password and sends an email verification token.")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid RegisterRequest request) {
        MessageResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Validates the user's email verification token.")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Parameter(description = "Verification token received via email")
            @RequestParam String token
    ) {
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification", description = "Invalidates old tokens and generates a new email verification token (rate limited).")
    public ResponseEntity<MessageResponse> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String ip = httpServletRequest.getRemoteAddr();
        String key = "rate-limit:resend-verification:" + ip;

        boolean isAllowed = rateLimitService.isAllowed(key, 3, Duration.ofMinutes(15));
        if (!isAllowed) {
            throw new RateLimitExceededException();
        }

        MessageResponse response = authService.emailVerification(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and sets HttpOnly accessToken and refreshToken cookies (rate limited).")
    public ResponseEntity<MessageResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String ip = httpServletRequest.getRemoteAddr();
        String key = "rate-limit:login:" + ip;

        boolean isAllowed = rateLimitService.isAllowed(key, 5, Duration.ofMinutes(1));
        if (!isAllowed) {
            throw new RateLimitExceededException();
        }

        AuthResponse response = authService.login(request);

        ResponseCookie accessCookie = authCookieService.createAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new MessageResponse("Login Successfull"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new access token via HttpOnly cookies.")
    public ResponseEntity<MessageResponse> refresh(
            @CookieValue(name = "refreshToken") String refreshToken
    ) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        AuthResponse response = authService.refresh(request);

        ResponseCookie accessCookie = authCookieService.createAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new MessageResponse("Token Refresh Successfull"));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes the active refresh session and clears authentication cookies.")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "refreshToken") String refreshToken
    ) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        authService.logout(request);

        ResponseCookie accessCookie = authCookieService.clearAccessTokenCookie();
        ResponseCookie refreshCookie = authCookieService.clearRefreshTokenCookie();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new MessageResponse("Logout Successfull"));
    }

    @GetMapping("/test-user")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Test user access", description = "Tests access for authenticated users with role USER.")
    public ResponseEntity<String> testUser() {
        return ResponseEntity.ok("user access granted");
    }

    @GetMapping("/test-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Test admin access", description = "Tests access for authenticated users with role ADMIN.")
    public ResponseEntity<String> testAdmin() {
        return ResponseEntity.ok("admin access granted");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Generates a timed password reset token and emails it to the user (rate limited).")
    public ResponseEntity<MessageResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest httpServletRequest
    ) {
        String ip = httpServletRequest.getRemoteAddr();
        String key = "rate-limit:forgot-password:" + ip;

        boolean isAllowed = rateLimitService.isAllowed(key, 5, Duration.ofMinutes(1));
        if (!isAllowed) {
            throw new RateLimitExceededException();
        }

        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();
        MessageResponse response = authService.forgotPassword(email);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Validates the reset token, updates the password hash, and revokes all active refresh sessions.")
    public ResponseEntity<MessageResponse> resetPassword(
            @RequestBody @Valid ResetPasswordRequest resetPasswordRequest
    ) {
        MessageResponse response = authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
