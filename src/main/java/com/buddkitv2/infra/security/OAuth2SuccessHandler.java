package com.buddkitv2.infra.security;

import com.buddkitv2.application.user.UserService;
import com.buddkitv2.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TempTokenService tempTokenService;
    private final UserService userService;

    @Value("${app.oauth2.redirect.login-success}")
    private String loginSuccessUrl;

    @Value("${app.oauth2.redirect.register}")
    private String registerUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = ((Number) oAuth2User.getAttribute("id")).longValue();

        Optional<User> existing = userService.findByKakaoId(kakaoId);

        if (existing.isPresent()) {
            User user = existing.get();
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
            refreshTokenService.save(user.getId(), refreshToken);

            String redirectUrl = loginSuccessUrl + "?at=" + accessToken + "&rt=" + refreshToken;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            String tempToken = tempTokenService.issue(kakaoId);
            getRedirectStrategy().sendRedirect(request, response, registerUrl + "?tempToken=" + tempToken);
        }
    }
}
