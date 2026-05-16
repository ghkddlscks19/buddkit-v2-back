package com.buddkitv2.api.user;

import com.buddkitv2.api.common.ApiResponse;
import com.buddkitv2.application.user.UserService;
import com.buddkitv2.infra.security.JwtTokenProvider;
import com.buddkitv2.infra.security.RefreshTokenService;
import com.buddkitv2.infra.security.TempTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TempTokenService tempTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RegisterResponse> register(
            @RequestHeader("X-Temp-Token") String tempToken,
            @RequestPart("data") @Valid RegisterRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        Long kakaoId = tempTokenService.getKakaoId(tempToken);

        UserService.RegisterResult result = userService.register(kakaoId, request, profileImage);
        tempTokenService.delete(tempToken);

        String accessToken = jwtTokenProvider.generateAccessToken(result.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(result.getUserId());
        refreshTokenService.save(result.getUserId(), refreshToken);

        return ApiResponse.ok(new RegisterResponse(accessToken, refreshToken, result.getPoint()));
    }
}
