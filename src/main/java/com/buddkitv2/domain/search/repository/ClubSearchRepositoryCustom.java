package com.buddkitv2.domain.search.repository;

import com.buddkitv2.domain.search.document.ClubDocument;

import java.util.List;

public interface ClubSearchRepositoryCustom {
    List<ClubDocument> search(String keyword, String interestCategory, String city, String district, int page, int size);
    List<ClubDocument> recommend(List<String> interestCategories, String city, String district, List<Long> excludeClubIds, int page, int size);
}
