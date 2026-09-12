package com.eventdriven.auth.service;

import com.eventdriven.auth.dto.auth.RegisterRequest;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.entity.*;
import com.eventdriven.auth.enums.UserStatus;
import com.eventdriven.auth.exception.EmailAlreadyRegisteredException;
import com.eventdriven.auth.exception.EmailAlreadyVerifiedException;
import com.eventdriven.auth.exception.InvalidEmailVerificationTokenException;
import com.eventdriven.auth.repository.EmailVerificationTokenRepository;
import com.eventdriven.auth.repository.RoleRepository;
import com.eventdriven.auth.repository.UserRepository;
import com.eventdriven.auth.repository.UserRoleRepository;
import com.eventdriven.auth.security.TokenHashService;
import com.eventdriven.auth.security.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final EmailService emailService;

    @Transactional
    @Override
    public MessageResponse register(RegisterRequest request){
        String email = request.getEmail().trim().toLowerCase();

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyRegisteredException();
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User();

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        user = userRepository.save(user);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER ROLE NOT FOUND"));

        UserRoleId userRoleId = new UserRoleId(
                user.getId(),
                userRole.getId()
        );

        UserRole userRoleEntity = new UserRole();

        userRoleEntity.setId(userRoleId);
        userRoleEntity.setUser(user);
        userRoleEntity.setRole(userRole);

        userRoleRepository.save(userRoleEntity);

        String rawToken = tokenService.generateToken();
        String hashToken = tokenHashService.hash(rawToken);

        EmailVerificationToken verificationToken = new EmailVerificationToken();

        verificationToken.setUser(user);
        verificationToken.setTokenHash(hashToken);
        verificationToken.setCreatedAt(now);
        verificationToken.setExpiresAt(now.plusMinutes(30));

        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(
                email,
                rawToken
        );

        return new MessageResponse("Registration successfull. Please verify your email.");

    }

    @Override
    @Transactional
    public MessageResponse verifyEmail(String rawToken){
        String tokenHash = tokenHashService.hash(rawToken);

        EmailVerificationToken verificationToken = verificationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if(verificationToken.getUsedAt() != null){
            throw new InvalidEmailVerificationTokenException();
        }
        if(verificationToken.getExpiresAt().isBefore(now)){
            throw new InvalidEmailVerificationTokenException();
        }

        User user = verificationToken.getUser();

        if(user.getEmailVerified()){
            throw new EmailAlreadyVerifiedException();
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);

        verificationToken.setUsedAt(now);

        return new MessageResponse("Email Successfully Verified");

    }




}
