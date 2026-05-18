package com.buddkitv2.domain.search.repository;

import com.buddkitv2.domain.search.document.ClubDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ClubSearchRepository extends ElasticsearchRepository<ClubDocument, String>, ClubSearchRepositoryCustom {
}
