package com.neong.vixie.services.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String userAgent = request.getHeader("User-Agent");
        boolean isAndroid = userAgent != null && userAgent.toLowerCase().contains("android");

        String targetUrl;
        if (isAndroid) {
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", exception.getLocalizedMessage())
                    .build()
                    .toUriString();
        } else {
            targetUrl = UriComponentsBuilder.fromPath("/oauth2/success")
                    .queryParam("error", exception.getLocalizedMessage())
                    .build()
                    .toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
