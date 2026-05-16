package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyPageResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String city;
    private String district;
    private LocalDate birth;
    private List<InterestCategory> interestList;
    private Long balance;
}
