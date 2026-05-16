package com.buddkitv2.domain.feed.entity;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED_COMMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_comment_id")
    private Long id;

    @Column(length = 255)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static FeedComment create(String content, Feed feed, User user) {
        FeedComment fc = new FeedComment();
        fc.content = content;
        fc.feed = feed;
        fc.user = user;
        return fc;
    }
}
