package com.eventdriven.auth.security;

import com.eventdriven.auth.entity.*;
import com.eventdriven.auth.enums.OAuthProvider;
import com.eventdriven.auth.enums.UserStatus;
import com.eventdriven.auth.repository.*;
import com.eventdriven.auth.service.AuthCookiesService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final RefreshSessionRepository refreshSessionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthCookiesService authCookiesService;

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException
    {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerUserId = oAuth2User.getAttribute("sub");

        OAuthAccount oAuthAccount = oAuthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                providerUserId
        ).orElse(null);

        User user;

        if(oAuthAccount != null){
            user = oAuthAccount.getUser();
        }else{
            user = userRepository.findByEmail(email).orElse(null);

            if(user == null){
                user = new User();
                user.setEmail(email);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);
                user.setEmailVerifiedAt(now);

                userRepository.save(user);

                Role role = roleRepository.findByName("USER")
                        .orElseThrow(() -> new IllegalStateException("Role not found"));

                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRoleRepository.save(userRole);

            }

            oAuthAccount = new OAuthAccount();
            oAuthAccount.setUser(user);
            oAuthAccount.setProviderUserId(providerUserId);
            oAuthAccount.setCreatedAt(now);
            oAuthAccount.setProvider(OAuthProvider.GOOGLE);

            oAuthAccountRepository.save(oAuthAccount);
        }

        List<UserRole> userRole = userRoleRepository.findByUserId(user.getId());
        List<String> roles = userRole.stream()
                                    .map(role -> role.getRole().getName()).toList();

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                email,
                roles
        );

        String refreshToken = tokenService.generateToken();
        String refreshTokenHash = tokenHashService.hash(refreshToken);

        RefreshSession refreshSession = new RefreshSession();
        refreshSession.setUser(user);
        refreshSession.setTokenHash(refreshTokenHash);
        refreshSession.setCreatedAt(now);
        refreshSession.setExpiresAt(now.plusDays(30));
        refreshSession.setLastUsedAt(now);

        refreshSessionRepository.save(refreshSession);

        ResponseCookie accessTokenCookie = authCookiesService.createAccessTokenCookie(accessToken);
        ResponseCookie refreshTokenCookie = authCookiesService.createRefreshTokenCookie(refreshTokenHash);

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        getRedirectStrategy().sendRedirect(
                request,
                response,
                "http://localhost:3000/"
        );


    }

}
