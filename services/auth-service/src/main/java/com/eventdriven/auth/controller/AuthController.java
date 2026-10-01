package com.eventdriven.auth.controller;

import com.eventdriven.auth.dto.auth.*;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.exception.RateLimitExceededException;
import com.eventdriven.auth.service.AuthCookiesService;
import com.eventdriven.auth.service.AuthService;
import com.eventdriven.auth.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookiesService authCookieService;
    private final RateLimitService rateLimitService;

    public AuthController(
            AuthService authService,
            AuthCookiesService authCookieService,
            RateLimitService rateLimitService
            ){
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid RegisterRequest request){
        MessageResponse response =  authService.register(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @RequestParam  String token
    ){
        MessageResponse response = authService.verifyEmail(token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request,
            HttpServletRequest httpServletRequest
    ){
        String ip = httpServletRequest.getRemoteAddr();

        String key = "rate-limit:resend-verification:" + ip;

        boolean isAllowed =  rateLimitService.isAllowed(key,3,Duration.ofMinutes(15));

        if(!isAllowed){
            throw new RateLimitExceededException();
        }

       MessageResponse response = authService.emailVerification(request);

       return ResponseEntity.status(HttpStatus.OK)
               .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest
    ){
        String ip = httpServletRequest.getRemoteAddr();

        String key = "rate-limit:login:" + ip;

        boolean isAllowed = rateLimitService.isAllowed(key,5, Duration.ofMinutes(1));

        if(!isAllowed){
            throw new RateLimitExceededException();
        }

        AuthResponse response = authService.login(request);

        ResponseCookie accessCookie = authCookieService
                .createAccessTokenCookie(response.getAccessToken());

        ResponseCookie refreshCookie = authCookieService
                .createRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE,accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(new MessageResponse("Login Successfull"));

    }

    @PostMapping("/refresh")
    public ResponseEntity<MessageResponse> refresh(
            @CookieValue(name = "refreshToken") String refreshToken
    ){
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        AuthResponse response = authService.refresh(request);

        ResponseCookie accessCookie = authCookieService.createAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = authCookieService.createRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE,accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(new MessageResponse("Token Refresh Successfull"));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "refreshToken") String refreshToken
    ){
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        authService.logout(request);

        ResponseCookie accessCookie = authCookieService.clearAccessTokenCookie();
        ResponseCookie refreshCookie = authCookieService.clearRefreshTokenCookie();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(new MessageResponse("Logout Successfull"));

    }


    @GetMapping("/test-user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testUser(){
            return ResponseEntity.ok("user access granted");
    }

    @GetMapping("/test-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testAdmin(){
        return ResponseEntity.ok("admin access granted");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest httpServletRequest
    )
    {
        String ip = httpServletRequest.getRemoteAddr();
        String key = "rate-limit:forgot-password:" + ip;

        boolean isAllowed = rateLimitService
                                .isAllowed(key,5, Duration.ofMinutes(1));
        if(!isAllowed){
            throw new RateLimitExceededException();
        }

        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();

        MessageResponse response = authService.forgotPassword(email);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }


    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @RequestBody @Valid ResetPasswordRequest resetPasswordRequest
    )
    {
        MessageResponse response = authService.resetPassword(resetPasswordRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}
