package com.eventdriven.auth.service;

import com.eventdriven.auth.dto.auth.*;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;

public interface AuthService {

    MessageResponse register(RegisterRequest request);

    MessageResponse verifyEmail(String rawToken);

    MessageResponse emailVerification(ResendVerificationRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    MessageResponse forgotPassword(String email);

    MessageResponse resetPassword(ResetPasswordRequest request);

}
