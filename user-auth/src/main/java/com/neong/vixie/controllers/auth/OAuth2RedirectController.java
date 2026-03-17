package com.neong.vixie.controllers.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/oauth2")
public class OAuth2RedirectController {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @GetMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE)
    public String success(
            HttpServletRequest request,
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "token_type", required = false) String tokenType,
            @RequestParam(value = "expires_in", required = false) String expiresIn,
            @RequestParam(value = "error", required = false) String error
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        if (accessToken != null) params.put("access_token", accessToken);
        if (refreshToken != null) params.put("refresh_token", refreshToken);
        if (tokenType != null) params.put("token_type", tokenType);
        if (expiresIn != null) params.put("expires_in", expiresIn);
        if (error != null) params.put("error", error);

        UriComponentsBuilder deepLinkBuilder = UriComponentsBuilder.fromUriString(redirectUri);
        for (Map.Entry<String, String> e : params.entrySet()) {
            deepLinkBuilder.queryParam(e.getKey(), e.getValue());
        }
        String deepLink = deepLinkBuilder.build().toUriString();
        String safeDeepLink = HtmlUtils.htmlEscape(deepLink);

        String title = "Đăng nhập thành công, Hãy truy cập đến ứng dụng";
        String body;
        if (error != null) {
            title = "Đăng nhập thất bại";
            body = "<p>" + HtmlUtils.htmlEscape(error) + "</p>";
        } else {
            body = "<p>" + HtmlUtils.htmlEscape(title) + "</p>";
        }

        return "<!doctype html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<title>OAuth2</title>" +
                "</head>" +
                "<body style=\"font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial; padding: 32px;\">" +
                "<div style=\"max-width: 720px; margin: 0 auto;\">" +
                "<h2 style=\"margin: 0 0 12px 0;\">" + HtmlUtils.htmlEscape(title) + "</h2>" +
                body +
                "<a href=\"" + safeDeepLink + "\" style=\"display:inline-block; margin-top: 16px; padding: 12px 16px; background:#111827; color:white; text-decoration:none; border-radius: 10px;\">Mở ứng dụng</a>" +
                "<p style=\"margin-top: 16px; color: #6b7280; word-break: break-all;\">" + safeDeepLink + "</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
