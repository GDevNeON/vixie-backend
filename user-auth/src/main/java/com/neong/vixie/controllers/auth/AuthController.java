package com.neong.vixie.controllers.auth;

import com.neong.vixie.services.auth.AuthService;
import com.neong.vixie.models.dto.LoginRequest;
import com.neong.vixie.models.dto.MobileOAuthLoginRequest;
import com.neong.vixie.models.dto.RefreshTokenRequest;
import com.neong.vixie.models.dto.RegisterRequest;
import com.neong.vixie.models.dto.ResetPasswordRequest;
import com.neong.vixie.models.dto.SendOtpRequest;
import com.neong.vixie.models.dto.TokenResponse;
import com.neong.vixie.models.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/oauth-login")
    public TokenResponse oauthLogin(@Valid @RequestBody MobileOAuthLoginRequest request) {
        return authService.oauthLogin(request);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<Void> oauthGoogle() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/google")
                .build();
    }

    @GetMapping("/oauth/facebook")
    public ResponseEntity<Void> oauthFacebook() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/facebook")
                .build();
    }

    @PostMapping("/register/send-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendRegistrationOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendRegistrationOtp(request.email());
    }

    @PostMapping("/forgot-password/send-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendForgotPasswordOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendForgotPasswordOtp(request.email());
    }

    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.email(), request.code());
    }

    @PostMapping("/reset-password")
    public TokenResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
