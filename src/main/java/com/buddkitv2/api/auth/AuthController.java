package com.buddkitv2.api.auth;

import com.buddkitv2.api.common.ApiResponse;
import com.buddkitv2.infra.security.JwtTokenProvider;
import com.buddkitv2.infra.security.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        String rt = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(rt) || jwtTokenProvider.isAccessToken(rt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("유효하지 않은 리프레시 토큰입니다."));
        }

        Long userId = jwtTokenProvider.getUserId(rt);

        if (!refreshTokenService.validate(userId, rt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("만료되었거나 이미 사용된 리프레시 토큰입니다."));
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(newAccessToken, rt)));
    }
}
