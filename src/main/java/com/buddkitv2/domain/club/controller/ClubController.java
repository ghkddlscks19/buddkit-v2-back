package com.buddkitv2.domain.club.controller;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ApiResponse<ClubDetailResponse> createClub(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ClubCreateRequest request
    ) {
        return ApiResponse.ok(clubService.createClub(userId, request));
    }

    @PatchMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> updateClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody @Valid ClubUpdateRequest request
    ) {
        return ApiResponse.ok(clubService.updateClub(userId, clubId, request));
    }

    @GetMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> getClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(clubService.getClub(userId, clubId));
    }

    @PostMapping("/{clubId}/members")
    public ResponseEntity<Void> joinClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.joinClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clubId}/members/me")
    public ResponseEntity<Void> leaveClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.leaveClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clubId}/like")
    public ResponseEntity<Void> likeClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.likeClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clubId}/like")
    public ResponseEntity<Void> unlikeClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.unlikeClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }
}
