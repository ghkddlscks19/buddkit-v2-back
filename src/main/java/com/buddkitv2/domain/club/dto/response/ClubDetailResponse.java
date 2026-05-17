package com.buddkitv2.domain.club.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubDetailResponse {

    private Long clubId;
    private String name;
    private String description;
    private String clubImage;
    private Integer userLimit;
    private Integer memberCount;
    private String city;
    private String district;
    private InterestCategory interestCategory;
    private String interestName;
    private boolean isLiked;
    private boolean isMember;
    private String myRole;
}
