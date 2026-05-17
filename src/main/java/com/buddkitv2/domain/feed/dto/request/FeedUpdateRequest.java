package com.buddkitv2.domain.feed.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FeedUpdateRequest {

    private String content;

    @NotEmpty
    @Size(min = 1, max = 5)
    private List<String> imageUrls;
}
