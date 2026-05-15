package com.buddkitv2.domain.feed;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED_IMAGE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_image_id")
    private Long id;

    // 첫 번째 이미지를 썸네일로 사용 (압축)
    @Column(name = "feed_image", length = 255)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    public static FeedImage create(String imageUrl, Feed feed) {
        FeedImage fi = new FeedImage();
        fi.imageUrl = imageUrl;
        fi.feed = feed;
        return fi;
    }
}
