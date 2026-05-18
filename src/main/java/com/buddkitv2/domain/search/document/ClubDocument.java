package com.buddkitv2.domain.search.document;

import com.buddkitv2.domain.search.event.ClubEventPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "club")
@Setting(settingPath = "elasticsearch/club-settings.json")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClubDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long clubId;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String name;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String district;

    @Field(type = FieldType.Keyword)
    private String interestCategory;

    @Field(type = FieldType.Keyword)
    private String interestName;

    @Field(type = FieldType.Integer)
    private Integer memberCount;

    @Field(type = FieldType.Integer)
    private Integer userLimit;

    @Field(type = FieldType.Keyword, index = false)
    private String clubImage;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime deletedAt;

    public static ClubDocument from(ClubEventPayload payload) {
        return new ClubDocument(
                String.valueOf(payload.getClubId()),
                payload.getClubId(),
                payload.getName(),
                payload.getDescription(),
                payload.getCity(),
                payload.getDistrict(),
                payload.getInterestCategory(),
                payload.getInterestName(),
                payload.getMemberCount(),
                payload.getUserLimit(),
                payload.getClubImage(),
                payload.getDeletedAt()
        );
    }
}
