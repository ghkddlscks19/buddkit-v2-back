package com.buddkitv2.application.user;

import com.buddkitv2.domain.user.User;
import com.buddkitv2.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final Long kakaoId = 12345L;
    private final String nickname = "테스트유저";
    private final String profileImageUrl = "https://example.com/img.jpg";

    @Test
    void 기존_회원이면_DB_조회_결과를_반환한다() {
        User existing = User.create(kakaoId, nickname, profileImageUrl);
        when(userRepository.findByKakaoId(kakaoId)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(kakaoId, nickname, profileImageUrl);

        assertThat(result.getKakaoId()).isEqualTo(kakaoId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 신규_회원이면_저장_후_반환한다() {
        User saved = User.create(kakaoId, nickname, profileImageUrl);
        when(userRepository.findByKakaoId(kakaoId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.findOrCreate(kakaoId, nickname, profileImageUrl);

        assertThat(result.getKakaoId()).isEqualTo(kakaoId);
        verify(userRepository).save(any(User.class));
    }
}
