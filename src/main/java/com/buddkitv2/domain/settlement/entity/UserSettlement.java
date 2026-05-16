package com.buddkitv2.domain.settlement.entity;

import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"USER_SETTLEMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_settlement_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserSettlementStatus status;

    @Enumerated(EnumType.STRING)
    private UserSettlementType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    private LocalDateTime completedTime;

    public static UserSettlement create(User user, Settlement settlement) {
        UserSettlement us = new UserSettlement();
        us.user = user;
        us.settlement = settlement;
        us.type = UserSettlementType.POINT;
        us.status = UserSettlementStatus.REQUESTED;
        return us;
    }
}
