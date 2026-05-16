package com.buddkitv2.infra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-chars!!",
                900_000L,
                604_800_000L
        );
    }

    @Test
    void AccessToken_생성_및_검증() {
        String at = provider.generateAccessToken(1L);

        assertThat(provider.validateToken(at)).isTrue();
        assertThat(provider.isAccessToken(at)).isTrue();
        assertThat(provider.getUserId(at)).isEqualTo(1L);
    }

    @Test
    void RefreshToken_생성_및_검증() {
        String rt = provider.generateRefreshToken(1L);

        assertThat(provider.validateToken(rt)).isTrue();
        assertThat(provider.isAccessToken(rt)).isFalse();
        assertThat(provider.getUserId(rt)).isEqualTo(1L);
    }

    @Test
    void AT를_RT로_사용할_수_없다() {
        String at = provider.generateAccessToken(1L);
        assertThat(provider.isAccessToken(at)).isTrue();

        String rt = provider.generateRefreshToken(1L);
        assertThat(provider.isAccessToken(rt)).isFalse();
    }

    @Test
    void 잘못된_토큰은_검증_실패() {
        assertThat(provider.validateToken("invalid.token.value")).isFalse();
    }
}
