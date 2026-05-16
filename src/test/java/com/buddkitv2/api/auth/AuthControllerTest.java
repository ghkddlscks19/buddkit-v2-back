package com.buddkitv2.api.auth;

import com.buddkitv2.infra.security.JwtTokenProvider;
import com.buddkitv2.infra.security.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    void 유효한_RT로_새_AT를_발급받는다() throws Exception {
        String rt = "valid.refresh.token";
        String newAt = "new.access.token";
        Long userId = 1L;

        when(jwtTokenProvider.validateToken(rt)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(rt)).thenReturn(false);
        when(jwtTokenProvider.getUserId(rt)).thenReturn(userId);
        when(refreshTokenService.validate(userId, rt)).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn(newAt);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value(newAt));
    }

    @Test
    void 만료된_RT는_401을_반환한다() throws Exception {
        String rt = "expired.refresh.token";

        when(jwtTokenProvider.validateToken(rt)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rt + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void Redis에_없는_RT는_401을_반환한다() throws Exception {
        String rt = "unknown.refresh.token";
        Long userId = 1L;

        when(jwtTokenProvider.validateToken(rt)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(rt)).thenReturn(false);
        when(jwtTokenProvider.getUserId(rt)).thenReturn(userId);
        when(refreshTokenService.validate(userId, rt)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rt + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
