package com.neong.vixie.services.auth;

import com.neong.vixie.models.db.User;
import com.neong.vixie.services.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import com.neong.vixie.models.constant.AuthProvider;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        Object emailAttr = oAuth2User.getAttributes().get("email");
        if (emailAttr == null) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        String email = emailAttr.toString();
        
        Object nameAttr = oAuth2User.getAttributes().get("name");
        String name = nameAttr != null ? nameAttr.toString() : email;

        AuthProvider authProvider = AuthProvider.LOCAL;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            if ("google".equalsIgnoreCase(registrationId)) {
                authProvider = AuthProvider.GOOGLE;
            } else if ("facebook".equalsIgnoreCase(registrationId)) {
                authProvider = AuthProvider.FACEBOOK;
            }
        }

        User user = userService.upsertOAuthUser(email, name, authProvider);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .queryParam("token_type", "Bearer")
                .queryParam("expires_in", jwtService.getAccessTokenExpirationSeconds())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
