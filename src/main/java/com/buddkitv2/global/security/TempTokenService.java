package com.buddkitv2.global.security;

import com.buddkitv2.global.exception.TempTokenExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TempTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final long TTL_MINUTES = 5;
    private static final String PREFIX = "TEMP:";

    public String issue(Long kakaoId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, String.valueOf(kakaoId), TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public Long getKakaoId(String token) {
        String value = redisTemplate.opsForValue().get(PREFIX + token);
        if (value == null) {
            throw new TempTokenExpiredException();
        }
        return Long.parseLong(value);
    }

    public void delete(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
