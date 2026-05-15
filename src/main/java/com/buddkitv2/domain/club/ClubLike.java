package com.buddkitv2.domain.club;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CLUB_LIKE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    public static ClubLike create(User user, Club club) {
        ClubLike cl = new ClubLike();
        cl.user = user;
        cl.club = club;
        return cl;
    }
}
