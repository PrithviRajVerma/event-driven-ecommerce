package com.eventdriven.auth.service;

import com.eventdriven.auth.dto.auth.*;
import com.eventdriven.auth.dto.response.AuthResponse;
import com.eventdriven.auth.dto.response.MessageResponse;
import com.eventdriven.auth.entity.*;
import com.eventdriven.auth.enums.UserStatus;
import com.eventdriven.auth.exception.*;
import com.eventdriven.auth.repository.*;
import com.eventdriven.auth.security.JwtTokenService;
import com.eventdriven.auth.security.TokenHashService;
import com.eventdriven.auth.security.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final EmailService emailService;
    private final JwtTokenService jwtTokenService;

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

        emailVerificationTokenRepository.save(verificationToken);

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

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
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

    @Override
    @Transactional
    public MessageResponse emailVerification(ResendVerificationRequest request){

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if(user == null){
            return new MessageResponse("If email exists, verification email has been sent.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);


        List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository
                .findByUserIdAndUsedAtIsNull(user.getId());

        for(EmailVerificationToken token: existingTokens){
            token.setUsedAt(now);
        }

        String rawToken = tokenService.generateToken();

        String tokenHash = tokenHashService.hash(rawToken);

        EmailVerificationToken verificationToken = new EmailVerificationToken();

        verificationToken.setUser(user);
        verificationToken.setTokenHash(tokenHash);
        verificationToken.setExpiresAt(now.plusMinutes(30));
        verificationToken.setCreatedAt(now);

        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(),rawToken);

        return new MessageResponse("If account exists , a verification email has been sent.");

    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request){

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }

        if(!Boolean.TRUE.equals(user.getEmailVerified())){
            throw new EmailNotVerifiedException();
        }

        if(user.getStatus() != UserStatus.ACTIVE){
            throw new AccountDisabledException();
        }

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());

        List<String> roles = userRoles.stream()
                .map(role -> role.getRole().getName())
                .toList();

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        String refreshToken = tokenService.generateToken();

        String refreshTokenHash = tokenHashService.hash(refreshToken);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshSession refreshSession = new RefreshSession();

        refreshSession.setCreatedAt(now);
        refreshSession.setTokenHash(refreshTokenHash);
        refreshSession.setUser(user);
        refreshSession.setExpiresAt(now.plusDays(30));
        refreshSession.setLastUsedAt(now);

        refreshSessionRepository.save(refreshSession);

        user.setLastLoginAt(now);
        user.setUpdatedAt(now);

        return new  AuthResponse(
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();

        String refreshTokenHash = tokenHashService.hash(refreshToken);

        RefreshSession oldSession = refreshSessionRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if(oldSession.getExpiresAt().isBefore(now)){
            throw new InvalidRefreshTokenException();
        }
        if(oldSession.getRevokedAt() != null){
            throw new InvalidRefreshTokenException();
        }

        User user = oldSession.getUser();

        if(user.getStatus() != UserStatus.ACTIVE){
            throw new AccountDisabledException();
        }

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<String> roles = userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        String newAccessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        String newRefreshToken = tokenService.generateToken();
        String newRefreshTokenHash = tokenHashService.hash(newRefreshToken);

        oldSession.setRevokedAt(now);
        oldSession.setRevokedReason("ROTATED");
        oldSession.setLastUsedAt(now);

        RefreshSession refreshSession = new RefreshSession();

        refreshSession.setTokenHash(newRefreshTokenHash);
        refreshSession.setExpiresAt(now.plusDays(30));
        refreshSession.setCreatedAt(now);
        refreshSession.setUser(user);
        refreshSession.setLastUsedAt(now);

        refreshSessionRepository.save(refreshSession);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer"
        );
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request){

        String refreshToken = request.getRefreshToken();
        String refreshTokenHash = tokenHashService.hash(refreshToken);

        RefreshSession refreshSession =
                refreshSessionRepository.findByTokenHash(refreshTokenHash)
                        .orElseThrow(InvalidRefreshTokenException::new);

        if(refreshSession.getRevokedAt() != null){
            throw new InvalidRefreshTokenException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        refreshSession.setExpiresAt(now);
        refreshSession.setRevokedAt(now);
        refreshSession.setRevokedReason("LOGOUT");

        refreshSessionRepository.save(refreshSession);
    }

    @Transactional
    @Override
    public MessageResponse forgotPassword(String email){
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userRepository.findByEmail(email).ifPresent(
                user -> {
                    String rawToken = tokenService.generateToken();
                    String rawTokenHash = tokenHashService.hash(rawToken);

                    PasswordResetToken passwordResetToken = new PasswordResetToken();

                    passwordResetToken.setTokenHash(rawTokenHash);
                    passwordResetToken.setCreatedAt(now);
                    passwordResetToken.setExpiresAt(now.plusMinutes(5));
                    passwordResetToken.setUser(user);

                    passwordResetTokenRepository.save(passwordResetToken);

                    log.debug("Forgot password requested for email: {}", email);
                    log.debug("UserId: {}", user.getId());
                    log.debug("passwordResetToken: {}", rawToken);
                }
        );

        return new MessageResponse("If email and user account exist, a verification email has been sent.");
    }

    @Transactional
    @Override
    public MessageResponse resetPassword(ResetPasswordRequest resetPasswordRequest){

        String token = resetPasswordRequest.getToken();
        String tokenHash = tokenHashService.hash(token);


        PasswordResetToken passwordResetToken
                                        = passwordResetTokenRepository.findByTokenHash(tokenHash)
                                                                            .orElseThrow(PasswordResetTokenNotFoundException::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if(passwordResetToken.getExpiresAt().isBefore(now)){
            throw new InvalidPasswordResetTokenException();
        }
        if(passwordResetToken.getUsedAt() != null){
            throw new InvalidPasswordResetTokenException();
        }

        User user = passwordResetToken.getUser();

        String newPasswordHash = passwordEncoder.encode(resetPasswordRequest.getNewPassword());

        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(now);

        passwordResetToken.setUsedAt(now);

        List<RefreshSession> refreshSessions = refreshSessionRepository
                                                        .findByUserId(user.getId());

        for(RefreshSession refreshSession : refreshSessions){
            refreshSession.setLastUsedAt(now);
            refreshSession.setRevokedAt(now);
            refreshSession.setRevokedReason("password reset");
        }

        return new MessageResponse("Password Reset Successful.");
    }
}