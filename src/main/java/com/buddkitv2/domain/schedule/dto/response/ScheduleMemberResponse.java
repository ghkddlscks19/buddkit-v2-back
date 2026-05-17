package com.buddkitv2.domain.schedule.dto.response;

import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ScheduleMemberResponse {
    private Long userScheduleId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private UserScheduleRole role;
}
