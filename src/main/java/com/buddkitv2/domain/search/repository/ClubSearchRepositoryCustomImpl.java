package com.buddkitv2.domain.search.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.buddkitv2.domain.search.document.ClubDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClubSearchRepositoryCustomImpl implements ClubSearchRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<ClubDocument> search(String keyword, String interestCategory,
                                     String city, String district, int page, int size) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .mustNot(mn -> mn.exists(e -> e.field("deletedAt")));

        if (keyword != null && !keyword.isBlank()) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name^3", "description^1")));
        }
        if (interestCategory != null) {
            bool.filter(f -> f.term(t -> t.field("interestCategory").value(interestCategory)));
        }
        if (city != null) {
            bool.filter(f -> f.term(t -> t.field("city").value(city)));
        }
        if (district != null) {
            bool.filter(f -> f.term(t -> t.field("district").value(district)));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withSort(s -> s.field(f -> f.field("memberCount").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(page, size))
                .build();

        return elasticsearchOperations.search(query, ClubDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClubDocument> recommend(List<String> interestCategories, String city,
                                        String district, List<Long> excludeClubIds, int page, int size) {
        BoolQuery.Builder baseFilter = new BoolQuery.Builder()
                .mustNot(mn -> mn.exists(e -> e.field("deletedAt")));

        if (!excludeClubIds.isEmpty()) {
            List<FieldValue> ids = excludeClubIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            baseFilter.mustNot(mn -> mn.terms(t -> t.field("clubId")
                    .terms(tv -> tv.value(ids))));
        }

        List<FunctionScore> functions = new ArrayList<>();
        for (String category : interestCategories) {
            functions.add(FunctionScore.of(fs -> fs
                    .filter(f -> f.term(t -> t.field("interestCategory").value(category)))
                    .weight(3.0)));
        }
        if (city != null) {
            String cityVal = city;
            functions.add(FunctionScore.of(fs -> fs
                    .filter(f -> f.term(t -> t.field("city").value(cityVal)))
                    .weight(2.0)));
        }
        if (district != null) {
            String districtVal = district;
            functions.add(FunctionScore.of(fs -> fs
                    .filter(f -> f.term(t -> t.field("district").value(districtVal)))
                    .weight(1.0)));
        }

        Query functionScoreQuery = Query.of(q -> q.functionScore(fs -> fs
                .query(bq -> bq.bool(baseFilter.build()))
                .functions(functions)
                .boostMode(FunctionBoostMode.Sum)
                .scoreMode(FunctionScoreMode.Sum)));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(functionScoreQuery)
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("memberCount").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(page, size))
                .build();

        return elasticsearchOperations.search(nativeQuery, ClubDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}
