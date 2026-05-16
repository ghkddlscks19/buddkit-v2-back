package com.buddkitv2.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"INTEREST\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private InterestCategory category;

    @Column(length = 20)
    private String name;

    public static Interest of(InterestCategory category, String name) {
        Interest i = new Interest();
        i.category = category;
        i.name = name;
        return i;
    }
}
