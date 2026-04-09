package com.neong.vixie.services.auth;

import com.neong.vixie.models.dto.LoginRequest;
import com.neong.vixie.models.dto.MobileOAuthLoginRequest;
import com.neong.vixie.models.dto.RefreshTokenRequest;
import com.neong.vixie.models.dto.RegisterRequest;
import com.neong.vixie.models.dto.ResetPasswordRequest;
import com.neong.vixie.models.dto.TokenResponse;
import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.db.User;
import com.neong.vixie.services.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final OtpService otpService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.otpService = otpService;
    }

    public TokenResponse register(RegisterRequest request) {
        if (!otpService.isEmailRecentlyVerified(request.email())) {
            throw new IllegalArgumentException("Email has not been verified with OTP.");
        }

        User user = userService.registerLocalUser(
                request.email(),
                request.password(),
                request.username(),
                request.firstName(),
                request.lastName(),
                request.countryOfOrigin()
        );
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new BadCredentialsException("Invalid user principal");
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String email = jwtService.extractUsername(refreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(newAccessToken, newRefreshToken, "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }

    public TokenResponse oauthLogin(MobileOAuthLoginRequest request) {
        String providerRaw = request.provider();
        AuthProvider provider;
        if ("google".equalsIgnoreCase(providerRaw)) {
            provider = AuthProvider.GOOGLE;
        } else if ("facebook".equalsIgnoreCase(providerRaw)) {
            provider = AuthProvider.FACEBOOK;
        } else {
            throw new IllegalArgumentException("Unsupported provider: " + providerRaw);
        }

        String username = request.username();
        if (username == null || username.isBlank()) {
            username = request.email();
        }

        try {
            JwtDecoder jwtDecoder;
            if (provider == AuthProvider.GOOGLE) {
                jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
            } else {
                throw new UnsupportedOperationException("Incomplete implementation: Facebook JWKS currently unsupported");
            }
            jwtDecoder.decode(request.idToken());
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid token signature");
        }

        User user = userService.upsertOAuthUser(request.email(), username, provider);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }

    public void sendRegistrationOtp(String email) {
        otpService.sendRegistrationOtp(email);
    }

    public void sendForgotPasswordOtp(String email) {
        userService.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User with email " + email + " does not exist."));
        otpService.sendForgotPasswordOtp(email);
    }

    public void verifyOtp(String email, String code) {
        otpService.verifyOtp(email, code);
    }

    public TokenResponse resetPassword(ResetPasswordRequest request) {
        // 1. Verify the OTP first
        otpService.verifyOtp(request.email(), request.code());

        // 2. Change the password in DB
        userService.changePassword(request.email(), request.newPassword());

        // 3. Return a new token so the user is logged in immediately
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found after password reset"));
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
    }
}
