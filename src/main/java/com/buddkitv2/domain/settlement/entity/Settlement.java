package com.buddkitv2.domain.settlement.entity;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"SETTLEMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    private Long sum;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private LocalDateTime completedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Settlement create(Schedule schedule, Long sum, User user) {
        Settlement s = new Settlement();
        s.schedule = schedule;
        s.sum = sum;
        s.user = user;
        s.status = SettlementStatus.REQUESTED;
        return s;
    }
}
