package com.buddkitv2.domain.search.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClubEventPayload {
    private String eventType;  // CREATED, UPDATED, DELETED
    private Long clubId;
    private String name;
    private String description;
    private String city;
    private String district;
    private String interestCategory;
    private String interestName;
    private Integer memberCount;
    private Integer userLimit;
    private String clubImage;
    private LocalDateTime deletedAt;
}
