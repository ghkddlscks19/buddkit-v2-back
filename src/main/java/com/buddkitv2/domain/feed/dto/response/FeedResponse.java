package com.buddkitv2.domain.feed.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class FeedResponse {

    private Long feedId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private List<String> imageUrls;
    private Long likeCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
}
