package com.buddkitv2.domain.search.controller;

import com.buddkitv2.domain.search.dto.request.ClubViewRequest;
import com.buddkitv2.domain.search.dto.response.ClubSearchResponse;
import com.buddkitv2.domain.search.service.SearchService;
import com.buddkitv2.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search/clubs")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/recommend")
    public ApiResponse<List<ClubSearchResponse>> recommend(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(searchService.recommend(userId, page, size));
    }

    @GetMapping
    public ApiResponse<List<ClubSearchResponse>> searchClubs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String interestCategory,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(searchService.searchClubs(keyword, interestCategory, city, district, page, size));
    }

    @GetMapping("/co-members")
    public ApiResponse<List<ClubSearchResponse>> coMemberRecommend(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(searchService.coMemberRecommend(userId, page, size));
    }

    @PostMapping("/{clubId}/view")
    public ResponseEntity<Void> recordView(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody ClubViewRequest request
    ) {
        searchService.emitViewEvent(userId, clubId, request.getDwellSeconds());
        return ResponseEntity.noContent().build();
    }
}
