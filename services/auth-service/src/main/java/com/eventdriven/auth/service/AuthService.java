package com.eventdriven.auth.service;

import com.eventdriven.auth.dto.auth.RegisterRequest;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.dto.verification.VerifyEmailRequest;

public interface AuthService {

    public MessageResponse register(RegisterRequest request);

    public MessageResponse verifyEmail(String rawToken);

}
