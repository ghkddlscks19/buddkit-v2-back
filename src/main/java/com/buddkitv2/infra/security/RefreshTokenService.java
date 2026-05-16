package com.buddkitv2.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(userId),
                hash(refreshToken),
                REFRESH_TOKEN_TTL_DAYS,
                TimeUnit.DAYS
        );
    }

    public boolean validate(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(userId));
        return stored != null && stored.equals(hash(refreshToken));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return "RT:" + userId;
    }

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
