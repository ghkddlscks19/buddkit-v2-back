package com.buddkitv2.domain.feed.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FeedCommentResponse {

    private Long commentId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private LocalDateTime createdAt;
}
