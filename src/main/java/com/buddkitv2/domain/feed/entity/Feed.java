package com.buddkitv2.domain.feed.entity;

import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long id;

    @Column(length = 255)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Feed create(String content, Club club, User user) {
        Feed feed = new Feed();
        feed.content = content;
        feed.club = club;
        feed.user = user;
        return feed;
    }
}
