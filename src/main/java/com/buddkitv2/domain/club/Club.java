package com.buddkitv2.domain.club;

import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.Interest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CLUB\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long id;

    @Column(length = 20)
    private String name;

    private Integer userLimit;

    @Column(length = 50)
    private String description;

    private String clubImage;

    private Integer memberCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    public static Club create(String name, Integer userLimit, String description,
                               String clubImage, Address address, Interest interest) {
        Club club = new Club();
        club.name = name;
        club.userLimit = userLimit;
        club.description = description;
        club.clubImage = clubImage;
        club.address = address;
        club.memberCount = 1;
        club.interest = interest;
        return club;
    }
}
