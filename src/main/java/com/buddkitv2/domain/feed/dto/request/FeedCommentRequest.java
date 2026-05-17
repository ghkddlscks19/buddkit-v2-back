package com.buddkitv2.domain.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FeedCommentRequest {

    @NotBlank
    private String content;
}
