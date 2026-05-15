package com.buddkitv2.domain.schedule;

import com.buddkitv2.domain.club.Club;
import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"SCHEDULE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    private LocalDateTime scheduleTime;

    @Column(length = 20)
    private String name;

    @Column(length = 255)
    private String location;

    private Long cost;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column(name = "\"limit\"")
    private Integer limit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    public static Schedule create(String name, LocalDateTime scheduleTime, String location,
                                   Long cost, Integer limit, Club club) {
        Schedule s = new Schedule();
        s.name = name;
        s.scheduleTime = scheduleTime;
        s.location = location;
        s.cost = cost;
        s.limit = limit;
        s.club = club;
        s.status = ScheduleStatus.RECRUITING;
        return s;
    }
}
