package com.buddkitv2.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = ((Number) oAuth2User.getAttribute("id")).longValue();

        // TODO: UserService.findOrCreate(kakaoId) — USER 도메인 구현 후 교체
        Long userId = kakaoId;

        String accessToken = jwtTokenProvider.generateAccessToken(userId);

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"token\":\"" + accessToken + "\"}");
    }
}
