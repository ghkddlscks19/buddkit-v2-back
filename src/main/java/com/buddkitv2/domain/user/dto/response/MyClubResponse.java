package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class MyClubResponse {
    private Long clubId;
    private String name;
    private String clubImage;
    private InterestCategory interestCategory;
    private String interestName;
    private String city;
    private String district;
    private Integer memberCount;
    private String role;
}
