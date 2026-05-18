package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.Club;

public interface ClubOverlapProjection {
    Club getClub();
    Long getOverlap();
}
