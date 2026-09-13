package com.eventdriven.auth.controller;

import com.eventdriven.auth.dto.auth.LoginRequest;
import com.eventdriven.auth.dto.auth.RefreshTokenRequest;
import com.eventdriven.auth.dto.auth.RegisterRequest;
import com.eventdriven.auth.dto.auth.ResendVerificationRequest;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.service.AuthCookiesService;
import com.eventdriven.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookiesService authCookieService;

    public AuthController(
            AuthService authService,
            AuthCookiesService authCookieService
            ){
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid RegisterRequest request){
        MessageResponse response =  authService.register(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @RequestParam @Valid String token
    ){
        MessageResponse response = authService.verifyEmail(token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @RequestBody @Valid ResendVerificationRequest request
    ){
       MessageResponse response = authService.resendVerification(request);

       return ResponseEntity.status(HttpStatus.OK)
               .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(
            @RequestBody @Valid LoginRequest request
    ){

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
        ResponseCookie refreshCookie = authCookieService.createRefreshTokenCookie(request.getRefreshToken());

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




}
