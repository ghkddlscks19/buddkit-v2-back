package com.buddkitv2.domain.search.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorEventPayload {
    private Long userId;
    private Long clubId;
    private String eventType;  // VIEW
    private Integer dwellSeconds;
    private LocalDateTime timestamp;
}
