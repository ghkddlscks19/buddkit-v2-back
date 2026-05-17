package com.buddkitv2.domain.user.controller;

import com.buddkitv2.domain.user.dto.request.ChargeRequest;
import com.buddkitv2.domain.user.dto.request.ProfileUpdateRequest;
import com.buddkitv2.domain.user.dto.request.RegisterRequest;
import com.buddkitv2.domain.user.dto.response.ChargeResponse;
import com.buddkitv2.domain.user.dto.response.MyPageResponse;
import com.buddkitv2.domain.user.dto.response.RegisterResponse;
import com.buddkitv2.domain.user.dto.response.SettlementHistoryResponse;
import com.buddkitv2.domain.user.dto.response.TransactionResponse;

import java.util.List;
import com.buddkitv2.global.common.ApiResponse;
import com.buddkitv2.domain.user.service.UserService;
import com.buddkitv2.global.security.JwtTokenProvider;
import com.buddkitv2.global.security.RefreshTokenService;
import com.buddkitv2.global.security.TempTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/me/wallet/charge")
    public ApiResponse<ChargeResponse> chargeWallet(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ChargeRequest request
    ) {
        return ApiResponse.ok(userService.chargeWallet(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getMyPage(userId));
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MyPageResponse> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestPart("data") @Valid ProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return ApiResponse.ok(userService.updateProfile(userId, request, profileImage));
    }

    @GetMapping("/me/transactions")
    public ApiResponse<List<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(userService.getTransactions(userId, lastId, size));
    }

    @GetMapping("/me/settlements")
    public ApiResponse<List<SettlementHistoryResponse>> getSettlements(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(userService.getSettlements(userId, lastId, size));
    }

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
