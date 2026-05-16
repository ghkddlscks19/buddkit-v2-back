package com.buddkitv2.domain.club.entity;

import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_CLUB\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_club_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserClubRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static UserClub create(Club club, User user, UserClubRole role) {
        UserClub uc = new UserClub();
        uc.club = club;
        uc.user = user;
        uc.role = role;
        return uc;
    }
}
