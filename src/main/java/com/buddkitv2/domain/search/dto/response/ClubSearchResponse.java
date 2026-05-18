package com.buddkitv2.domain.search.dto.response;

import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.search.document.ClubDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubSearchResponse {
    private Long clubId;
    private String name;
    private String description;
    private String clubImage;
    private Integer memberCount;
    private Integer userLimit;
    private String city;
    private String district;
    private String interestCategory;
    private String interestName;

    public static ClubSearchResponse from(ClubDocument doc) {
        return new ClubSearchResponse(
                doc.getClubId(), doc.getName(), doc.getDescription(),
                doc.getClubImage(), doc.getMemberCount(), doc.getUserLimit(),
                doc.getCity(), doc.getDistrict(),
                doc.getInterestCategory(), doc.getInterestName()
        );
    }

    public static ClubSearchResponse fromClub(Club club) {
        return new ClubSearchResponse(
                club.getId(), club.getName(), club.getDescription(),
                club.getClubImage(), club.getMemberCount(), club.getUserLimit(),
                club.getAddress().getCity(), club.getAddress().getDistrict(),
                club.getInterest().getCategory().name(), club.getInterest().getName()
        );
    }
}
