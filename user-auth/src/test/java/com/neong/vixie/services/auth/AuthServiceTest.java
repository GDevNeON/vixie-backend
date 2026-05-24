package com.neong.vixie.services.auth;

import com.neong.vixie.models.dto.MobileOAuthLoginRequest;
import com.neong.vixie.services.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserService userService;
    @Mock
    private OtpService otpService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, userService, otpService);
    }

    @Test
    void oauthLogin_WithInvalidGoogleToken_ShouldThrowBadCredentialsException() {
        MobileOAuthLoginRequest request = new MobileOAuthLoginRequest(
                "google",
                "invalid.token.signature",
                "test@example.com",
                "Test User"
        );

        assertThatThrownBy(() -> authService.oauthLogin(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid token signature");
    }
}
