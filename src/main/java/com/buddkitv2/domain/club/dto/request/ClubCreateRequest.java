package com.buddkitv2.domain.club.dto.request;

import com.buddkitv2.domain.user.entity.InterestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClubCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer userLimit;

    @NotBlank
    private String description;

    private String clubImage;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotNull
    private InterestCategory interestCategory;
}
