package com.eventdriven.auth.service;

import com.eventdriven.auth.config.CookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookiesService {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final CookieProperties cookieProperties;

    public AuthCookiesService(CookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    public ResponseCookie createAccessTokenCookie(String token){
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE,token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(Duration.ofMinutes(5))
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token){
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE,token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
    }

    public ResponseCookie clearAccessTokenCookie(){
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE,"")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie(){
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE,"")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
