package com.buddkitv2.domain.schedule.dto.response;

import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class ScheduleResponse {
    private Long scheduleId;
    private String name;
    private LocalDateTime scheduleTime;
    private String location;
    private Long cost;
    private ScheduleStatus status;
    private Integer limit;
    private long participantCount;
    private boolean isJoined;
}
