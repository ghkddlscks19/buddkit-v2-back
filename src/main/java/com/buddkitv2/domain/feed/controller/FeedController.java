package com.buddkitv2.domain.feed.controller;

import com.buddkitv2.domain.feed.dto.request.FeedCommentRequest;
import com.buddkitv2.domain.feed.dto.request.FeedCreateRequest;
import com.buddkitv2.domain.feed.dto.request.FeedUpdateRequest;
import com.buddkitv2.domain.feed.dto.response.FeedCommentResponse;
import com.buddkitv2.domain.feed.dto.response.FeedResponse;
import com.buddkitv2.domain.feed.entity.FeedSort;
import com.buddkitv2.domain.feed.service.FeedService;
import com.buddkitv2.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs/{clubId}/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @PostMapping
    public ApiResponse<FeedResponse> createFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody @Valid FeedCreateRequest request
    ) {
        return ApiResponse.ok(feedService.createFeed(userId, clubId, request));
    }

    @PatchMapping("/{feedId}")
    public ApiResponse<FeedResponse> updateFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId,
            @RequestBody @Valid FeedUpdateRequest request
    ) {
        return ApiResponse.ok(feedService.updateFeed(userId, clubId, feedId, request));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId
    ) {
        feedService.deleteFeed(userId, clubId, feedId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ApiResponse<List<FeedResponse>> getFeeds(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "LATEST") FeedSort sort,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(feedService.getFeeds(userId, clubId, sort, lastId, page, size));
    }

    @GetMapping("/{feedId}")
    public ApiResponse<FeedResponse> getFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId
    ) {
        return ApiResponse.ok(feedService.getFeed(userId, clubId, feedId));
    }

    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> likeFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId
    ) {
        feedService.likeFeed(userId, clubId, feedId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId
    ) {
        feedService.unlikeFeed(userId, clubId, feedId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{feedId}/comments")
    public ApiResponse<FeedCommentResponse> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId,
            @RequestBody @Valid FeedCommentRequest request
    ) {
        return ApiResponse.ok(feedService.createComment(userId, clubId, feedId, request));
    }

    @PatchMapping("/{feedId}/comments/{commentId}")
    public ApiResponse<FeedCommentResponse> updateComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @RequestBody @Valid FeedCommentRequest request
    ) {
        return ApiResponse.ok(feedService.updateComment(userId, clubId, feedId, commentId, request));
    }

    @DeleteMapping("/{feedId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        feedService.deleteComment(userId, clubId, feedId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{feedId}/comments")
    public ApiResponse<List<FeedCommentResponse>> getComments(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long feedId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(feedService.getComments(userId, clubId, feedId, lastId, size));
    }
}
