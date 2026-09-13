package com.eventdriven.auth.service;

import com.eventdriven.auth.dto.auth.LoginRequest;
import com.eventdriven.auth.dto.auth.RefreshTokenRequest;
import com.eventdriven.auth.dto.auth.RegisterRequest;
import com.eventdriven.auth.dto.auth.ResendVerificationRequest;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.dto.verification.VerifyEmailRequest;

public interface AuthService {

    MessageResponse register(RegisterRequest request);

    MessageResponse verifyEmail(String rawToken);

    MessageResponse resendVerification(ResendVerificationRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

}
